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

import io.github.kgbis.phatjellyfin.client.model.UpdateItem;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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

	private StopWatch stopWatch;

	@Inject
	public Importer(ConfigManager configManager, MessageManager messageManager, Console console,
			MusicScanner musicScanner, JellyfinMatcher jellyfinMatcher, JellyfinWriter jellyfinWriter) {
		this.configManager = configManager;
		this.messageManager = messageManager;
		this.console = console;
		this.musicScanner = musicScanner;
		this.jellyfinMatcher = jellyfinMatcher;
		this.jellyfinWriter = jellyfinWriter;
	}

	/**
	 * Importer entry point
	 */
	public void run(StopWatch stopWatch) throws IOException, InterruptedException {
		this.stopWatch = stopWatch;
		if (scanSuccessful(musicScanner.scan()) && shouldApply()) {
			Map<MusicKey, List<ScannedFile>> scannedData = musicScanner.prepareScannedData();
			Map<MusicKey, AlbumToUpdate> matchingItems = jellyfinMatcher.matchAgainstJellyfin(scannedData);
			Map<OperationResult<Void>, List<UpdateItem>> writeResult = jellyfinWriter.write(matchingItems);
			processWriteResult(writeResult);
		}
	}

	/**
	 * Checks if the provided arguments "scan directories" exist, they're a directory and
	 * they're readable (permissions)
	 * @param musicFolders Root directories to check (1..n)
	 * @return The result of the operation
	 */
	public OperationResult<Void> rootFoldersExistAndReadable(Collection<Path> musicFolders) {
		for (Path musicFolder : musicFolders) {
			if (!Files.exists(musicFolder)) {
				return new OperationResult<>(false, null, String.format(PATH_DOES_NOT_EXIST_MESSAGE, musicFolder));
			}

			if (!Files.isDirectory(musicFolder)) {
				return new OperationResult<>(false, null, String.format(PATH_NOT_A_DIRECTORY_MESSAGE, musicFolder));
			}
			if (!Files.isReadable(musicFolder)) {
				return new OperationResult<>(false, null, String.format(PATH_NOT_READABLE_MESSAGE, musicFolder));
			}
		}

		return new OperationResult<>(true, null, null);
	}

	/**
	 * Checks if the scan process was successful
	 * @param scanResult The library/libraries scan result
	 * @return true if success and at least one file to process
	 */
	private boolean scanSuccessful(OperationResult<ScanResult> scanResult) {
		return scanResult.isSuccess() && !scanResult.getResult().scannedFiles().isEmpty();
	}

	/**
	 * If program arguments didn't include {@code --apply} ask for confirmation to apply
	 * changes
	 * @return if program will continue or stop
	 * @throws IOException on {@linkplain ConfigManager} I/O error
	 */
	private boolean shouldApply() throws IOException {
		if (configManager.current().isApply()) {
			return true;
		}

		// To pause chrono. No reason to keep counting while waiting for user interaction
		stopWatch.suspend();
		try {
			return console.confirm("Apply changes to Jellyfin?");
		}
		finally {
			stopWatch.resume();
		}
	}

	private void processWriteResult(Map<OperationResult<Void>, List<UpdateItem>> writeResult) {
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
