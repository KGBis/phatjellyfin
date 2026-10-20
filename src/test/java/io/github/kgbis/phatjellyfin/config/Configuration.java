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

import java.util.Map;
import java.util.UUID;

public class Configuration {

    public static Config dummyConfig() {
        JellyfinServerData serverData = new JellyfinServerData("http://localhost:8096", UUID.randomUUID().toString());

        // @formatter:off
        Map<JellyfinMetadata, String> metadata = Map.of(
                JellyfinMetadata.TITLE,        "TITLE_TAG",
                JellyfinMetadata.ARTIST,       "ARTIST_TAG",
                JellyfinMetadata.ALBUM,        "ALBUM_TAG",
                JellyfinMetadata.ALBUM_ARTIST, "ALBUMARTIST_TAG");
        // @formatter:on
        return new Config(serverData, metadata, false);
    }

}
