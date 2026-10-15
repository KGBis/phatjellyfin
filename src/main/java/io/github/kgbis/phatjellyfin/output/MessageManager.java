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
package io.github.kgbis.phatjellyfin.output;

import io.github.kgbis.phatjellyfin.config.Config;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.util.List;

import static io.github.kgbis.phatjellyfin.Application.APP_CONFIG_FILE;

@Singleton
public class MessageManager {

	// Application Logo
	public static final String LOGO = """
			   ___ _           _    __       _ _        __ _      \s
			  / _ \\ |__   __ _| |_  \\ \\  ___| | |_   _ / _(_)_ __ \s
			 / /_)/ '_ \\ / _` | __|  \\ \\/ _ \\ | | | | | |_| | '_ \\\s
			/ ___/| | | | (_| | |_/\\_/ /  __/ | | |_| |  _| | | | |
			\\/    |_| |_|\\__,_|\\__\\___/ \\___|_|_|\\__, |_| |_|_| |_|
			Because your Jellyfin deserves proper|___/ metadata too\s
			""";

	// Metadata configuration line template
	public static final String METADATA_MAPPING_TEMPLATE = "Using \"%s\" for Jellyfin %s";

	// Configuration summary template. Uses Config and METADATA_MAPPING_TEMPLATE
	public static final String CONFIG_MESSAGE = """
			    Jellyfin configuration:
			      Server URL: %s
			      API Key: %s

			    Metadata mapping:
			      %s
			""";

	// No configuration template. Uses CONFIG_MESSAGE and
	public static final String NO_CONFIG_MESSAGE = """
			No configuration file was found. A default configuration has been created:

			%s
			Please review and edit the configuration file before running PhatJellyfin again.
			Configuration file: %s

			PhatJellyfin will now exit.
			""";

	// Server info
	public static final String SERVER_INFO_MESSAGE = """
			    Jellyfin server found:
			      Name: %s
			      Version: %s
			      Address: %s
			""";

	public static final String API_KEY_REPLACEMENT = "$1************************$2";

	public static final String API_KEY_NOT_CONFIGURED = "not configured";

	public static final String SCAN_ERROR_SUMMARY = """
			Folder scan finished for '%s' with error: %s

			PhatJellyfin will now exit.
			""";

	public static final String UPDATE_SUMMARY = """
			Jellyfin Metadata update finished.
			    Update summary:
			      Updated %s item%s
			      Failed %s item%s
			    %s
			""";

	public static final String FAILED_ITEM = "%s, reason: %s";

	private static final String SCAN_SUCCESS_SUMMARY = """
			Folder scan finished.
			    Scan summary:
			      Found %s file%s to update
			      Found %s file%s to skip (no custom tags)
			      Found %s file%s failed to read
			""";

	private static final String MATCHING_SUMMARY = """
			Library matching finished.
			    Matching summary:
			      Matched %s track%s
			      Matched %s album%s
			      Unmatched %s track%s
			      Unmatched %s album%s
			""";

	private static final String SCAN_FOLDERS = """
			Folders to scan:
			%s
			Starting music scan%s...
			""";

	private static final String API_KEY_REGEX = "^(.{4}).*(.{4})$";

	private final ConfigManager configManager;

	@Inject
	public MessageManager(ConfigManager configManager) {
		this.configManager = configManager;
	}

	@SneakyThrows
	public String buildConfigMessage() {
		Config config = configManager.current();

		// Build metadata section
		StringBuilder stringBuilder = new StringBuilder();
		config.getMetadata()
			.entrySet()
			.stream()
			.map(entry -> METADATA_MAPPING_TEMPLATE.formatted(entry.getValue(), entry.getKey().getDisplayName()))
			.sorted()
			.forEach(s -> stringBuilder.append(s).append("\n").append(spaces(6)));
		String metadata = stringBuilder.toString();

		String apiKey = config.getServerData().apiKey() == null ? API_KEY_NOT_CONFIGURED
				: config.getServerData().apiKey().replaceAll(API_KEY_REGEX, API_KEY_REPLACEMENT);

		return CONFIG_MESSAGE.formatted(config.getServerData().url(), apiKey, metadata);
	}

	public String buildNoConfigMessage() {
		return NO_CONFIG_MESSAGE.formatted(buildConfigMessage(), APP_CONFIG_FILE);
	}

	public String buildUpdateResult(int success, int failures, String s) {
		return UPDATE_SUMMARY.formatted(success, sOrBlank(success), failures, sOrBlank(failures), s);
	}

	public String buildFailedUpdates(List<String> failureItems) {
		if (failureItems.isEmpty()) {
			return "";
		}

		StringBuilder s = new StringBuilder();
		s.append("\n").append(spaces(4)).append("Failed updates:");
		for (String failureItem : failureItems) {
			s.append("\n").append(spaces(6)).append(failureItem);
		}

		return s.toString();
	}

	public String buildStartScanMessage(List<Path> rootFolders, String recursive) {
		StringBuilder s = new StringBuilder();
		for (Path rootFolder : rootFolders) {
			s.append(spaces(4)).append(rootFolder.toString()).append("\n");
		}
		String folders = s.toString();
		return SCAN_FOLDERS.formatted(folders, recursive);
	}

	public String buildMatchingResult(int tracksMatching, int albumsMatching, int tracksNotMatching,
			int albumsNotMatching) {
		return MATCHING_SUMMARY.formatted(tracksMatching, sOrBlank(tracksMatching), albumsMatching,
				sOrBlank(albumsMatching), tracksNotMatching, sOrBlank(tracksNotMatching), albumsNotMatching,
				sOrBlank(albumsNotMatching));
	}

	private String spaces(int count) {
		return " ".repeat(count);
	}

	private String sOrBlank(int value) {
		return value == 1 ? "" : "s";
	}

	public String buildScanSuccessMessage(int scanned, int skipped, int error) {
		return SCAN_SUCCESS_SUMMARY.formatted(scanned, sOrBlank(scanned), skipped, sOrBlank(skipped), error,
				sOrBlank(error));
	}

}
