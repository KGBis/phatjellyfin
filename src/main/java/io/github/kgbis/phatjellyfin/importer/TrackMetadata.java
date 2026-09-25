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

import io.github.kgbis.phatjellyfin.client.model.Item;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;

import java.util.Map;

public record TrackMetadata(
        String jellyfinId,
        String path,
        Integer track,
        Map<JellyfinMetadata, String> metadata) {

    public static TrackMetadata from(Item item, ScannedFile scannedFile) {
        return new TrackMetadata(item.id(), item.path(), scannedFile.track(), scannedFile.metadata());
    }
}
