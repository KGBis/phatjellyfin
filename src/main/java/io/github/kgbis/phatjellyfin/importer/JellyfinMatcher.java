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
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.kgbis.phatjellyfin.client.JellyfinClient.ITEMS_ARTIST_ALBUM_QUERYPARAMS;
import static io.github.kgbis.phatjellyfin.client.JellyfinClient.ITEMS_TRACKS_QUERYPARAMS;

@Singleton
@Slf4j
public class JellyfinMatcher {

	private final ConfigManager configManager;

	private final Console console;

	private final JellyfinClient jellyfinClient;

	private final Matcher matcher;

	private final Helper helper;

	private List<String> libraryPaths; // NOSONAR

	@Inject
	public JellyfinMatcher(ConfigManager configManager, Console console, JellyfinClient jellyfinClient, Matcher matcher,
			Helper helper) {
		this.configManager = configManager;
		this.console = console;
		this.jellyfinClient = jellyfinClient;
		this.matcher = matcher;
		this.helper = helper;
	}

	/**
	 * Matches scanned library data with the data gathered from Jellyfin
	 * @param preparedLibrary Map of scanned tracks grouped by album
	 * @return Albums and tracks to update
	 */
	public Map<MusicKey, AlbumToUpdate> matchAgainstJellyfin(Map<MusicKey, List<ScannedFile>> preparedLibrary)
			throws IOException {
		// initialize data structure
		Map<MusicKey, AlbumToUpdate> albumsToUpdateMap = new HashMap<>();

		// Jellyfin library paths
		libraryPaths = configManager.current().getLibraryPaths();

		for (Map.Entry<MusicKey, List<ScannedFile>> entry : preparedLibrary.entrySet()) {
			// Get tracks from Jellyfin
			AlbumToUpdate albumToUpdate = getMatchingTracksV3(entry.getKey(), entry.getValue());

			if (albumToUpdate != null) {
				albumsToUpdateMap.put(entry.getKey(), albumToUpdate);
			}
		}

		console.printSameLine("");

		return albumsToUpdateMap;
	}

	/**
	 * Get Album tracks from Jellyfin. Tries to find by album with album artist as
	 * fallback. Then match against the list of scanned files
	 * @param musicKey ALBUMARTIST + ALBUM key to find by.
	 * @param scannedFiles List if scanned files
	 * @return The album with all the matching tracks
	 */
	AlbumToUpdate getMatchingTracksV3(MusicKey musicKey, List<ScannedFile> scannedFiles) {
		try {
			// Find either album or artist
			console.printSameLine("Getting Jellyfin tracks for %s".formatted(musicKey.toDisplayString()));
			Optional<Item> optionalAlbum = findJellyfinAlbum(musicKey);

			// No results
			if (optionalAlbum.isEmpty()) {
				log.warn("Album not found: {} - {}", musicKey.getAlbumArtists(), musicKey.getAlbum());
				return null;
			}

			Item jellyfinAlbum = optionalAlbum.get();
			log.debug("Found Item '{}' ({})", jellyfinAlbum.name(), jellyfinAlbum.id());

			// fill artists with their ids
			jellyfinAlbum.artistItems()
				.forEach(artistItem -> musicKey.getAlbumArtists()
					.stream()
					.filter(artist -> Matcher.normalize(artist.getName())
						.equals(Matcher.normalize(artistItem.getName())))
					.findFirst()
					.ifPresent(artist -> {
						artist.setId(artistItem.getId());
						helper.addArtist(artist);
					}));

			// get tracks with normalized paths (no library prefix)
			List<Item> jellyfinTracks = getJellyfinTracks(jellyfinAlbum);

			// once all normalized match paths to know if the track should be updated
			List<TrackMetadata> matchingTracks = matchTracks(jellyfinTracks, scannedFiles);
			log.debug("Matching tracks: {}", matchingTracks.stream()
				.map(tm -> "Track: %s - Name: %s".formatted(tm.track(), tm.metadata().get(JellyfinMetadata.TITLE))));

			// transform the list of TrackMetadata to the Album-Tracks structure
			List<TrackToUpdate> tracksToUpdate = matchingTracks.stream()
				.map(tm -> TrackToUpdate.builder()
					.jellyfinId(tm.jellyfinId())
					.number(tm.track())
					.title(tm.metadata().get(JellyfinMetadata.TITLE))
					.album(tm.metadata().get(JellyfinMetadata.ALBUM))
					.albumArtists(musicKey.getAlbumArtists())
					.trackArtists(helper.toArtists(tm.metadata().get(JellyfinMetadata.ARTIST)))
					.build())
				.toList();

			if (!tracksToUpdate.isEmpty()) {
				TrackToUpdate sampleTrack = tracksToUpdate.getFirst();
				return AlbumToUpdate.builder()
					.jellyfinId(jellyfinAlbum.id())
					.title(sampleTrack.getAlbum())
					.albumArtists(musicKey.getAlbumArtists())
					.tracks(tracksToUpdate)
					.build();
			}

			return null;
		}
		catch (IOException | InterruptedException e) { // NOSONAR
			throw new RuntimeException("Unable to export album: " + musicKey.getAlbum(), e);
		}
	}

	/**
	 * Returns the Jellyfin {@linkplain Item} that matches the required album or artist
	 * from the {@linkplain MusicKey}
	 * @param musicKey Album artist and Album key
	 * @return Optional of Jellyfin {@linkplain Item}
	 * @throws IOException from HTTP client
	 * @throws InterruptedException from HTTP client
	 */
	private Optional<Item> findJellyfinAlbum(MusicKey musicKey) throws IOException, InterruptedException {
		Optional<Item> album = findAlbumV2(musicKey, null);

		if (album.isPresent()) {
			log.info("Album found: {}", album.get().name());
			return album;
		}

		for (Artist albumArtist : musicKey.getAlbumArtists()) {
			Optional<Item> optionalArtist = findArtist(albumArtist.getName());
			if (optionalArtist.isPresent()) {
				Item artist = optionalArtist.get();
				log.info("Artist found: {}", artist.name());
				String artistId = artist.id();
				return findAlbumV2(musicKey, artistId);
			}
		}

		return Optional.empty();
	}

