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
import io.github.kgbis.phatjellyfin.config.ConfigStorage;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.kgbis.phatjellyfin.output.Console.ANSI_DEL;
import static io.github.kgbis.phatjellyfin.output.MessageManager.SCAN_ERROR_SUMMARY;
import static io.github.kgbis.phatjellyfin.output.MessageManager.SCAN_SUCCESS_SUMMARY;

@Singleton
@Slf4j
public class MusicScanner {

	private final ConfigManager configManager;

	private final Console console;

	private OperationResult<ScanResult> scanResult;

	@Inject
	public MusicScanner(ConfigManager configManager, Console console) {
		this.configManager = configManager;
		this.console = console;
	}

	public boolean scan() throws IOException, ConfigException {
		log.debug("Using config directory: {}", ConfigStorage.getConfigFile());
		Config config = configManager.current();

		log.info("Starting music scanner with config: {}", config);
		String recursive = config.isRecursive() ? " recursively" : "";
		console.println("Starting scan music files in '%s'%s...".formatted(config.getRootFolder(), recursive));

		// get the scanning result
		scanResult = scanFiles(config);
		log.info("Scan finished with {}", scanResult.isSuccess() ? "success" : "error");

		// Show summary (if success)
		showSummary();

		// return true if success and at least one file to process
		return scanResult.isSuccess() && !scanResult.getResult().scannedFiles().isEmpty();

	}

