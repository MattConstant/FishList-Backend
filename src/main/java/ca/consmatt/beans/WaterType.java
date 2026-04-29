package ca.consmatt.beans;

/**
 * Body of water classification on a {@link Location}. {@code null} on a row means "not specified" —
 * legacy locations created before this column existed stay null and don't qualify for water-type
 * achievements.
 *
 * Names are stable (used in DB + API). Frontend strings live under {@code locales/&#42;/catch.ts}
 * keyed by {@code catch.waterType.&lt;lowercase enum name&gt;}.
 */
public enum WaterType {
	LAKE,
	STOCKED_LAKE,
	POND,
	RIVER,
	STREAM,
	RESERVOIR,
	OCEAN,
	OTHER
}