	/**
	 * Tries to find the album from the {@linkplain MusicKey}. It uses the Album Artist to
	 * avoid wrong matches (same album name)
	 * @param musicKey Key containing Album Artist and Album
	 * @return Optional {@linkplain Item}
	 * @throws IOException from HTTP Client
	 * @throws InterruptedException from HTTP Client
	 */
	private Optional<Item> findAlbumV2(MusicKey musicKey, String id) throws IOException, InterruptedException {
		log.info("Searching album: artist='{}', album='{}', artistId='{}'", musicKey.getAlbumArtists(),
				musicKey.getAlbum(), id);

		Map<String, String> queryParams = new HashMap<>(ITEMS_ARTIST_ALBUM_QUERYPARAMS);
		if (StringUtils.isNotEmpty(id)) {
			queryParams.put("AlbumArtistIds", id);
		}
		else {
			queryParams.put("searchTerm", musicKey.getAlbum());
		}

		return jellyfinClient.getItems(queryParams)
			.items()
			.stream()
			.filter(item -> "MusicAlbum".equals(item.type()))
			.filter(item -> sameAlbum(musicKey, item))
			.filter(item -> musicKey.getAlbumArtists()
				.stream()
				.anyMatch(albumArtist -> item.artistItems()
					.stream()
					.anyMatch(artist -> sameArtist(albumArtist.getName(), artist.getName()))))
			.findFirst();
	}

	private boolean sameArtist(String artist1, String artist2) {
		log.debug("sameArtist() -> {}, {}", Matcher.normalize(artist1), Matcher.normalize(artist2));
		return Matcher.normalize(artist1).equals(Matcher.normalize(artist2));
	}

	private boolean sameAlbum(MusicKey musicKey, Item item) {
		log.debug("sameAlbum() -> {}, {}", Matcher.normalize(musicKey.getAlbum()), Matcher.normalize(item.name()));
		return Matcher.normalize(musicKey.getAlbum()).equals(Matcher.normalize(item.name()));
	}

	/**
	 * Tries to return the Album Artist from the parameter
	 * @param artist Album Artist
	 * @return Optional {@linkplain Item}
	 * @throws IOException from HTTP Client
	 * @throws InterruptedException from HTTP Client
	 */
	private Optional<Item> findArtist(String artist) throws IOException, InterruptedException {
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

	/**
	 * Returns the tracks found with a normalized path removing the Jellyfin library
	 * prefix
	 * @param musicItem The album from where to find its tracks
	 * @return List of tracks
	 * @throws IOException from HTTP Client
	 * @throws InterruptedException from HTTP Client
	 */
	private @NonNull List<Item> getJellyfinTracks(Item musicItem) throws IOException, InterruptedException {
		List<Item> tracks = findTracks(musicItem.id());
		return tracks.stream().map(i -> {
			for (String path : libraryPaths) {
				if (i.path().startsWith(path)) {
					return i.withPath(i.path().substring(path.length()));
				}
			}
			return musicItem;
		}).toList();
	}

	/**
	 * Returns the tracks from the parent Id
	 * @param id Parent Id
	 * @return List of {@linkplain Item}
	 * @throws IOException from HTTP Client
	 * @throws InterruptedException from HTTP Client
	 */
	private List<Item> findTracks(String id) throws IOException, InterruptedException {
		Map<String, String> queryParams = new HashMap<>(ITEMS_TRACKS_QUERYPARAMS);
		queryParams.put("ParentId", id);
		List<Item> items = jellyfinClient.getItems(queryParams).items();
		log.debug("Found {} items in '{}' ({}).", items.size(), items, id);
		return items;
	}

	/**
	 * Matches Jellyfin tracks against library scanned ones
	 * @param normalizedTracks Jellyfin tracks
	 * @param normalizedScanned library scanned
	 * @return List of matches
	 */
	private @NonNull List<TrackMetadata> matchTracks(List<Item> normalizedTracks, List<ScannedFile> normalizedScanned) {
		List<TrackMetadata> matchingTracks = new ArrayList<>();
		StopWatch stopWatch = StopWatch.createStarted();
		normalizedTracks.forEach(track -> {
			for (ScannedFile sf : normalizedScanned) {
				if (track.path().endsWith(sf.path())) {
					matchingTracks.add(TrackMetadata.from(track, sf));
					break;
				}
			}
		});

		stopWatch.stop();

		log.debug("Scanned tracks size: {}, matching tracks size: {}, Took {}", normalizedScanned.size(),
				matchingTracks.size(), stopWatch);

		// If all files matched, return the result as it is
		if (normalizedScanned.size() == matchingTracks.size()) {
			return matchingTracks;
		}

		// get all those Items that didn't match
		Set<String> matchedIds = matchingTracks.stream().map(TrackMetadata::jellyfinId).collect(Collectors.toSet());

		List<Item> unmatchedTracks = normalizedTracks.stream()
			.filter(track -> !matchedIds.contains(track.id()))
			.toList();

		unmatchedTracks.forEach(item -> {
			for (ScannedFile scannedFile : normalizedScanned) {
				if (matcher.fallbackMatch(item, scannedFile)) {
					matchingTracks.add(TrackMetadata.from(item, scannedFile));
					break;
				}
			}
		});

		return matchingTracks;
	}

}
