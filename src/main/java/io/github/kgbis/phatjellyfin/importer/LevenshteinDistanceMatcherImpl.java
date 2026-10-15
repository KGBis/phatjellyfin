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
import io.github.kgbis.phatjellyfin.client.model.Item;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.List;

@Singleton
public class LevenshteinDistanceMatcherImpl implements Matcher {

	private static final double RELAXED_THRESHOLD = 0.20; // Permisive with track number

	private static final double STRICT_THRESHOLD = 0.17; // Strict w/o track number

	private final ArtistHelper artistHelper;

	private final LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();

	@Inject
	public LevenshteinDistanceMatcherImpl(ArtistHelper artistHelper) {
		this.artistHelper = artistHelper;
	}

	@Override
	public boolean fallbackMatch(Item item, ScannedFile scannedFile) {
		if (!trackNumberMatches(item.indexNumber(), scannedFile.track())) {
			return false;
		}

		double currentThreshold = getCurrentThreshold(item, scannedFile);
		return albumNameMatches(item, scannedFile)
				&& artistMatches(item, scannedFile, currentThreshold);
	}

	/**
	 * Check if there's a match for track number. Any track number could be null (no track
	 * number either in Jellyfin or in scanned file). In that case we set result as
	 * {@code true} to let the matching process continue.
	 * @param itemTrackNum Jellyfin track number
	 * @param scannedTrackNum Scanned track number for track
	 * @return true if matches (or any of the niumbers is null, so to continue) or false
	 * if not
	 */
	private boolean trackNumberMatches(Integer itemTrackNum, Integer scannedTrackNum) {
		// If Jellyfin or scanned file has no track number, let the process continue
		if (itemTrackNum == null || scannedTrackNum == null) {
			return true;
		}

		return itemTrackNum.equals(scannedTrackNum);
	}

	/**
	 * Check if there's a match for ALBUM name. It always uses {@linkplain STRICT_THRESHOLD}
	 *
	 * @param item        Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean albumNameMatches(Item item, ScannedFile scannedFile) {
		String scannedAlbum = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ALBUM));
		String itemAlbum = normalizeToLowercase(item.album());

		return isFuzzyMatch(scannedAlbum, itemAlbum, LevenshteinDistanceMatcherImpl.STRICT_THRESHOLD);
	}

	/**
	 * Check if there's a match for ALBUM_ARTIST or ARTIST
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean artistMatches(Item item, ScannedFile scannedFile, double threshold) {
		List<String> scannedAlbumArtists = getArtistList(scannedFile.metadata().get(JellyfinMetadata.ALBUM_ARTIST));
		List<String> scannedArtists = getArtistList(scannedFile.metadata().get(JellyfinMetadata.ARTIST));

		// Main list: albumArtists
		if (item.albumArtists() != null) {
			boolean matchAlbumArtists = item.albumArtists()
				.stream()
				.map(artist -> normalizeToLowercase(artist.getName()))
				.anyMatch(itemArtist -> matchesAnyArtist(threshold, itemArtist, scannedArtists, scannedAlbumArtists));
			if (matchAlbumArtists)
				return true;
		}

		// Secondary list: artistItems
		if (item.artistItems() != null) {
			boolean matchArtistItems = item.artistItems()
				.stream()
				.map(artist -> normalizeToLowercase(artist.getName()))
				.anyMatch(itemArtist -> matchesAnyArtist(threshold, itemArtist, scannedArtists, scannedAlbumArtists));
			if (matchArtistItems)
				return true;
		}

		// field AlbumArtist
		if (item.albumArtist() != null) {
			String itemAlbumArtist = normalizeToLowercase(item.albumArtist());
			return matchesAnyArtist(threshold, itemAlbumArtist, scannedArtists, scannedAlbumArtists);
		}

		// No fuzzy match at all
		return false;
	}

	private List<String> getArtistList(String fieldValue) {
		return artistHelper.toArtists(fieldValue)
			.stream()
			.map(Artist::getName)
			.map(this::normalizeToLowercase)
			.toList();
	}

	private boolean matchesAnyArtist(double threshold, String itemArtist, List<String> scannedArtists,
			List<String> scannedAlbumArtists) {

		return scannedArtists.stream().anyMatch(scannedArtist -> isFuzzyMatch(itemArtist, scannedArtist, threshold))
				|| scannedAlbumArtists.stream()
					.anyMatch(scannedAlbumArtist -> isFuzzyMatch(itemArtist, scannedAlbumArtist, threshold));
	}

	/**
	 * Compares two strings using Levenshtein distance adapted to their length. Returns
	 * true if the difference is less than or equal to the threshold (typo).
	 */
	private boolean isFuzzyMatch(String s1, String s2, double threshold) {
		if (StringUtils.isBlank(s1) || StringUtils.isBlank(s2)) {
			return false;
		}

		if (s1.equals(s2) || matchesIgnoringThe(s1, s2)) {
			return true;
		}

		int distance = levenshtein.apply(s1, s2);
		int maxLength = Math.max(s1.length(), s2.length());

		if (maxLength == 0)
			return true;

		double errorRatio = (double) distance / maxLength;
		return errorRatio <= threshold;
	}

	private boolean matchesIgnoringThe(String artist1, String artist2) {
		return artist1.startsWith("the ") && artist1.substring(4).equals(artist2)
				|| artist2.startsWith("the ") && artist2.substring(4).equals(artist1);
	}

	private double getCurrentThreshold(Item item, ScannedFile scannedFile) {
		return (item.indexNumber() == null || scannedFile.track() == null) ? STRICT_THRESHOLD : RELAXED_THRESHOLD;
	}

}
