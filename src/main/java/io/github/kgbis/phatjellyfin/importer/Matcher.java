package io.github.kgbis.phatjellyfin.importer;

import io.github.kgbis.phatjellyfin.client.model.Item;

import java.text.Normalizer;
import java.util.Locale;

public interface Matcher {

	static String normalize(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}

	default String normalizeToLowercase(String value) {
		return Matcher.normalize(value).toLowerCase(Locale.ROOT);
	}

	boolean fallbackMatch(Item item, ScannedFile scannedFile);

}
