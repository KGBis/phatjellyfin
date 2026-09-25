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
package io.github.kgbis.phatjellyfin.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Items response that holds a list of {@linkplain Item}
 * @param items List of items returned
 * @param totalRecordCount Total records returned
 * @param startIndex From where (pagination stuff)
 */
// @formatter:off
public record Items(
		@JsonProperty("Items")
		List<Item> items,

		@JsonProperty("TotalRecordCount")
		int totalRecordCount,

		@JsonProperty("StartIndex")
		int startIndex) {
}
