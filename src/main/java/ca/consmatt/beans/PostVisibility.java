package ca.consmatt.beans;

/**
 * Who can see a location post in the feed, on the map, and on your profile.
 */
public enum PostVisibility {
	/** Visible to everyone signed in. */
	PUBLIC,
	/** Visible to you and confirmed friends only. */
	FRIENDS,
	/** Visible only to you (still listed on your own profile). */
	PRIVATE
}
