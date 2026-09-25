package io.github.kgbis.phatjellyfin.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Thin Jellyfin Item object with just the needed fields
 */
// @formatter:off
@JsonIgnoreProperties(ignoreUnknown = true)
public record Item(
		@JsonProperty("Name") String name,

		@JsonProperty("ServerId") String serverId,

		@JsonProperty("Id") String id,

		@JsonProperty("Type") String type,

		@JsonProperty("IsFolder") boolean isFolder,

		@JsonProperty("IndexNumber") Integer indexNumber,

		@JsonProperty("Album") String album,

		@JsonProperty("AlbumArtist") String albumArtist,

		@JsonProperty("AlbumArtists") List<Artist> albumArtists,

		@JsonProperty("ArtistItems") List<Artist> artistItems,

		@JsonProperty("Path") String path) {

	public Item withPath(String path) {
		return new Item(
				name,
				serverId,
				id,
				type,
				isFolder,
				indexNumber,
				album,
				albumArtist,
				albumArtists,
				artistItems,
				path
		);
	}
}
