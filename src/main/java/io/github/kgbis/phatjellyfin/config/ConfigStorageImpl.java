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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kgbis.phatjellyfin.PhatJellyfin;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.kgbis.phatjellyfin.Application.APP_CONFIG_FILE;

@Singleton
@Slf4j
public class ConfigStorageImpl implements ConfigStorage {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final Path configFile;

	public ConfigStorageImpl() {
		this.configFile = locateConfigFile();
	}

	@Override
	public boolean exists() {
		return Files.exists(configFile);
	}

	@Override
	public Config read() throws IOException {
		return mapper.readValue(configFile.toFile(), Config.class);
	}

	@Override
	public void write(Config config) {
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), config);
		}
		catch (IOException e) {
			log.error("Error writting config file", e);
		}
	}

	@Override
	public Path getConfigFile() {
		return configFile;
	}

	private Path locateConfigFile() {
		try {
			return getApplicationDirectory().resolve(APP_CONFIG_FILE).toAbsolutePath();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Error getting config path", e);
		}
	}

	private Path getApplicationDirectory() throws URISyntaxException {
		String appPath = System.getProperty("jpackage.app-path");

		if (StringUtils.isNotBlank(appPath)) {
			return Path.of(appPath).toAbsolutePath().getParent();
		}

		var codeSource = PhatJellyfin.class.getProtectionDomain().getCodeSource();

		if (codeSource != null) {
			Path location = Path.of(codeSource.getLocation().toURI());
			return Files.isDirectory(location) ? location : location.getParent();
		}

		throw new IllegalStateException("Cannot determine application directory");
	}

}