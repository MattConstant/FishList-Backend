package ca.consmatt.beans;

/**
 * Fishing technique used for a catch. {@code null} on a catch means "not specified" — old rows
 * created before this column was added stay null and the UI hides the badge for them.
 *
 * Names are stable (used in DB + API). Frontend strings live under {@code locales/&#42;/catch.ts}
 * keyed by {@code catch.fishingType.&lt;lowercase enum name&gt;}.
 */
public enum FishingType {
	FLY,
	SPIN,
	BAITCAST,
	TROLLING,
	ICE,
	JIGGING,
	BOTTOM,
	FLOAT,
	SURF,
	OTHER
}
