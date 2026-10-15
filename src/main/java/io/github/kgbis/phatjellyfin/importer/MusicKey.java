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

import io.github.kgbis.phatjellyfin.client.model.Artist;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@AllArgsConstructor
public class MusicKey implements Comparable<MusicKey> {

	private final List<Artist> albumArtists;

	private String album;

	public String toDisplayString() {
		String[] array = albumArtists.stream().map(Artist::getName).toArray(String[]::new);
		String artists = StringUtils.join(array, ",");
		return "album '" + album + "' by " + artists + "...";
	}

	public int compareTo(MusicKey other) {
		int minSize = Math.min(albumArtists.size(), other.albumArtists.size());

		for (int i = 0; i < minSize; i++) {
			int result = albumArtists.get(i).getName()
					.compareToIgnoreCase(other.albumArtists.get(i).getName());

			if (result != 0) {
				return result;
			}
		}

		int result = Integer.compare(albumArtists.size(), other.albumArtists.size());
		return result != 0 ? result : album.compareToIgnoreCase(other.album);
	}

}
