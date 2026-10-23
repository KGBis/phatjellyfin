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

import io.github.kgbis.phatjellyfin.config.Config;
import io.github.kgbis.phatjellyfin.config.ConfigException;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.kgbis.phatjellyfin.output.Console.ANSI_DEL;
import static io.github.kgbis.phatjellyfin.output.MessageManager.SCAN_ERROR_SUMMARY;

@Singleton
@Slf4j
public class MusicScanner {

	private final ConfigManager configManager;

	private final Console console;

	private final MessageManager messageManager;

	private final ArtistHelper artistHelper;

	private final MusicFileReader musicFileReader;

	private OperationResult<ScanResult> scanResult;

	@Inject
	public MusicScanner(ConfigManager configManager, Console console, MessageManager messageManager,
			ArtistHelper artistHelper, MusicFileReader musicFileReader) {
		this.configManager = configManager;
		this.console = console;
		this.messageManager = messageManager;
		this.artistHelper = artistHelper;
		this.musicFileReader = musicFileReader;
	}

	public OperationResult<ScanResult> scan() throws IOException, ConfigException {
		log.debug("Using config directory: {}", configManager.getConfigFile());
		Config config = configManager.current();
		musicFileReader.setConfiguredMetadata(config.getMetadata());
		log.info("Starting music scanner with config: {}", config);

		// get the scanning result
		scanResult = scanFiles(config);
		log.info("Scan finished with {}", scanResult.isSuccess() ? "success" : "error");

		// Show summary (if success)
		showSummary();

		// return the result. It'll be the Importer the one to take care of it
		return scanResult;
	}

	public Map<MusicKey, List<ScannedFile>> prepareScannedData() throws IOException {
		// Group scanned track files by artist + album
		List<ScannedFile> scannedFiles = scanResult.getResult().scannedFiles();
		Map<MusicKey, List<ScannedFile>> preparedScannedData = prepareScannedData(scannedFiles);

		// normalize scanned tracks to get rid of the root folder used in the scanning
		Set<Path> rootFolders = configManager.current().getRootFolders();
		return preparedScannedData.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> getNormalizedScanned(rootFolders, entry.getValue()),
					(a, _) -> a, TreeMap::new));
	}

	/**
	 * Change back slashes to forward slashes and normalize the filename
	 * @param rootFolders Paths where scan started
	 * @param scannedFiles list of scanned files
	 * @return the normalized scanned file
	 */
	private List<ScannedFile> getNormalizedScanned(Collection<Path> rootFolders, List<ScannedFile> scannedFiles) {
		return scannedFiles.stream().map(scannedFile -> {
			Path path = Path.of(scannedFile.path());
			Path rootFolder = rootFolders.stream().filter(path::startsWith).findFirst().orElseThrow();

			return scannedFile.withPath(Matcher.normalize(rootFolder.relativize(path).toString().replace('\\', '/')));
		}).sorted(Comparator.comparing(ScannedFile::path)).toList();
	}

	private OperationResult<ScanResult> scanFiles(Config config) {
		String recursive = config.isRecursive() ? " recursively" : "";

		// status and message (success unless exception is thrown)
		boolean success = true;
		String errorCause = null;

		// jAudioTagger supported extensions
		Set<String> supportedExtensions = getSupportedExtensions();

		console.print(messageManager.buildStartScanMessage(config.getRootFolders(), recursive));

		// to finetune the scanning message
		AtomicReference<String> oldStrPath = new AtomicReference<>("");

		try (Stream<Path> pathStream = getAllPathsStream(config)) {
			pathStream.filter(Files::isRegularFile)
				.filter(path -> isSupportedExtension(path, supportedExtensions))
				.forEach(path -> {
					printScanningProgress(path, oldStrPath);
					musicFileReader.processFile(path);
				});

			console.print("\r" + ANSI_DEL);
		}
		catch (UncheckedIOException e) {
			success = false;
			errorCause = e.getMessage();
		}

		return new OperationResult<>(success, new ScanResult(musicFileReader.getProcessableFiles(),
				musicFileReader.getSkippableFiles(), musicFileReader.getErrorFiles()), errorCause);
	}

	private Stream<Path> getAllPathsStream(Config config) {
		return config.getRootFolders().stream().flatMap(path -> {
			try {
				if (config.isRecursive()) {
					return Files.walk(path);
				}
				else {
					return Files.list(path);
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private void printScanningProgress(Path path, AtomicReference<String> oldStrPath) {
		String strPath = path.getParent() != null ? path.getParent().toString() : path.toString();
		if (!strPath.equalsIgnoreCase(oldStrPath.get())) {
			console.print("\r%sScanning music folder '%s'".formatted(ANSI_DEL, strPath));
			oldStrPath.set(strPath);
		}
	}

	/**
	 * Gets jAudioTagger supported extensions from its {@linkplain SupportedFileFormat}
	 * enum
	 * @return jAudioTagger supported extension set
	 */
	private @NonNull Set<String> getSupportedExtensions() {
		return Arrays.stream(SupportedFileFormat.values())
			.filter(supportedFileFormat -> !supportedFileFormat.equals(SupportedFileFormat.UNKNOWN))
			.map(format -> format.getFilesuffix().toLowerCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * checks if file extension is one of the jAudioTagger ones
	 * @param path File path
	 * @param supportedExtensions jAudioTagger supported extensions
	 * @return if supported
	 */
	private boolean isSupportedExtension(Path path, Set<String> supportedExtensions) {
		return supportedExtensions
			.contains(StringUtils.substringAfterLast(path.getFileName().toString(), ".").toLowerCase(Locale.ROOT));
	}

	/**
	 * Group scanned track files by {@linkplain MusicKey} (ALBUM_ARTIST + ALBUM)
	 * @param scannedFiles Locally scanned files
	 * @return Grouping map
	 */
	private Map<MusicKey, List<ScannedFile>> prepareScannedData(List<ScannedFile> scannedFiles) {
		return scannedFiles.stream()
			.collect(Collectors
				.groupingBy(sf -> new MusicKey(artistHelper.toArtists(sf.metadata().get(JellyfinMetadata.ALBUM_ARTIST)),
						sf.metadata().get(JellyfinMetadata.ALBUM))));

	}

	private void showSummary() throws IOException {
		Config config = configManager.current();

		if (scanResult.isSuccess()) {
			ScanResult result = scanResult.getResult();
			log.info("Files to update: {}, Skipped: {}, Failed to read: {}", result.scannedFiles().size(),
					result.skippedFiles().size(), result.errorFiles().size());

			String s = messageManager.buildScanSuccessMessage(result.scannedFiles().size(),
					result.skippedFiles().size(), result.errorFiles().size());
			console.println(s);

			if (result.scannedFiles().isEmpty()) {
				console.println("No files to update, PhatJellyfin will now exit.");
			}
		}
		else {
			console.println(SCAN_ERROR_SUMMARY.formatted(config.getRootFolders(), scanResult.getErrorCause()));
		}
	}

}
