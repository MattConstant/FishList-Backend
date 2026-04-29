package ca.consmatt.dto;

import java.util.List;

/**
 * Achievements page payload for one account: identity + role/XP summary + per-achievement state.
 *
 * @param accountId account primary key
 * @param username login name
 * @param profileImageKey optional storage key for avatar
 * @param xp total XP from unlocked achievements
 * @param roleTier highest reached tier ("NEWCOMER".."LEGEND")
 * @param roleTierLabelKey i18n key for the tier label
 * @param nextRoleTier the tier above {@code roleTier}, or null when at the top
 * @param nextRoleTierLabelKey i18n key for the next tier (null when at the top)
 * @param nextRoleTierMinXp XP threshold for the next tier (null when at the top)
 * @param admin true when the account holds the ADMIN role (overlays the regular tier)
 * @param unlockedCount number of achievements already earned
 * @param totalCount size of the catalog
 * @param achievements per-achievement state (locked + unlocked) sorted unlocked-first
 */
public record AchievementProgressResponse(
		Long accountId,
		String username,
		String profileImageKey,
		int xp,
		String roleTier,
		String roleTierLabelKey,
		String nextRoleTier,
		String nextRoleTierLabelKey,
		Integer nextRoleTierMinXp,
		boolean admin,
		int unlockedCount,
		int totalCount,
		List<AchievementResponse> achievements) {
}
