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

import io.github.kgbis.phatjellyfin.client.model.Item;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import jakarta.inject.Singleton;

import java.util.List;

@SuppressWarnings("unused")
@Singleton
public class ClassicMatcherImpl implements Matcher {

	/**
	 * Fallback matching relies on track number (except if Jellyfin does not have it -
	 * really a very strange case), album and artist
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	@Override
	public boolean fallbackMatch(Item item, ScannedFile scannedFile) {
		boolean trackMatches = trackNumberMatches(item.indexNumber(), scannedFile.track());
		boolean albumMatches = albumNameMatches(item, scannedFile);
		boolean artistMatches = artistMatches(item, scannedFile);

		return trackMatches && albumMatches && artistMatches;
	}

	/**
	 * Check if there's a match for ALBUM_ARTIST or ARTIST
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean artistMatches(Item item, ScannedFile scannedFile) {
		String itemArtist = normalizeToLowercase(item.albumArtist());
		List<String> itemAlbumArtists = item.albumArtists().stream().map(artist -> normalizeToLowercase(artist.getName())).toList();
		String scannedAlbumArtist = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ALBUM_ARTIST));
		String scannedArtist = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ARTIST));

		if (itemAlbumArtists.contains(scannedAlbumArtist) || itemAlbumArtists.contains(scannedArtist)) {
			return true;
		}

		return itemArtist.equals(scannedArtist) || itemArtist.equals(scannedAlbumArtist);
	}

	/**
	 * Check if there's a match for ALBUM name
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean albumNameMatches(Item item, ScannedFile scannedFile) {
		String scannedAlbum = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ALBUM));
		String itemAlbum = normalizeToLowercase(item.album());

		return scannedAlbum.equals(itemAlbum);
	}

	/**
	 * Check if there's a match for track number. In really really strange cases could be
	 * null (no track number). In that case the coming track number from scanned file is
	 * used.
	 * @param itemTrackNum Jellyfin track number
	 * @param scannedTrackNum Scanned track number for track
	 * @return true if matches or false if not
	 */
	private boolean trackNumberMatches(Integer itemTrackNum, Integer scannedTrackNum) {
		// If Jellyfin has not track number, it's a match
		if (itemTrackNum == null) {
			return true;
		}

		return itemTrackNum.equals(scannedTrackNum);
	}

}
