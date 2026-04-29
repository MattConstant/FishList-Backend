package ca.consmatt.beans;

/**
 * Display tier derived from an account's total achievement XP. Higher XP unlocks higher tiers.
 * Admins are reported as {@code ADMIN} regardless of XP (handled in the response mapper).
 *
 * Frontend strings live under {@code locales/&#42;/achievements.ts} keyed by {@link #labelKey()}.
 */
public enum RoleTier {
	NEWCOMER(0, "roles.newcomer"),
	ANGLER(25, "roles.angler"),
	ADEPT(75, "roles.adept"),
	VETERAN(150, "roles.veteran"),
	MASTER(300, "roles.master"),
	LEGEND(500, "roles.legend");

	private final int minXp;
	private final String labelKey;

	RoleTier(int minXp, String labelKey) {
		this.minXp = minXp;
		this.labelKey = labelKey;
	}

	public int minXp() {
		return minXp;
	}

	public String labelKey() {
		return labelKey;
	}

	/**
	 * Resolves the highest tier whose threshold is met by the given XP value.
	 * Returns {@link #NEWCOMER} for any non-positive value.
	 */
	public static RoleTier forXp(int xp) {
		RoleTier best = NEWCOMER;
		for (RoleTier tier : values()) {
			if (xp >= tier.minXp) {
				best = tier;
			}
		}
		return best;
	}

	/**
	 * @return the next tier above {@code current}, or {@code null} if already at the top
	 */
	public static RoleTier nextTier(RoleTier current) {
		RoleTier[] values = values();
		for (int i = 0; i < values.length - 1; i++) {
			if (values[i] == current) {
				return values[i + 1];
			}
		}
		return null;
	}
}
