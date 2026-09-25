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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Minimum payload needed to update a Jellyfin Item. In this case to update an album or a
 * track.
 */
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateItem {

	@JsonProperty("Name")
	private String title;

	@JsonProperty("Album")
	private String album;

	@JsonProperty("IndexNumber")
	private Integer trackNumber;

	@JsonProperty("Genres")
	@Builder.Default
	private List<String> genres = List.of();

	@JsonProperty("Tags")
	@Builder.Default
	private List<String> tags = List.of();

	@JsonProperty("ProviderIds")
	@Builder.Default
	private Map<String, String> providerIds = Map.of();

	@JsonProperty("AlbumArtists")
	private List<Artist> albumArtists;

	@JsonProperty("ArtistItems")
	private List<Artist> artistItems;

}