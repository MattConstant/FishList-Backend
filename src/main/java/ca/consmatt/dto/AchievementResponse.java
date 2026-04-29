package ca.consmatt.dto;

import ca.consmatt.beans.AchievementCategory;
import ca.consmatt.beans.AchievementRarity;

/**
 * One achievement (locked or unlocked) with the viewer's current progress toward its target.
 *
 * @param code stable enum name (used as React key and to look up frontend metadata)
 * @param titleKey i18n title key
 * @param descriptionKey i18n description key
 * @param xp xp granted on unlock
 * @param rarity visual tier
 * @param category logical group ("CATCH" / "EXPLORATION" / "SOCIAL")
 * @param target threshold required to unlock (e.g. 10 catches → 10)
 * @param progress current progress toward {@code target} (capped at {@code target})
 * @param unlocked whether the user already earned this achievement
 * @param unlockedAt ISO-8601 instant of unlock (null when locked)
 */
public record AchievementResponse(
		String code,
		String titleKey,
		String descriptionKey,
		int xp,
		AchievementRarity rarity,
		AchievementCategory category,
		int target,
		int progress,
		boolean unlocked,
		String unlockedAt) {
}
