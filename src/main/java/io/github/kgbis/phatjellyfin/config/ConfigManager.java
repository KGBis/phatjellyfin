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
import io.github.kgbis.phatjellyfin.client.model.Folder;
import io.github.kgbis.phatjellyfin.client.model.Library;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class ConfigManager {

	private final ConfigStorage configStorage;

	private Config currentConfig;

	private CliArguments cliArguments;

	@Inject
	public ConfigManager(ConfigStorage configStorage) {
		this.configStorage = configStorage;
	}

	public void storeArguments(CliArguments cliArguments) {
		this.cliArguments = cliArguments;
	}

	/**
	 * Returns current, if already set, or load stored configuration
	 * @return Config object
	 * @throws IOException if I/O error on read
	 * @throws ConfigException if config file didn't exist and configuration was created
	 * on spot
	 */
	public synchronized Config current() throws IOException, ConfigException {
		if (currentConfig != null) {
			return currentConfig;
		}

		currentConfig = loadConfig();

		if (currentConfig.isDefaults()) {
			throw new ConfigException(ConfigException.NO_CONFIG_FILE_MSG);
		}

		return currentConfig;
	}

	public Config loadConfig() throws IOException {
		Config config;
		if (configStorage.exists()) {
			config = configStorage.read();
		}
		else {
			config = getDefaultConfig();
		}

		return addOtherProperties(config);
	}

	public Config getDefaultConfig() {
		JellyfinServerData serverData = new JellyfinServerData("http://localhost:8096", null);

		// @formatter:off
		Map<JellyfinMetadata, String> metadata = Map.of(
				JellyfinMetadata.TITLE,        "TITLE",
				JellyfinMetadata.ARTIST,       "ARTIST",
				JellyfinMetadata.ALBUM,        "ALBUM",
				JellyfinMetadata.ALBUM_ARTIST, "ALBUMARTIST"//,
				/*JellyfinMetadata.GENRE,        "GENRE"*/);
		// @formatter:on

		Config config = Config.createNewDefault(serverData, metadata);
		configStorage.write(config);
		return config;
	}

	public void addServerLibraries(List<Library> libraries) {
		List<String> list = libraries.stream()
			.flatMap(library -> library.folders().stream())
			.map(Folder::path)
			.toList();
		currentConfig = currentConfig.toBuilder().libraryPaths(list).build();
	}

	private Config addOtherProperties(Config config) {
		return config.toBuilder()
			.apply(cliArguments.isApply())
			.recursive(cliArguments.isRecursive())
			.rootFolder(cliArguments.getFolder())
			.build();
	}

}