	public Map<MusicKey, List<ScannedFile>> prepareScannedLibrary() throws IOException {
		// Group scanned track files by artist + album
		Map<MusicKey, List<ScannedFile>> preparedScannedData = prepareScannedData(
				scanResult.getResult().scannedFiles());

		// normalize scanned tracks to get rid of the root folder used in the scanning
		String rootPath = configManager.current().getRootFolder().toString();
		return preparedScannedData.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> getNormalizedScanned(rootPath, entry.getValue())));
	}

	private @NonNull List<ScannedFile> getNormalizedScanned(String rootPath, List<ScannedFile> scannedFiles) {
		return scannedFiles.stream()
			.map(scannedFile -> scannedFile
				.withPath(scannedFile.path().substring(rootPath.length()).replace('\\', '/')))
			.toList();
	}

	private OperationResult<ScanResult> scanFiles(Config config) {
		Path rootFolder = config.getRootFolder();

		// status and message (success unless exception is thrown)
		boolean success = true;
		String errorCause = null;

		// jAudioTagger supported extensions
		Set<String> supportedExtensions = getSupportedExtensions();

		List<ScannedFile> processableFiles = new ArrayList<>();
		List<ScannedFile> skippableFiles = new ArrayList<>();
		List<ScannedError> errorFiles = new ArrayList<>();

		// to finetune the scanning message
		AtomicReference<String> oldStrPath = new AtomicReference<>("");

		try (Stream<Path> pathStream = config.isRecursive() ? Files.walk(rootFolder) : Files.list(rootFolder)) {
			pathStream.filter(Files::isRegularFile)
				.filter(path -> isSupportedExtension(path, supportedExtensions))
				.forEach(path -> {
					String strPath = path.getParent() != null ? path.getParent().toString() : path.toString();
					if (!strPath.equalsIgnoreCase(oldStrPath.get())) {
						console.print("\r%sScanning music folder '%s'".formatted(ANSI_DEL, strPath));
						oldStrPath.set(strPath);
					}

					processFile(config, path, processableFiles, skippableFiles, errorFiles);
				});

			console.print("\r" + ANSI_DEL);
		}
		catch (IOException e) {
			success = false;
			errorCause = e.getMessage();
		}

		return new OperationResult<>(success, new ScanResult(processableFiles, skippableFiles, errorFiles), errorCause);
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
	 * Process a file with jAudioTagger and try to extract the wanted tags.
	 * <p>
	 * {@linkplain CannotReadException} usually means the format / content is not
	 * supported, like a text file or so on, so those are igonred
	 * </p>
	 * @param config Current configuration
	 * @param path The file to process
	 * @param processableFiles List of files that HAVE the wanted metadata
	 * @param skippableFiles List of files that DO NOT HAVE the wanted metadata
	 * @param errorFiles List of files that could not be read / scanned
	 */
	private void processFile(Config config, Path path, List<ScannedFile> processableFiles,
			List<ScannedFile> skippableFiles, List<ScannedError> errorFiles) {
		Map<JellyfinMetadata, String> wantedMetadata = config.getMetadata();
		Map<JellyfinMetadata, String> scannedMetadata = new EnumMap<>(JellyfinMetadata.class);

		try {
			AudioFile file = AudioFileIO.read(path.toFile());

			Integer trackNumber = null;

			// It's a MP3
			if (file.getExt().equals(SupportedFileFormat.MP3.getFilesuffix())) {
				log.info("Found MP3 file '{}'", path);
				MP3File f = (MP3File) file;
				Tag tag = f.getTag();

				// Track number
				trackNumber = tag.getAll(FieldKey.TRACK).stream().findFirst().map(Integer::parseInt).orElse(null);

				// If genre will be used -> tag.getAll(FieldKey.GENRE)

				// The map with the custom tags should be filled if the TXXX frame exists
				extractMetadata(tag, wantedMetadata, scannedMetadata);

				log.info("Scanned {} metadata fields, content: {}", scannedMetadata.size(), scannedMetadata);
			} // end of file tag extraction

			if (scannedMetadata.isEmpty()) {
				skippableFiles.add(new ScannedFile(path.toString(), null, Map.of()));
			}
			else {
				processableFiles.add(new ScannedFile(path.toString(), trackNumber, scannedMetadata));
			}
		}
		catch (CannotReadException _) {
			log.debug("Audio file cannot be read: {}", path);
		}
		catch (Exception e) {
			log.error("Error reading file: {}", path, e);
			errorFiles.add(new ScannedError(path, e.getMessage(), e));
		}
	}

	private void extractMetadata(Tag tag, Map<JellyfinMetadata, String> wantedMetadata,
			Map<JellyfinMetadata, String> scannedMetadata) {
		for (TagField field : tag.getFields("TXXX")) {
			log.debug("Found TXXX field {}", field);
			if (!field.isBinary() && field instanceof AbstractID3v2Frame id3v2Frame) {
				String description = id3v2Frame.getBody().getObjectValue("Description").toString();
				String value = id3v2Frame.getBody().getUserFriendlyValue();

				for (Map.Entry<JellyfinMetadata, String> entry : wantedMetadata.entrySet()) {
					// In the wanted metadata map we find the field in TXXX
					if (entry.getValue().equals(description)) {
						scannedMetadata.put(entry.getKey(), value);
						break;
					}
				}
			}
		}
	}

	/**
	 * Group scanned track files by {@linkplain MusicKey} (ALBUM_ARTIST + ALBUM)
	 * @param scannedFiles Locally scanned files
	 * @return Grouping map
	 */
	private Map<MusicKey, List<ScannedFile>> prepareScannedData(List<ScannedFile> scannedFiles) {
		return scannedFiles.stream()
			.flatMap(sf -> Arrays.stream(StringUtils.split(sf.metadata().get(JellyfinMetadata.ALBUM_ARTIST), '\0'))
				.map(artist -> Map.entry(new MusicKey(artist, sf.metadata().get(JellyfinMetadata.ALBUM)), sf)))
			.collect(Collectors.groupingBy(Map.Entry::getKey,
					Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
	}

	private void showSummary() throws IOException {
		Config config = configManager.current();

		if (scanResult.isSuccess()) {
			ScanResult result = scanResult.getResult();
			log.info("Files to update: {}, Skipped: {}, Failed to read: {}", result.scannedFiles().size(),
					result.skippedFiles().size(), result.errorFiles().size());

			console.println(SCAN_SUCCESS_SUMMARY.formatted(result.scannedFiles().size(), result.skippedFiles().size(),
					result.errorFiles().size()));

			if (result.scannedFiles().isEmpty()) {
				console.println("No files to update, PhatJellyfin will now exit.");
			}
		}
		else {
			console.println(SCAN_ERROR_SUMMARY.formatted(config.getRootFolder(), scanResult.getErrorCause()));
		}
	}

}
