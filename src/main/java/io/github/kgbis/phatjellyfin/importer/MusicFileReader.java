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

import jakarta.inject.Singleton;
import lombok.Getter;
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

import java.nio.file.Path;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class MusicFileReader {

	private final Pattern pattern = Pattern.compile("^(\\d{4})");

	private Map<JellyfinMetadata, String> metadata;

	@Getter
	private List<ScannedFile> processableFiles;

	@Getter
	private List<ScannedFile> skippableFiles;

	@Getter
	private List<ScannedError> errorFiles;

	public void setConfiguredMetadata(Map<JellyfinMetadata, String> metadata) {
		this.metadata = metadata;
		processableFiles = new ArrayList<>();
		skippableFiles = new ArrayList<>();
		errorFiles = new ArrayList<>();
	}

	/**
	 * Process a file with jAudioTagger and try to extract the wanted tags.
	 * <p>
	 * {@linkplain CannotReadException} usually means the format / content is not
	 * supported, like a text file or so on, so those are igonred
	 * </p>
	 * @param path The file to process
	 */
	public void processFile(Path path) {
		Map<JellyfinMetadata, String> scannedMetadata = new EnumMap<>(JellyfinMetadata.class);

		try {
			AudioFile file = AudioFileIO.read(path.toFile());

			Integer trackNumber = null;
			Integer discNumber = null;
			Integer productionYear = null;
			Set<String> genres = new HashSet<>();

			// It's a MP3
			if (file.getExt().equals(SupportedFileFormat.MP3.getFilesuffix())) {
				MP3File f = (MP3File) file;
				Tag tag = f.getTag();

				// Track number & disc number
				trackNumber = tag.getAll(FieldKey.TRACK).stream().findFirst().map(Integer::parseInt).orElse(null);
				discNumber = tag.getAll(FieldKey.DISC_NO).stream().findFirst().map(Integer::parseInt).orElse(null);
				productionYear = tag.getAll(FieldKey.YEAR).stream().findFirst().map(this::parseYear).orElse(null);
    			genres.addAll(tag.getAll(FieldKey.GENRE));

				// The map with the custom tags should be filled if the TXXX frame exists
				extractMetadata(tag, scannedMetadata);

				if (!scannedMetadata.isEmpty()) {
					log.debug("Scanned {} metadata fields, content: {}", scannedMetadata.size(), scannedMetadata);
				}
			} // end of file tag extraction

			if (scannedMetadata.isEmpty()) {
				skippableFiles.add(new ScannedFile(path.toString(), null, null, null, Set.of(), Map.of()));
			}
			else {
				processableFiles
					.add(new ScannedFile(path.toString(), trackNumber, discNumber, productionYear, genres, scannedMetadata));
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

	private void extractMetadata(Tag tag, Map<JellyfinMetadata, String> scannedMetadata) {
		for (TagField field : tag.getFields("TXXX")) {
			if (!field.isBinary() && field instanceof AbstractID3v2Frame id3v2Frame) {
				String description = id3v2Frame.getBody().getObjectValue("Description").toString();
				String value = id3v2Frame.getBody().getUserFriendlyValue();

				for (Map.Entry<JellyfinMetadata, String> entry : metadata.entrySet()) {
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
	 * Parsing of years due to combinations of tag YEAR and DATE come to jAudioTagger
	 * building a yyyy-MM-dd date or just returning a year made of MMdd values. Hence that
	 * '1231'
	 * @param value Year value returned by jAudioTagger
	 * @return a normalized four digits year or null
	 */
	private Integer parseYear(String value) {
		if (StringUtils.isEmpty(value)) {
			return null;
		}

		java.util.regex.Matcher matcher = pattern.matcher(value);

		if (!matcher.find()) {
			return null;
		}

		int year = Integer.parseInt(matcher.group(1));
		int currentYear = Year.now(ZoneId.systemDefault()).getValue();

		return (year > 1231 && year <= currentYear) ? year : null;
	}

}
