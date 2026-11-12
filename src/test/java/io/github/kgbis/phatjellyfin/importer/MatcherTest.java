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