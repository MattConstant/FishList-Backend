package ca.consmatt.dto;

import ca.consmatt.beans.AchievementRarity;

/**
 * Inline notification payload returned from mutation endpoints when one or more achievements
 * unlocked as a side effect (e.g. adding a catch trips FIRST_CATCH).
 *
 * @param code stable enum name, used by the frontend to fetch its localized text
 * @param titleKey i18n title key
 * @param descriptionKey i18n description key
 * @param xp xp granted by this achievement
 * @param rarity visual tier
 */
public record UnlockedAchievementSummary(
		String code,
		String titleKey,
		String descriptionKey,
		int xp,
		AchievementRarity rarity) {
}
