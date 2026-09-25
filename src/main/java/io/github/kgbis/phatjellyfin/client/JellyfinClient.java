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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kgbis.phatjellyfin.client.model.Items;
import io.github.kgbis.phatjellyfin.client.model.Library;
import io.github.kgbis.phatjellyfin.client.model.SystemInfo;
import io.github.kgbis.phatjellyfin.client.model.SystemInfoStorage;
import io.github.kgbis.phatjellyfin.client.model.UpdateItem;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.importer.OperationResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Class to make HTTP calls to Jellyfin
 */
@Singleton
@Slf4j
public class JellyfinClient {

	// @formatter:off
	public static final Map<String, String> ITEMS_ARTIST_ALBUM_QUERYPARAMS = Map.of(
			"IncludeItemTypes", "MusicAlbum,MusicArtist,Artist",
			"recursive", "true"
			// then "searchTerm", "<album>|<artist>"
	);

	public static final Map<String, String> ITEMS_TRACKS_QUERYPARAMS = Map.of(
			"IncludeItemTypes", "Audio",
			"recursive", "true",
			"Fields", "Album,Path,AlbumArtist,AlbumArtists,Artists,ProviderIds,IndexNumber"
			// then "ParentId, "<id>"
	);
	// @formatter:on

	private static final String SYSTEM_INFO = "System/Info";

	private static final String SYSTEM_INFO_STORAGE = SYSTEM_INFO + "/Storage";

	private static final String ITEMS = "Items";

	private static final String ITEMS_UPDATE = ITEMS + "/:itemId";

	private static final String LIBRARY = "Library";

	private static final String VIRTUAL_FOLDERS = LIBRARY + "/VirtualFolders";

	private static final String REFRESH_LIBRARY = LIBRARY + "/Refresh";

	private final HttpClient httpClient;

	private final ConfigManager configManager;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private String baseUrl;

	private Map<String, String> headers;

	private String customSeparators = null;

	@Inject
	public JellyfinClient(ConfigManager configManager) {
		this.configManager = configManager;
		this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
	}

	public SystemInfo getServerBasicInfo() throws IOException, InterruptedException {
		HttpRequest request = request(SYSTEM_INFO, Map.of(), Map.of()).GET().build();
		return httpClient.send(request, JsonBodyHandler.ofJson(SystemInfo.class)).body();
	}

	public List<Library> getLibraries() throws IOException, InterruptedException {
		HttpRequest request = request(SYSTEM_INFO_STORAGE, Map.of(), Map.of()).GET().build();
		return httpClient.send(request, JsonBodyHandler.ofJson(SystemInfoStorage.class)).body().libraries();
	}

	public OperationResult<Void> refreshLibrary() throws IOException, InterruptedException {
		HttpRequest request = request(REFRESH_LIBRARY, Map.of(), Map.of()).POST(HttpRequest.BodyPublishers.noBody())
			.build();
		HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
		return getResultFromResponse(response);
	}

	public String getCustomTagSeparatorsFromMusicLibrary() throws IOException, InterruptedException {
		if (customSeparators != null) {
			return customSeparators;
		}

		HttpRequest request = request(VIRTUAL_FOLDERS, Map.of(), Map.of()).GET().build();
		List<Library> libraries = httpClient.send(request, JsonBodyHandler.ofJson(new TypeReference<List<Library>>() {
		})).body();

		customSeparators = libraries.stream()
			.filter(lib -> "music".equalsIgnoreCase(lib.collectionType()))
			.map(Library::libraryOptions)
			// .filter(LibraryOptions::useCustomTagsDelimiter)
			.map(options -> String.join("", options.customTagDelimiters()))
			.findFirst()
			.orElse("") + "\0";

		return customSeparators;
	}

	public Items getItems(Map<String, String> queryParams) throws IOException, InterruptedException {
		HttpRequest request = request(ITEMS, queryParams, Map.of()).GET().build();
		return httpClient.send(request, JsonBodyHandler.ofJson(Items.class)).body();
	}

	public OperationResult<Void> updateItem(String id, UpdateItem payload) throws IOException, InterruptedException {
		String body = objectMapper.writeValueAsString(payload);

		HttpRequest request = request(ITEMS_UPDATE, Map.of(), Map.of(":itemId", id))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		log.info("Update returned status code {}, message {}", response.statusCode(), response.body());

		return getResultFromResponse(response);
	}

	private HttpRequest.Builder request(String endpoint, Map<String, String> queryParams, Map<String, String> pathVars)
			throws IOException {

		ensureConfigured();

		String baseUri = StringUtils.joinWith("/", baseUrl, endpoint);

		for (var entry : pathVars.entrySet()) {
			baseUri = Strings.CS.replace(baseUri, entry.getKey(), entry.getValue());
		}

		UriQueryBuilder uriQueryBuilder = new UriQueryBuilder(baseUri);
		queryParams.forEach(uriQueryBuilder::queryParam);

		URI uri = uriQueryBuilder.build();
		log.info("URI: {}", uri);

		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);

		headers.forEach(builder::header);

		return builder;
	}

	private void ensureConfigured() throws IOException {
		if (headers == null || baseUrl == null) {
			// Store base URL without final forward slash, if any
			this.baseUrl = Strings.CI.removeEnd(configManager.current().getServerData().url(), "/");
			this.headers = Map.of("Accept", "application/json", "X-Emby-Token",
					configManager.current().getServerData().apiKey());
		}
	}

	private <T> OperationResult<T> getResultFromResponse(HttpResponse<?> response) {
		if (response.statusCode() >= 200 && response.statusCode() < 300) {
			return new OperationResult<>(true, null, null);
		}

		// in case of error: Build a Result object with the code and message
		return new OperationResult<>(false, null, "HTTP %s - %s".formatted(response.statusCode(), response.body()));
	}

}
