package ca.consmatt.beans;

/**
 * Stable identifier + metadata for every achievement in the catalog.
 *
 * Adding a new achievement: append a new enum constant here and (if the trigger does not match
 * an existing branch in {@code AchievementService}) wire it into the corresponding event handler.
 * Frontend strings live under {@code locales/&#42;/achievements.ts} keyed by {@link #titleKey()}
 * and {@link #descriptionKey()}.
 *
 * The {@link #target()} value is the criterion threshold (e.g. 10 catches) and is used to
 * report progress on the frontend; for one-shot triggers it is 1.
 */
public enum AchievementCode {

	FIRST_CATCH(
			"achievements.firstCatch.title",
			"achievements.firstCatch.description",
			5,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			1),

	TEN_CATCHES(
			"achievements.tenCatches.title",
			"achievements.tenCatches.description",
			25,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			10),

	FIFTY_CATCHES(
			"achievements.fiftyCatches.title",
			"achievements.fiftyCatches.description",
			75,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			50),

	HUNDRED_CATCHES(
			"achievements.hundredCatches.title",
			"achievements.hundredCatches.description",
			150,
			AchievementRarity.LEGENDARY,
			AchievementCategory.CATCH,
			100),

	FIVE_SPECIES(
			"achievements.fiveSpecies.title",
			"achievements.fiveSpecies.description",
			25,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			5),

	TEN_SPECIES(
			"achievements.tenSpecies.title",
			"achievements.tenSpecies.description",
			75,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			10),

	THREE_LOCATIONS(
			"achievements.threeLocations.title",
			"achievements.threeLocations.description",
			15,
			AchievementRarity.COMMON,
			AchievementCategory.EXPLORATION,
			3),

	TEN_LOCATIONS(
			"achievements.tenLocations.title",
			"achievements.tenLocations.description",
			50,
			AchievementRarity.RARE,
			AchievementCategory.EXPLORATION,
			10),

	FIRST_PHOTO(
			"achievements.firstPhoto.title",
			"achievements.firstPhoto.description",
			10,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			1),

	FIRST_LIKE_GIVEN(
			"achievements.firstLikeGiven.title",
			"achievements.firstLikeGiven.description",
			5,
			AchievementRarity.COMMON,
			AchievementCategory.SOCIAL,
			1),

	FIRST_COMMENT(
			"achievements.firstComment.title",
			"achievements.firstComment.description",
			5,
			AchievementRarity.COMMON,
			AchievementCategory.SOCIAL,
			1),

	FIRST_FRIEND(
			"achievements.firstFriend.title",
			"achievements.firstFriend.description",
			5,
			AchievementRarity.COMMON,
			AchievementCategory.SOCIAL,
			1),

	POPULAR_CATCH(
			"achievements.popularCatch.title",
			"achievements.popularCatch.description",
			50,
			AchievementRarity.RARE,
			AchievementCategory.SOCIAL,
			10),

	TROPHY_FISH(
			"achievements.trophyFish.title",
			"achievements.trophyFish.description",
			30,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			1),

	// ── Fishing technique achievements ────────────────────────────────
	// Each technique branch tracks catches whose owning {@link Catch#fishingType} matches the
	// corresponding {@link FishingType} value; multi-fish posts inherit the post-level technique.

	FIRST_FLY(
			"achievements.firstFly.title",
			"achievements.firstFly.description",
			10,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			1),

	FLY_FISHING_PRO(
			"achievements.flyFishingPro.title",
			"achievements.flyFishingPro.description",
			40,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			10),

	FIRST_TROLLING(
			"achievements.firstTrolling.title",
			"achievements.firstTrolling.description",
			10,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			1),

	TROLLING_PRO(
			"achievements.trollingPro.title",
			"achievements.trollingPro.description",
			40,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			10),

	FIRST_ICE(
			"achievements.firstIce.title",
			"achievements.firstIce.description",
			15,
			AchievementRarity.COMMON,
			AchievementCategory.CATCH,
			1),

	ICE_FISHING_PRO(
			"achievements.iceFishingPro.title",
			"achievements.iceFishingPro.description",
			50,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			10),

	VERSATILE_ANGLER(
			"achievements.versatileAngler.title",
			"achievements.versatileAngler.description",
			50,
			AchievementRarity.RARE,
			AchievementCategory.CATCH,
			5),

	TECHNIQUE_MASTER(
			"achievements.techniqueMaster.title",
			"achievements.techniqueMaster.description",
			150,
			AchievementRarity.LEGENDARY,
			AchievementCategory.CATCH,
			10),

	// ── Location / water-type achievements ────────────────────────────
	// Triggered when the user has at least one catch on a location with the matching
	// {@link WaterType}. Trailblazer is special: see {@code AchievementService}.

	STOCKED_LAKE_CATCH(
			"achievements.stockedLakeCatch.title",
			"achievements.stockedLakeCatch.description",
			15,
			AchievementRarity.COMMON,
			AchievementCategory.EXPLORATION,
			1),

	RIVER_CATCH(
			"achievements.riverCatch.title",
			"achievements.riverCatch.description",
			10,
			AchievementRarity.COMMON,
			AchievementCategory.EXPLORATION,
			1),

	OCEAN_CATCH(
			"achievements.oceanCatch.title",
			"achievements.oceanCatch.description",
			25,
			AchievementRarity.RARE,
			AchievementCategory.EXPLORATION,
			1),

	TRAILBLAZER(
			"achievements.trailblazer.title",
			"achievements.trailblazer.description",
			40,
			AchievementRarity.RARE,
			AchievementCategory.EXPLORATION,
			1),

	RANGE_ROAMER(
			"achievements.rangeRoamer.title",
			"achievements.rangeRoamer.description",
			30,
			AchievementRarity.COMMON,
			AchievementCategory.EXPLORATION,
			3);

	private final String titleKey;
	private final String descriptionKey;
	private final int xp;
	private final AchievementRarity rarity;
	private final AchievementCategory category;
	private final int target;

	AchievementCode(String titleKey, String descriptionKey, int xp, AchievementRarity rarity,
			AchievementCategory category, int target) {
		this.titleKey = titleKey;
		this.descriptionKey = descriptionKey;
		this.xp = xp;
		this.rarity = rarity;
		this.category = category;
		this.target = target;
	}

	public String titleKey() {
		return titleKey;
	}

	public String descriptionKey() {
		return descriptionKey;
	}

	public int xp() {
		return xp;
	}

	public AchievementRarity rarity() {
		return rarity;
	}

	public AchievementCategory category() {
		return category;
	}

	public int target() {
		return target;
	}
}
