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
import io.github.kgbis.phatjellyfin.client.model.UpdateItem;
import io.github.kgbis.phatjellyfin.output.Console;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.kgbis.phatjellyfin.output.Console.ANSI_DEL;

@Singleton
@Slf4j
public class JellyfinWriter {

	private final JellyfinClient jellyfinClient;

	private final Console console;

	@Inject
	public JellyfinWriter(JellyfinClient jellyfinClient, Console console, Helper helper) {
		this.jellyfinClient = jellyfinClient;
		this.console = console;
	}

	public Map<OperationResult<Void>, List<UpdateItem>> write(Map<MusicKey, AlbumToUpdate> matchedItemsV2)
			throws IOException, InterruptedException {
		// Gather all tracks and albums to update as a single list of ID+DATA
		List<UpdateItem> itemsToUpdate = buildDataToUpdate(matchedItemsV2);

		if (log.isEnabledForLevel(Level.INFO)) {
			itemsToUpdate.forEach(item -> log.info("Item to update: {}", item));
		}

		// Update all
		console.println("Updating Jellyfin metadata...");
		Map<OperationResult<Void>, List<UpdateItem>> result = new HashMap<>();

		int size = itemsToUpdate.size();
		for (int i = 0; i < itemsToUpdate.size(); i++) {
			UpdateItem updateItem = itemsToUpdate.get(i);
			OperationResult<Void> operationResult = jellyfinClient.updateItem(updateItem.getJellyfinId(), updateItem);
			addResultToMap(result, operationResult, updateItem);
			console.progress(i + 1, size);
		}
		console.print("\r" + ANSI_DEL);

		log.debug("Result: {}", result);
		return result;
	}

	private void addResultToMap(Map<OperationResult<Void>, List<UpdateItem>> result,
			OperationResult<Void> operationResult, UpdateItem updateItem) {
		if (result.containsKey(operationResult)) {
			result.get(operationResult).add(updateItem);
		}
		else {
			result.put(operationResult, new ArrayList<>(List.of(updateItem)));
		}
	}

	private List<UpdateItem> buildDataToUpdate(Map<MusicKey, AlbumToUpdate> matchedItemsV2) {
		return matchedItemsV2.values().stream().flatMap(album -> {
			UpdateItem albumToUpdate = UpdateItem.builder()
				.jellyfinId(album.getJellyfinId())
				.title(album.getTitle())
				.albumArtists(album.getAlbumArtists())
				.artistItems(album.getAlbumArtists())
				.build();
			return Stream.concat(Stream.of(albumToUpdate),
					album.getTracks()
						.stream()
						.map(track -> UpdateItem.builder()
							.jellyfinId(track.getJellyfinId())
							.title(track.getTitle())
							.album(track.getAlbum())
							.trackNumber(track.getNumber())
							.albumArtists(track.getAlbumArtists())
							.artistItems(track.getTrackArtists())
							.build()));

		}).toList();
	}

}
