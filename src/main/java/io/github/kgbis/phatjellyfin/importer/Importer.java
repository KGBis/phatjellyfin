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
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class Importer {

	private static final String PATH_DOES_NOT_EXIST_MESSAGE = "Path does not exist: '%s'";

	private static final String PATH_NOT_A_DIRECTORY_MESSAGE = "Path is not a directory: '%s'";

	private static final String PATH_NOT_READABLE_MESSAGE = "Path is not readable: '%s'";

	private final ConfigManager configManager;

	private final MessageManager messageManager;

	private final Console console;

	private final MusicScanner musicScanner;

	private final JellyfinMatcher jellyfinMatcher;

	private final JellyfinWriter jellyfinWriter;

	private final JellyfinClient jellyfinClient;

	@Inject
	public Importer(ConfigManager configManager, MessageManager messageManager, Console console,
			MusicScanner musicScanner, JellyfinMatcher jellyfinMatcher, JellyfinWriter jellyfinWriter,
			JellyfinClient jellyfinClient) {
		this.configManager = configManager;
		this.messageManager = messageManager;
		this.console = console;
		this.musicScanner = musicScanner;
		this.jellyfinMatcher = jellyfinMatcher;
		this.jellyfinWriter = jellyfinWriter;
		this.jellyfinClient = jellyfinClient;
	}

	/**
	 * Importer entry point
	 */
	public void run() throws IOException, InterruptedException {
		if (musicScanner.scan() && shouldApply()) {
			Map<MusicKey, List<ScannedFile>> preparedLibrary = musicScanner.prepareScannedLibrary();
			MatchedItems matchedItems = jellyfinMatcher.matchWithLibrary(preparedLibrary);

			if (noMatches(matchedItems)) {
				console.println("Found no matching items to update in Jellyfin...\nPhatJellyfin will now exit.");
				return;
			}
			Map<OperationResult<Void>, List<UpdateItem>> writeResult = jellyfinWriter.write(matchedItems.albums(),
					matchedItems.tracks());
			processWriteResult(writeResult);
		}
	}

	private boolean noMatches(MatchedItems matchedItems) {
		return matchedItems.albums().isEmpty() && matchedItems.tracks().isEmpty();
	}

	/**
	 * Checks if the provided argument scan directory exists, is a directory and is
	 * readable (permissions)
	 * @param musicFolder Root directory to check
	 * @return The result of the operation
	 */
	public OperationResult<Void> rootFolderExistsAndReadable(Path musicFolder) {
		if (!Files.exists(musicFolder)) {
			return new OperationResult<>(false, null, String.format(PATH_DOES_NOT_EXIST_MESSAGE, musicFolder));
		}

		if (!Files.isDirectory(musicFolder)) {
			return new OperationResult<>(false, null, String.format(PATH_NOT_A_DIRECTORY_MESSAGE, musicFolder));
		}
		if (!Files.isReadable(musicFolder)) {
			return new OperationResult<>(false, null, String.format(PATH_NOT_READABLE_MESSAGE, musicFolder));
		}

		return new OperationResult<>(true, null, null);
	}

	private boolean shouldApply() throws IOException {
		if (configManager.current().isApply()) {
			return true;
		}

		return console.confirm("Apply changes to Jellyfin?");
	}

	private void processWriteResult(Map<OperationResult<Void>, List<UpdateItem>> writeResult)
			throws IOException, InterruptedException {
		int success = 0;
		int failures = 0;
		List<String> failureItems = new ArrayList<>();

		for (Map.Entry<OperationResult<Void>, List<UpdateItem>> entry : writeResult.entrySet()) {
			OperationResult<Void> operationResult = entry.getKey();
			List<UpdateItem> updateItems = entry.getValue();
			if (operationResult.isSuccess()) {
				success = success + updateItems.size();
			}
			else {
				failures = failures + updateItems.size();
				failureItems.addAll(updateItems.stream().map(item -> {
					String data = getFailureString(item);
					return MessageManager.FAILED_ITEM.formatted(data, operationResult.getErrorCause());
				}).toList());
			}
		}

		String failedUpdates = messageManager.buildFailedUpdates(failureItems);
		String updateResult = messageManager.buildUpdateResult(success, failures, failedUpdates);

		log.info("Update result: {}", updateResult);
		console.println(updateResult);

		refreshLibrary(success);
	}

	/**
	 * Refresh library if at least one item was updated
	 * @param success Number of updated items
	 * @throws IOException HTTP Client Exception
	 * @throws InterruptedException HTTP Client Exception
	 */
	private void refreshLibrary(int success) throws IOException, InterruptedException {
		if (success > 0) {
			console.println("Jellyfin music library will be refreshed...");
			jellyfinClient.refreshLibrary();
		}
	}

	private @NonNull String getFailureString(UpdateItem item) {
		String data;
		if (item.getAlbum() == null) { // album
			data = "Album " + item.getTitle();
		}
		else {
			data = "Album %s, track %s - %s".formatted(item.getAlbum(),
					StringUtils.leftPad("" + item.getTrackNumber(), 2, "0"), item.getTitle());
		}
		return data;
	}

}
