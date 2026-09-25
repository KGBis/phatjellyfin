package io.github.kgbis.phatjellyfin.config;

import lombok.Getter;

/**
 * Jellyfin
 */
@Getter
public enum JellyfinMetadata {

	// @formatter:off
	TITLE("Title"),
	ALBUM("Album"),
	ARTIST("Artist"),
	ALBUM_ARTIST("Album Artist");
	// @formatter:on

	private final String displayName;

	JellyfinMetadata(String displayName) {
		this.displayName = displayName;
	}

}
