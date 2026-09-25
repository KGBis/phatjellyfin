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
import io.github.kgbis.phatjellyfin.client.model.UpdateItem;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.kgbis.phatjellyfin.output.Console.ANSI_DEL;

@Singleton
@Slf4j
public class JellyfinWriter {

	private final JellyfinClient jellyfinClient;

	private final Console console;

	private String customSeparators;

	@Inject
	public JellyfinWriter(JellyfinClient jellyfinClient, Console console) {
		this.jellyfinClient = jellyfinClient;
		this.console = console;
	}

	public Map<OperationResult<Void>, List<UpdateItem>> write(Map<MusicKey, Pair<String, TrackMetadata>> albumsToUpdate,
			List<TrackMetadata> tracksToUpdate) throws IOException, InterruptedException {
		// get custom separators for music library
		customSeparators = jellyfinClient.getCustomTagSeparatorsFromMusicLibrary();

		// Gather all tracks and albums to update as a single list of ID+DATA
		List<Pair<String, UpdateItem>> itemsToUpdate = new ArrayList<>();
		itemsToUpdate.addAll(buildTracksToUpdate(tracksToUpdate));
		itemsToUpdate.addAll(buildAlbumsToUpdate(albumsToUpdate));

		if(log.isEnabledForLevel(Level.INFO)) {
			itemsToUpdate.forEach(pair -> log.info("Item to update: {}", pair.getRight()));
		}

		// Update all
		console.println("Updating Jellyfin metadata...");
		Map<OperationResult<Void>, List<UpdateItem>> result = new HashMap<>();

		int size = itemsToUpdate.size();
		for (int i = 0; i < itemsToUpdate.size(); i++) {
			Pair<String, UpdateItem> pair = itemsToUpdate.get(i);
			OperationResult<Void> operationResult = jellyfinClient.updateItem(pair.getLeft(), pair.getRight());
			if (result.containsKey(operationResult)) {
				result.get(operationResult).add(pair.getRight());
			}
			else {
				result.put(operationResult, new ArrayList<>(List.of(pair.getRight())));
			}
			console.progress(i + 1, size);
		}
		console.print("\r" + ANSI_DEL);

		log.debug("Result: {}", result);
		return result;
	}

	private List<Pair<String, UpdateItem>> buildTracksToUpdate(List<TrackMetadata> tracksToUpdate) {
		return tracksToUpdate.stream().map(tm -> {
			List<Artist> artistItems = toArtists(tm.metadata().get(JellyfinMetadata.ARTIST));
			List<Artist> albumArtists = toArtists(tm.metadata().get(JellyfinMetadata.ALBUM_ARTIST));

			UpdateItem body = UpdateItem.builder()
				.title(tm.metadata().get(JellyfinMetadata.TITLE))
				.album(tm.metadata().get(JellyfinMetadata.ALBUM))
				.trackNumber(tm.track())
				.albumArtists(albumArtists)
				.artistItems(artistItems)
				.build();

			return Pair.of(tm.jellyfinId(), body);
		}).toList();
	}

	private List<Pair<String, UpdateItem>> buildAlbumsToUpdate(
			Map<MusicKey, Pair<String, TrackMetadata>> albumsToUpdate) {
		return albumsToUpdate.values().stream().map(album -> {
			String albumId = album.getLeft();
			TrackMetadata sampleTrack = album.getRight();

			List<Artist> artistItems = toArtists(sampleTrack.metadata().get(JellyfinMetadata.ARTIST));
			List<Artist> albumArtists = toArtists(sampleTrack.metadata().get(JellyfinMetadata.ALBUM_ARTIST));

			UpdateItem payload = UpdateItem.builder()
				.title(sampleTrack.metadata().get(JellyfinMetadata.ALBUM))
				.albumArtists(albumArtists)
				.artistItems(artistItems)
				.build();
			return Pair.of(albumId, payload);
		}).toList();
	}

	private List<Artist> toArtists(String value) {
		if (value == null) {
			return List.of();
		}

		// Trim is needed as the last artist seems to end with '\n'
		return Arrays.stream(StringUtils.split(value, customSeparators)).map(s -> new Artist(StringUtils.trim(s), null)).toList();
	}

}
