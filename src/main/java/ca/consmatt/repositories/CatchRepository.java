package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.Catch;
import ca.consmatt.beans.FishingType;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.beans.WaterType;
import ca.consmatt.dto.FeedPostResponse;

/**
 * Persistence for {@link Catch} entities.
 */
public interface CatchRepository extends JpaRepository<Catch, Long> {

	List<Catch> findByLocation_IdOrderByIdAsc(Long locationId);

	Optional<Catch> findByIdAndLocation_Id(Long id, Long locationId);

	@Query("""
			select new ca.consmatt.dto.FeedPostResponse(
				l.id,
				l.locationName,
				l.latitude,
				l.longitude,
				l.timeStamp,
				a.id,
				a.username,
				c.id,
				c.species,
				c.quantity,
				c.lengthCm,
				c.weightKg,
				c.notes,
				c.description,
				c.imageUrl,
				c.imageUrlsRaw,
				c.fishDetailsJson,
				c.fishingType,
				l.visibility
			)
			from Catch c
			join c.location l
			join l.account a
			where (l.visibility is null or l.visibility = :publicVis)
				or a.id = :viewerId
				or (l.visibility = :friendsVis and (
					exists (select 1 from Friendship f where f.accountA.id = a.id and f.accountB.id = :viewerId)
					or exists (select 1 from Friendship f2 where f2.accountA.id = :viewerId and f2.accountB.id = a.id)
				))
			order by l.timeStamp desc, c.id desc
			""")
	List<FeedPostResponse> findFeedPostsForViewer(
			@Param("viewerId") Long viewerId,
			@Param("publicVis") PostVisibility publicVis,
			@Param("friendsVis") PostVisibility friendsVis,
			Pageable pageable);

	/** Removes all catches for the given location (used before deleting the location). */
	void deleteByLocation_Id(Long locationId);

	long countByLocation_Account_Id(Long accountId);

	/** Number of catches still attached to a location (used to auto-prune empty locations). */
	long countByLocation_Id(Long locationId);

	/** Distinct (lower-cased, trimmed) {@code species} field across the user's catches; multi-fish posts store comma-joined species which the service splits client-side. */
	@Query("select c.species from Catch c join c.location l where l.account.id = :accountId")
	List<String> findSpeciesStringsByAccountId(@Param("accountId") Long accountId);

	/** Whether the account has at least one catch with a stored photo (single or multi-image). */
	@Query("""
			select case when count(c) > 0 then true else false end
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and ((c.imageUrl is not null and c.imageUrl <> '')
					or (c.imageUrlsRaw is not null and c.imageUrlsRaw <> ''))
			""")
	boolean existsByAccountIdWithPhoto(@Param("accountId") Long accountId);

	/**
	 * Whether the account has at least one trophy-sized catch.
	 * Single-fish posts use {@code lengthCm}/{@code weightKg}; multi-fish posts leave them null
	 * and store per-fish details in {@code fishDetailsJson}, which this query intentionally ignores
	 * (multi-fish trophies are an accepted edge-case miss).
	 */
	@Query("""
			select case when count(c) > 0 then true else false end
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and (c.lengthCm >= :minLength or c.weightKg >= :minWeight)
			""")
	boolean existsTrophyCatchByAccountId(@Param("accountId") Long accountId,
			@Param("minLength") double minLengthCm,
			@Param("minWeight") double minWeightKg);

	/** Number of catches per fishing technique for the account; missing techniques are absent from the list. */
	@Query("""
			select c.fishingType, count(c)
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and c.fishingType is not null
			group by c.fishingType
			""")
	List<Object[]> countCatchesByFishingType(@Param("accountId") Long accountId);

	/** Whether the account has at least one catch on a location of the given water type. */
	@Query("""
			select case when count(c) > 0 then true else false end
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and l.waterType = :waterType
			""")
	boolean existsCatchAtWaterType(@Param("accountId") Long accountId,
			@Param("waterType") WaterType waterType);

	/** Whether the account has at least one catch on a location whose water type is in the given set. */
	@Query("""
			select case when count(c) > 0 then true else false end
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and l.waterType in :waterTypes
			""")
	boolean existsCatchAtAnyWaterType(@Param("accountId") Long accountId,
			@Param("waterTypes") java.util.Collection<WaterType> waterTypes);

	/** Distinct water types the account has logged at least one catch under. */
	@Query("""
			select distinct l.waterType
			from Catch c
			join c.location l
			where l.account.id = :accountId
				and l.waterType is not null
			""")
	List<WaterType> findDistinctWaterTypesByAccountId(@Param("accountId") Long accountId);

	/** Convenience: pull the full list of fishing types the account has used (absent = unspecified). */
	default java.util.Set<FishingType> findDistinctFishingTypes(Long accountId) {
		java.util.Set<FishingType> set = new java.util.HashSet<>();
		for (Object[] row : countCatchesByFishingType(accountId)) {
			Object first = row != null && row.length > 0 ? row[0] : null;
			if (first instanceof FishingType ft) {
				set.add(ft);
			}
		}
		return set;
	}
}
