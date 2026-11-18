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
package io.github.kgbis.phatjellyfin.client;

import io.github.kgbis.phatjellyfin.config.Config;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.JellyfinServerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JellyfinClientTest {

	@Mock
	private ConfigManager configManager;

	@InjectMocks
	private JellyfinClient underTest;

	@Test
	void when_UrlIsMissing_then_throwsIllegalArgumentException() throws IOException {
		Config config = Config.builder()
			.rootFolders(Set.of(Path.of("anypath")))
			.recursive(true)
			.apply(true)
			.serverData(new JellyfinServerData("", "anyAPIkey"))
			.defaults(false)
			.build();
		when(configManager.current()).thenReturn(config);
		assertThrows(IllegalArgumentException.class, () -> underTest.getServerBasicInfo());
	}

	@Test
	void when_APIKeyIsMissing_then_throwsIllegalArgumentException() throws IOException {
		Config config = Config.builder()
			.rootFolders(Set.of(Path.of("anypath")))
			.recursive(true)
			.apply(true)
			.serverData(new JellyfinServerData("http://localhost:8096", null))
			.defaults(false)
			.build();
		when(configManager.current()).thenReturn(config);
		assertThrows(IllegalArgumentException.class, () -> underTest.getServerBasicInfo());
	}

}