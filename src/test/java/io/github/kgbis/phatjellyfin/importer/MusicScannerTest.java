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
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static io.github.kgbis.phatjellyfin.config.Configuration.dummyConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicScannerTest {

	@TempDir
	Path tempDir;

	@Mock
	private ConfigManager configManager;

	@Mock
	private Console console;

	@Mock
	private MessageManager messageManager;

	@Mock
	private ArtistHelper artistHelper;

	@Mock
	private MusicFileReader musicFileReader;

	@Mock
	private OperationResult<ScanResult> scanResult;

	@InjectMocks
	private MusicScanner underTest;

	@Test
	void when_FolderToScanIsBroken_then_scanResultIsFailure() throws IOException {
		Config config = dummyConfig().toBuilder()
			.rootFolders(Set.of(tempDir.resolve("broken_path")))
			.recursive(true)
			.apply(true)
			.libraryPaths(List.of("/media/music"))
			.defaults(false)
			.build();

		when(configManager.current()).thenReturn(config);
		OperationResult<ScanResult> scanResultOperationResult = underTest.scan();

		assertNotNull(scanResultOperationResult);
		assertFalse(scanResultOperationResult.isSuccess());
	}

	@ParameterizedTest
	@CsvSource({ "false, 2", "true, 3" })
	void when_FolderToScanExists_then_scanResultIsSuccess(boolean recursive, int expected)
			throws IOException, URISyntaxException {
		Path sourceFolder = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("mp3")).toURI());
		Path artistFolder = Files.createDirectory(tempDir.resolve("Artist No1"));

		copyFilesToTemp(sourceFolder, artistFolder);

		Config config = dummyConfig().toBuilder()
			.rootFolders(Set.of(artistFolder))
			.recursive(recursive)
			.apply(true)
			.libraryPaths(List.of("/media/music"))
			.defaults(false)
			.build();

		when(configManager.current()).thenReturn(config);
		doNothing().when(musicFileReader).processFile(any(Path.class));
		OperationResult<ScanResult> scanResultOperationResult = underTest.scan();

		assertTrue(scanResultOperationResult.isSuccess());
		verify(musicFileReader, times(expected)).processFile(any(Path.class));
	}

	/**
	 * Creates a folder structure like: <pre>
	 * Artist No1/
	 * ├── track1.mp3
	 * ├── track2.mp3
	 * └── Album No1/
	 *     └── track3.mp3
	 * </pre>
	 * @param sourceFolder resource mp3 files
	 * @param artistFolder root folder in temp
	 * @throws IOException on error
	 */
	private void copyFilesToTemp(Path sourceFolder, Path artistFolder) throws IOException {
		Path albumFolder = Files.createDirectory(artistFolder.resolve("Album No1"));

		try (Stream<Path> paths = Files.list(sourceFolder)) {
			paths.filter(path -> path.getFileName().toString().startsWith("sample-15s")).forEach(sourceFile -> {
				try {
					Path fileName = sourceFile.getFileName();
					Path targetFile = fileName.toString().equals("sample-15s-some_wanted_tags.mp3")
							? albumFolder.resolve(fileName) : artistFolder.resolve(fileName);
					Files.copy(sourceFile, targetFile);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

}