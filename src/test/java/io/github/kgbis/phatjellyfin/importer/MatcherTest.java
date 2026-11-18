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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatcherTest {

	@ParameterizedTest
	@CsvSource({ "¿Quiénes Somos? ¿De Dónde Venimos? ¿Adónde Vamos?, Quienes Somos? De Donde Venimos? Adonde Vamos?",
			"Mötley Crüe, Motley Crue" })
	void normalize_test(String input, String expected) {
		String output = Matcher.normalize(input);
		assertEquals(expected, output);
	}

	@Test
	void normalizeWithExtraSpaces_test() {
		String input = "	A lot    of Spaces in the strïng!	";
		String expected = "A lot of Spaces in the string!";

		String output = Matcher.normalize(input);
		assertEquals(expected, output);
	}

	@ParameterizedTest
	@CsvSource({ "¿Quiénes Somos? ¿De Dónde Venimos? ¿Adónde Vamos?, quienes somos? de donde venimos? adonde vamos?",
			"Mötley Crüe, motley crue" })
	void normalizeLowercase_test(String input, String expected) {
		String output = Matcher.normalizeToLowercase(input);
		assertEquals(expected, output);
	}

}