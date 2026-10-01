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
package io.github.kgbis.phatjellyfin.importer;

import io.github.kgbis.phatjellyfin.client.JellyfinClient;
import io.github.kgbis.phatjellyfin.client.model.Artist;
import io.github.kgbis.phatjellyfin.client.model.Item;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.kgbis.phatjellyfin.client.JellyfinClient.ITEMS_ARTIST_ALBUM_QUERYPARAMS;

@Singleton
public class ArtistHelper {

	private final JellyfinClient jellyfinClient;

	private final Map<String, Artist> artists;

	@Inject
	public ArtistHelper(JellyfinClient jellyfinClient) {
		this.jellyfinClient = jellyfinClient;
		this.artists = new HashMap<>();
	}

	/**
	 * Adds an {@linkplain Artist} to this class' map
	 * @param artist the artist to store
	 */
	public void addArtist(Artist artist) {
		if (artist != null) {
			artists.putIfAbsent(Matcher.normalize(artist.getName()), artist);
		}
	}

	public List<Artist> toArtists(String value) {
		if (value == null) {
			return List.of();
		}

		String customSeparators;
		try {
			customSeparators = jellyfinClient.getCustomTagSeparatorsFromMusicLibrary();
		}
		catch (IOException | InterruptedException e) { // NOSONAR
			customSeparators = "\0";
		}

		// Trim is needed as the last artist seems to end with '\n'
		return Arrays.stream(StringUtils.split(value, customSeparators))
			.map(String::trim)
			.map(this::getOrCreateArtist)
			.toList();
	}

	/**
	 * Tries to return the Album Artist from the parameter
	 * @param artist Album Artist
	 * @return Optional {@linkplain Item}
	 * @throws IOException from HTTP Client
	 * @throws InterruptedException from HTTP Client
	 */
	public Optional<Item> findArtist(String artist) throws IOException, InterruptedException {
		Map<String, String> queryParams = new HashMap<>(ITEMS_ARTIST_ALBUM_QUERYPARAMS);
		queryParams.put("searchTerm", artist);

		return jellyfinClient.getItems(queryParams)
			.items()
			.stream()
			.filter(item -> "MusicArtist".equals(item.type()))
			.filter(item -> !item.isFolder())
			.filter(item -> Matcher.normalize(artist).equals(Matcher.normalize(item.name())))
			.findFirst();
	}

	private Artist getOrCreateArtist(String name) {
		String key = Matcher.normalize(name);
		return artists.computeIfAbsent(key, k -> {
			Artist.ArtistBuilder builder = Artist.builder().name(name);
			try {
				Optional<Item> optionalItem = findArtist(k);
				if (optionalItem.isPresent()) {
					Item item = optionalItem.get();
                    return builder.id(item.id()).build();
				}
				else {
					return builder.build();
				}
			}
			catch (IOException | InterruptedException e) { // NOSONAR
				return builder.build();
			}
		});
	}

}
