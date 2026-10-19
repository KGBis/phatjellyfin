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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UriQueryBuilderTest {

	@Test
	void whenParamsAreAdded_thenBuildReturnsCorrectUri() {
		UriQueryBuilder underTest = new UriQueryBuilder("https://test.com");
		underTest.queryParam("param1", "value1");
		URI uri = underTest.build();

		assertEquals("https://test.com?param1=value1", uri.toString());

		underTest.queryParam("param2", "value2");
		uri = underTest.build();

		assertEquals("https://test.com?param1=value1&param2=value2", uri.toString());
	}

	@Test
	void whenParamNameIsNull_thenExeptionIsThrown() {
		UriQueryBuilder underTest = new UriQueryBuilder("https://test.com");
		assertThrows(NullPointerException.class, () -> underTest.queryParam(null, "value"));
	}

	@Test
	void whenParamValueIsNull_thenExeptionIsThrown() {
		UriQueryBuilder underTest = new UriQueryBuilder("https://test.com");
		assertThrows(NullPointerException.class, () -> underTest.queryParam("name", null));
	}

}