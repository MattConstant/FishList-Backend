package ca.consmatt.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountAchievement;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.AchievementCode;
import ca.consmatt.beans.FishingType;
import ca.consmatt.beans.Location;
import ca.consmatt.beans.RoleTier;
import ca.consmatt.beans.WaterType;
import ca.consmatt.dto.AchievementProgressResponse;
import ca.consmatt.dto.AchievementResponse;
import ca.consmatt.dto.UnlockedAchievementSummary;
import ca.consmatt.repositories.AccountAchievementRepository;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchCommentRepository;
import ca.consmatt.repositories.CatchLikeRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Single source of truth for achievement evaluation and progress reporting.
 *
 * Controllers call {@link #evaluateAll(Account)} after a write that could earn an achievement
 * (logging a catch, liking, commenting, adding a friend) and forward the returned list to the
 * client as an inline notification. {@link #getProgress(Account, AdminCheck)} powers the
 * achievements panels on profile/users pages.
 *
 * Adding a new achievement is a two-step process:
 *   1. Append a constant to {@link AchievementCode} (with metadata).
 *   2. Add a {@code case} to {@link #computeProgress(StateSnapshot, AchievementCode)} mapping it
 *      to the correct stat. No new event hooks are needed when the existing snapshot already
 *      covers the data point.
 *
 * <p>Idempotency: each unlock is guarded by an existence check plus a unique DB constraint, so
 * concurrent requests cannot create duplicate rows; a second concurrent unlock is silently dropped.
 *
 * <p>Performance: each event triggers a single small batch of count queries (the snapshot) and
 * a fixed-size loop over the catalog. Roles are derived from already-counted XP, so role lookups
 * never re-run the snapshot.
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

	private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

	// Trophy thresholds — rounded to clean imperial values (the UI reports both metric and
	// imperial in descriptions, but the user-facing units are inches/lbs).
	private static final double IN_TO_CM = 2.54;
	private static final double LBS_TO_KG = 0.45359237;
	private static final double TROPHY_MIN_LENGTH_CM = 27.0 * IN_TO_CM; // 68.58 cm ≈ 27 in
	private static final double TROPHY_MIN_WEIGHT_KG = 11.0 * LBS_TO_KG; // 4.989 kg ≈ 11 lbs

	/**
	 * Lat/lng tolerance used to determine whether two map pins represent the "same spot" for the
	 * trailblazer achievement. 3 decimals ≈ ~110 m at the equator, narrowing toward the poles.
	 */
	private static final int TRAILBLAZER_DECIMALS = 3;

	private final AccountAchievementRepository accountAchievementRepo;
	private final AccountRepository accountRepo;
	private final CatchRepository catchRepo;
	private final CatchLikeRepository catchLikeRepo;
	private final CatchCommentRepository catchCommentRepo;
	private final LocationRepository locationRepo;
	private final FriendshipRepository friendshipRepo;

	/**
	 * Evaluates every achievement for {@code account} and persists the ones whose criteria are now
	 * met but were not previously unlocked.
	 *
	 * @param account current authenticated account (may be {@code null}: returns empty)
	 * @return summaries of achievements that were newly unlocked by this call
	 */
	public List<UnlockedAchievementSummary> evaluateAll(Account account) {
		if (account == null || account.getId() == null) {
			return List.of();
		}
		Set<AchievementCode> already = currentlyUnlockedCodes(account.getId());
		StateSnapshot snapshot = snapshot(account.getId());
		List<UnlockedAchievementSummary> unlocked = new ArrayList<>();
		for (AchievementCode code : AchievementCode.values()) {
			if (already.contains(code)) {
				continue;
			}
			if (computeProgress(snapshot, code) >= code.target()) {
				if (tryPersist(account, code)) {
					unlocked.add(toSummary(code));
				}
			}
		}
		return unlocked;
	}

	/**
	 * Returns the user's full achievement state (locked + unlocked) plus role/XP summary.
	 */
	public AchievementProgressResponse getProgress(Account viewedAccount, AdminCheck adminCheck) {
		if (viewedAccount == null || viewedAccount.getId() == null) {
			throw new IllegalArgumentException("account is required");
		}
		StateSnapshot snapshot = snapshot(viewedAccount.getId());
		Map<AchievementCode, String> unlockedAt = currentlyUnlockedAt(viewedAccount.getId());
		int totalXp = 0;
		List<AchievementResponse> rows = new ArrayList<>(AchievementCode.values().length);
		int unlockedCount = 0;
		for (AchievementCode code : AchievementCode.values()) {
			boolean isUnlocked = unlockedAt.containsKey(code);
			int progress = Math.min(computeProgress(snapshot, code), code.target());
			if (isUnlocked) {
				totalXp += code.xp();
				unlockedCount++;
			}
			rows.add(new AchievementResponse(
					code.name(),
					code.titleKey(),
					code.descriptionKey(),
					code.xp(),
					code.rarity(),
					code.category(),
					code.target(),
					isUnlocked ? code.target() : progress,
					isUnlocked,
					unlockedAt.get(code)));
		}
		// Sort: unlocked first (most-recent first), then locked ordered by closest-to-completion.
		rows.sort(Comparator
				.<AchievementResponse, Integer>comparing(a -> a.unlocked() ? 0 : 1)
				.thenComparing(Comparator
						.<AchievementResponse, String>comparing(a -> a.unlocked() ? a.unlockedAt() : "",
								Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparingDouble(a -> a.unlocked()
								? 0.0
								: -((double) a.progress() / Math.max(1, a.target())))));

		RoleTier tier = RoleTier.forXp(totalXp);
		RoleTier next = RoleTier.nextTier(tier);
		boolean isAdmin = adminCheck != null && adminCheck.isAdmin(viewedAccount);
		return new AchievementProgressResponse(
				viewedAccount.getId(),
				viewedAccount.getUsername(),
				viewedAccount.getProfileImageKey(),
				totalXp,
				tier.name(),
				tier.labelKey(),
				next == null ? null : next.name(),
				next == null ? null : next.labelKey(),
				next == null ? null : next.minXp(),
				isAdmin,
				unlockedCount,
				AchievementCode.values().length,
				rows);
	}

	/**
	 * Re-evaluates achievements for every existing account. Used by the startup backfill so that
	 * users who already have catches/likes/comments before this feature shipped see their badges.
	 *
	 * @return number of unlock rows inserted
	 */
	public int backfillAll() {
		int inserted = 0;
		for (Account account : accountRepo.findAll()) {
			inserted += evaluateAll(account).size();
		}
		return inserted;
	}

	private boolean tryPersist(Account account, AchievementCode code) {
		try {
			accountAchievementRepo.save(new AccountAchievement(null, account, code, Instant.now().toString()));
			log.info("ACHIEVEMENT_UNLOCKED user={} code={} xp={}",
					account.getUsername(), code, code.xp());
			return true;
		} catch (DataIntegrityViolationException e) {
			// Concurrent request already inserted this code — treat as no-op.
			return false;
		}
	}

	private Set<AchievementCode> currentlyUnlockedCodes(Long accountId) {
		Set<AchievementCode> codes = new HashSet<>();
		for (AccountAchievement a : accountAchievementRepo.findByAccount_Id(accountId)) {
			codes.add(a.getCode());
		}
		return codes;
	}

	private Map<AchievementCode, String> currentlyUnlockedAt(Long accountId) {
		Map<AchievementCode, String> map = new HashMap<>();
		for (AccountAchievement a : accountAchievementRepo.findByAccount_Id(accountId)) {
			map.put(a.getCode(), a.getUnlockedAt());
		}
		return map;
	}

	private StateSnapshot snapshot(Long accountId) {
		long catches = catchRepo.countByLocation_Account_Id(accountId);
		long locations = locationRepo.countByAccount_Id(accountId);
		List<String> rawSpecies = catchRepo.findSpeciesStringsByAccountId(accountId);
		int distinctSpecies = computeDistinctSpecies(rawSpecies);
		boolean hasPhoto = catchRepo.existsByAccountIdWithPhoto(accountId);
		long likesGiven = catchLikeRepo.countByAccount_Id(accountId);
		long commentsGiven = catchCommentRepo.countByAccount_Id(accountId);
		long friendCount = friendshipRepo.countByAccountA_IdOrAccountB_Id(accountId, accountId);
		long likesReceived = catchLikeRepo.countLikesReceivedByAccountId(accountId);
		boolean hasTrophy = catchRepo.existsTrophyCatchByAccountId(accountId,
				TROPHY_MIN_LENGTH_CM, TROPHY_MIN_WEIGHT_KG);

		Map<FishingType, Long> catchesByTechnique = new HashMap<>();
		for (Object[] row : catchRepo.countCatchesByFishingType(accountId)) {
			if (row == null || row.length < 2) {
				continue;
			}
			if (row[0] instanceof FishingType ft && row[1] instanceof Number n) {
				catchesByTechnique.put(ft, n.longValue());
			}
		}
		int distinctTechniques = catchesByTechnique.size();

		boolean hasStockedLakeCatch = catchRepo.existsCatchAtWaterType(accountId, WaterType.STOCKED_LAKE);
		boolean hasRiverCatch = catchRepo.existsCatchAtAnyWaterType(accountId,
				List.of(WaterType.RIVER, WaterType.STREAM));
		boolean hasOceanCatch = catchRepo.existsCatchAtWaterType(accountId, WaterType.OCEAN);
		int distinctWaterTypes = catchRepo.findDistinctWaterTypesByAccountId(accountId).size();
		boolean isTrailblazer = computeIsTrailblazer(accountId);

		return new StateSnapshot(catches, distinctSpecies, locations, hasPhoto, likesGiven,
				commentsGiven, friendCount, likesReceived, hasTrophy, catchesByTechnique,
				distinctTechniques, hasStockedLakeCatch, hasRiverCatch, hasOceanCatch,
				distinctWaterTypes, isTrailblazer);
	}

	/**
	 * True when the user has at least one location whose rounded lat/lng is shared with
	 * <em>no other</em> account in the database. "Rounded" uses {@link #TRAILBLAZER_DECIMALS}
	 * decimals (~110 m), which is generous enough that two anglers picking the same lake from
	 * the map collapse to the same key.
	 */
	private boolean computeIsTrailblazer(Long accountId) {
		List<Location> mine = locationRepo.findByAccount_Id(accountId);
		if (mine.isEmpty()) {
			return false;
		}
		Set<String> othersKeys = new HashSet<>();
		for (Object[] row : locationRepo.findOtherAccountsLatLng(accountId)) {
			if (row == null || row.length < 2) {
				continue;
			}
			String key = roundedLatLngKey(row[0], row[1]);
			if (key != null) {
				othersKeys.add(key);
			}
		}
		for (Location loc : mine) {
			String key = roundedLatLngKey(loc.getLatitude(), loc.getLongitude());
			if (key != null && !othersKeys.contains(key)) {
				return true;
			}
		}
		return false;
	}

	private static String roundedLatLngKey(Object lat, Object lng) {
		Double a = parseCoord(lat);
		Double b = parseCoord(lng);
		if (a == null || b == null) {
			return null;
		}
		double scale = Math.pow(10, TRAILBLAZER_DECIMALS);
		long rLat = Math.round(a * scale);
		long rLng = Math.round(b * scale);
		return rLat + "," + rLng;
	}

	private static Double parseCoord(Object raw) {
		if (raw == null) {
			return null;
		}
		try {
			return Double.parseDouble(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int computeDistinctSpecies(List<String> rawSpeciesStrings) {
		Set<String> distinct = new HashSet<>();
		for (String raw : rawSpeciesStrings) {
			if (raw == null) {
				continue;
			}
			for (String part : raw.split(",")) {
				String norm = part.trim().toLowerCase(Locale.ROOT);
				if (!norm.isEmpty()) {
					distinct.add(norm);
				}
			}
		}
		return distinct.size();
	}

	private static int computeProgress(StateSnapshot s, AchievementCode code) {
		switch (code) {
			case FIRST_CATCH:
			case TEN_CATCHES:
			case FIFTY_CATCHES:
			case HUNDRED_CATCHES:
				return clampInt(s.catches);
			case FIVE_SPECIES:
			case TEN_SPECIES:
				return s.distinctSpecies;
			case THREE_LOCATIONS:
			case TEN_LOCATIONS:
				return clampInt(s.locations);
			case FIRST_PHOTO:
				return s.hasPhoto ? 1 : 0;
			case FIRST_LIKE_GIVEN:
				return s.likesGiven > 0 ? 1 : 0;
			case FIRST_COMMENT:
				return s.commentsGiven > 0 ? 1 : 0;
			case FIRST_FRIEND:
				return s.friendCount > 0 ? 1 : 0;
			case POPULAR_CATCH:
				return clampInt(s.likesReceived);
			case TROPHY_FISH:
				return s.hasTrophy ? 1 : 0;
			case FIRST_FLY:
			case FLY_FISHING_PRO:
				return clampInt(catchesOfTechnique(s, FishingType.FLY));
			case FIRST_TROLLING:
			case TROLLING_PRO:
				return clampInt(catchesOfTechnique(s, FishingType.TROLLING));
			case FIRST_ICE:
			case ICE_FISHING_PRO:
				return clampInt(catchesOfTechnique(s, FishingType.ICE));
			case VERSATILE_ANGLER:
			case TECHNIQUE_MASTER:
				return s.distinctTechniques;
			case STOCKED_LAKE_CATCH:
				return s.hasStockedLakeCatch ? 1 : 0;
			case RIVER_CATCH:
				return s.hasRiverCatch ? 1 : 0;
			case OCEAN_CATCH:
				return s.hasOceanCatch ? 1 : 0;
			case TRAILBLAZER:
				return s.isTrailblazer ? 1 : 0;
			case RANGE_ROAMER:
				return s.distinctWaterTypes;
			default:
				return 0;
		}
	}

	private static long catchesOfTechnique(StateSnapshot s, FishingType type) {
		Long n = s.catchesByTechnique.get(type);
		return n == null ? 0L : n;
	}

	private static int clampInt(long v) {
		if (v <= 0) {
			return 0;
		}
		if (v >= Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) v;
	}

	private static UnlockedAchievementSummary toSummary(AchievementCode code) {
		return new UnlockedAchievementSummary(
				code.name(),
				code.titleKey(),
				code.descriptionKey(),
				code.xp(),
				code.rarity());
	}

	/** Tiny callback so the service can ask the caller "is this account an admin?" without owning the rule. */
	@FunctionalInterface
	public interface AdminCheck {
		boolean isAdmin(Account account);

		static AdminCheck byRole() {
			return account -> account != null && account.getRole() == AccountRole.ADMIN;
		}
	}

	private record StateSnapshot(
			long catches,
			int distinctSpecies,
			long locations,
			boolean hasPhoto,
			long likesGiven,
			long commentsGiven,
			long friendCount,
			long likesReceived,
			boolean hasTrophy,
			Map<FishingType, Long> catchesByTechnique,
			int distinctTechniques,
			boolean hasStockedLakeCatch,
			boolean hasRiverCatch,
			boolean hasOceanCatch,
			int distinctWaterTypes,
			boolean isTrailblazer) {
	}
}
