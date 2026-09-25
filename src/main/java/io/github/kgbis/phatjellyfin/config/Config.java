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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Config {

	@JsonIgnore
	private Path rootFolder;

	@JsonIgnore
	private boolean recursive;

	@JsonIgnore
	private boolean apply;

	private JellyfinServerData serverData;

	private Map<JellyfinMetadata, String> metadata;

	@JsonIgnore
	private List<String> libraryPaths;

	@JsonIgnore
	private boolean defaults;

	protected Config(JellyfinServerData serverData, Map<JellyfinMetadata, String> metadata, boolean defaults) {
		this.serverData = serverData;
		this.metadata = metadata;
		this.defaults = defaults;
	}

	// Static utility method for creating a NEW configuration. 'defaults' will be true
	public static Config createNewDefault(JellyfinServerData serverData, Map<JellyfinMetadata, String> metadata) {
		return new Config(serverData, metadata, true);
	}

}