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