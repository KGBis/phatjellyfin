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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class Helper {

	private final JellyfinClient jellyfinClient;

	private final Map<String, Artist> artists;

	@Inject
	public Helper(JellyfinClient jellyfinClient) {
		this.jellyfinClient = jellyfinClient;
		this.artists = new HashMap<>();
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
			.map(s -> Artist.builder().name(s).build())
			.toList();
	}

	public void addArtist(Artist artist) {
        if(artist != null) {
			artists.put(Matcher.normalize(artist.getName()), artist);
		}
    }
}
