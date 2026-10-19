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
package io.github.kgbis.phatjellyfin.config;

import io.github.kgbis.phatjellyfin.arguments.CliArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.kgbis.phatjellyfin.config.Configuration.dummyConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigManagerTest {

	@Mock
	private ConfigStorage configStorage;

	@Mock
	private Config currentConfig;

	@Mock
	private CliArguments cliArguments;

	@InjectMocks
	private ConfigManager underTest;

	@BeforeEach
	void setUp() {
		underTest.storeArguments(cliArguments);
	}

	@Nested
	class BasicCurrentConfig {

		@Test
		void when_noConfigFileExists_then_throwsConfigException() {
			when(configStorage.exists()).thenReturn(false);
			when(cliArguments.getFolders()).thenReturn(List.of(Path.of("./")));

			// This is to show message and let user set their configuration file
			assertThrows(ConfigException.class, () -> underTest.current());
		}

		@Test
		void when_configFileExistsAndIsDefault_then_throwsConfigException() throws IOException {
			when(configStorage.exists()).thenReturn(true);
			when(configStorage.read()).thenReturn(Config.createNewDefault(null, Map.of()));
			when(cliArguments.getFolders()).thenReturn(List.of(Path.of("./")));

			// This is to show message and let user set their configuration file
			assertThrows(ConfigException.class, () -> underTest.current());
		}

		@Test
		void when_configFileExistsAndIsNotDefault_then_returnsConfig() throws IOException {
			when(configStorage.exists()).thenReturn(true);
			when(configStorage.read()).thenReturn(dummyConfig());

			Config config = assertDoesNotThrow(() -> underTest.current());
			assertNotNull(config);
			assertNotNull(config.getMetadata());
			assertNotNull(config.getServerData());
		}

	}

	@Nested
	class ProgramArgumentsConfig {

		@EnabledOnOs(OS.WINDOWS)
		@Test
		void when_repeatedFoldersInArguments_then_duplicatesAreRemoved_WindowsFilesystem() throws IOException {
			// @formatter:off
			List<Path> argsFolders = List.of(
					Path.of("c:/music/artist 1"),
					Path.of("c:/music/artist 2"),
					Path.of("C:/Music/ArtIst 1")
			);
			// @formatter:on

			when(configStorage.exists()).thenReturn(true);
			when(configStorage.read()).thenReturn(dummyConfig());
			when(cliArguments.getFolders()).thenReturn(argsFolders);

			Config config = underTest.current();
			assertEquals(2, config.getRootFolders().size());
		}

		@Test
		void when_nonRecursiveScanAndChildFolders_then_allFoldersAreKept() throws IOException {
			// @formatter:off
			List<Path> argsFolders = List.of(
					Path.of("c:/music/artist 1"),
					Path.of("c:/music/artist 2"),
					Path.of("c:/music")
			);
			// @formatter:on

			when(configStorage.exists()).thenReturn(true);
			when(configStorage.read()).thenReturn(dummyConfig());
			when(cliArguments.isRecursive()).thenReturn(false);
			when(cliArguments.getFolders()).thenReturn(argsFolders);

			Config config = underTest.current();
			assertEquals(3, config.getRootFolders().size());
		}

		@Test
		void when_recursiveScanAndChildFolders_then_onlyRootsAreKept() throws IOException {
			Path path1 = Path.of("c:/music");
			Path path2 = Path.of("x:/musical");

			// @formatter:off
			List<Path> argsFolders = List.of(
					Path.of("c:/music/artist 1"),
					Path.of("c:/music/artist 2"),
					path1,
					path2
			);
			// @formatter:on

			when(configStorage.exists()).thenReturn(true);
			when(configStorage.read()).thenReturn(dummyConfig());
			when(cliArguments.isRecursive()).thenReturn(true);
			when(cliArguments.getFolders()).thenReturn(argsFolders);

			Config config = underTest.current();
			assertEquals(2, config.getRootFolders().size());
			assertEquals(Set.of(path1, path2), config.getRootFolders());
		}

	}

}