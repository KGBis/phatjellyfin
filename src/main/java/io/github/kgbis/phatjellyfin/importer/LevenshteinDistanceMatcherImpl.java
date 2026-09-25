package io.github.kgbis.phatjellyfin.importer;

import io.github.kgbis.phatjellyfin.client.model.Item;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import jakarta.inject.Singleton;
import org.apache.commons.text.similarity.LevenshteinDistance;

@Singleton
public class LevenshteinDistanceMatcherImpl implements Matcher {

	private static final double TRACK_THRESHOLD = 0.20; // Permisive with track number

	private static final double NO_TRACK_THRESHOLD = 0.08; // Strict w/o track number

	private final LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();

	@Override
	public boolean fallbackMatch(Item item, ScannedFile scannedFile) {
		if (!trackNumberMatches(item.indexNumber(), scannedFile.track())) {
			return false;
		}

		double currentThreshold = getCurrentThreshold(item);
		return albumNameMatches(item, scannedFile, currentThreshold)
				&& artistMatches(item, scannedFile, currentThreshold);
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

	/**
	 * Check if there's a match for ALBUM name
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean albumNameMatches(Item item, ScannedFile scannedFile, double threshold) {
		String scannedAlbum = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ALBUM));
		String itemAlbum = normalizeToLowercase(item.album());

		return isFuzzyMatch(scannedAlbum, itemAlbum, threshold);
	}

	/**
	 * Check if there's a match for ALBUM_ARTIST or ARTIST
	 * @param item Previously unmatched item
	 * @param scannedFile scanned file metadata
	 * @return true if matches or false if not
	 */
	private boolean artistMatches(Item item, ScannedFile scannedFile, double threshold) {
		String scannedAlbumArtist = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ALBUM_ARTIST));
		String scannedArtist = normalizeToLowercase(scannedFile.metadata().get(JellyfinMetadata.ARTIST));

		// Main list: albumArtists
		if (item.albumArtists() != null) {
			boolean matchAlbumArtists = item.albumArtists()
				.stream()
				.map(artist -> normalizeToLowercase(artist.name()))
				.anyMatch(itemArtist -> isFuzzyMatch(itemArtist, scannedArtist, threshold)
						|| isFuzzyMatch(itemArtist, scannedAlbumArtist, threshold));
			if (matchAlbumArtists)
				return true;
		}

		// Secondary list: artistItems
		if (item.artistItems() != null) {
			boolean matchArtistItems = item.artistItems()
				.stream()
				.map(artist -> normalizeToLowercase(artist.name()))
				.anyMatch(itemArtist -> isFuzzyMatch(itemArtist, scannedArtist, threshold)
						|| isFuzzyMatch(itemArtist, scannedAlbumArtist, threshold));
			if (matchArtistItems)
				return true;
		}

		// field AlbumArtist
		if (item.albumArtist() != null) {
			String itemAlbumArtist = normalizeToLowercase(item.albumArtist());
			return isFuzzyMatch(itemAlbumArtist, scannedArtist, threshold)
					|| isFuzzyMatch(itemAlbumArtist, scannedAlbumArtist, threshold);
		}

		// No fuzzy match at all
		return false;
	}

	/**
	 * Compares two strings using Levenshtein distance adapted to their length. Returns
	 * true if the difference is less than or equal to the threshold (typo).
	 */
	private boolean isFuzzyMatch(String s1, String s2, double threshold) {
		if (s1 == null || s2 == null)
			return false;
		if (s1.equals(s2))
			return true;

		int distance = levenshtein.apply(s1, s2);
		int maxLength = Math.max(s1.length(), s2.length());

		if (maxLength == 0)
			return true;

		double errorRatio = (double) distance / maxLength;
		return errorRatio <= threshold;
	}

	private double getCurrentThreshold(Item item) {
		return (item.indexNumber() == null) ? NO_TRACK_THRESHOLD : TRACK_THRESHOLD;
	}

}
