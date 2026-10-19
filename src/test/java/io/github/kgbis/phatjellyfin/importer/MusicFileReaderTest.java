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

import ch.qos.logback.classic.Level;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MusicFileReaderTest {

	private MusicFileReader underTest;

	@BeforeAll
	static void configureLogging() {
		Logger logger = LoggerFactory.getLogger("org.jaudiotagger");
		((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
	}

	@BeforeEach
	void setUp() {
		Map<JellyfinMetadata, String> metadata = Map.of(JellyfinMetadata.ALBUM, "JF_ALBUM",
				JellyfinMetadata.ALBUM_ARTIST, "JF_ALBUMARTIST", JellyfinMetadata.ARTIST, "JF_ARTIST",
				JellyfinMetadata.TITLE, "JF_TITLE");

		underTest = new MusicFileReader();
		underTest.setConfiguredMetadata(metadata);
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			// filename ->                    Error, Processable, Skippable
			"testdata_corrupt.mp3,            1,     0,           0",
			"sample-15s-id3v2.mp3,            0,     0,           1",
			"sample-3s-8khz.wav,              0,     0,           1",
			"sample-15s-all_wanted_tags.mp3,  0,     1,           0",
			"sample-15s-some_wanted_tags.mp3, 0,     1,           0"
	})
	// @formatter:on
	void when_processFile_thenClassifiesFileCorrectly(String fileName, int expectedErrors, int expectedProcessable,
			int expectedSkippable) {

		underTest.processFile(getPath(fileName));

		assertAll(() -> assertEquals(expectedErrors, underTest.getErrorFiles().size()),
				() -> assertEquals(expectedProcessable, underTest.getProcessableFiles().size()),
				() -> assertEquals(expectedSkippable, underTest.getSkippableFiles().size()));
	}

	private Path getPath(String fileName) {
		URL resource = getClass().getResource("/mp3/" + fileName);
		File file = new File(Objects.requireNonNull(resource).getFile());
		return Path.of(file.getPath());
	}

}