/*
 * PhatJellyfin
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
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
