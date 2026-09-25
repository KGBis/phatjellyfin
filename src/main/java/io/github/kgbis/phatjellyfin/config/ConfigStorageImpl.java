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
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
@NoArgsConstructor
@Slf4j
public class ConfigStorageImpl implements ConfigStorage {

	private static final ObjectMapper mapper = new ObjectMapper();

	private static final Path configFile = ConfigStorage.getConfigFile();

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

}