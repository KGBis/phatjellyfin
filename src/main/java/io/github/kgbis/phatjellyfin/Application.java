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
package io.github.kgbis.phatjellyfin;

import io.github.kgbis.phatjellyfin.log.LogbackConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {

	public static final String APP_NAME = "phatjellyfin";

	// log files
	public static final String LOG_DIR = "logs";

	public static final String LOG_FILE_NAME = APP_NAME + ".log";

	public static final String GZ_FILE_NAME = LOG_FILE_NAME + "-%d{yyyy-MM-dd}.gz";

	// config file
	public static final String APP_CONFIG_FILE = APP_NAME + ".cfg.json";

	public static final String UNKNOWN_VERSION = "unknown";

	private static String version;

	public static String getVersion() {
		if (version == null) {
			try (InputStream in = LogbackConfiguration.class.getClassLoader().getResourceAsStream("version.txt")) {
				if (in == null) {
					version = UNKNOWN_VERSION;
				}
				else {
					version = new String(in.readAllBytes()).trim();
				}
			}
			catch (IOException _) {
				version = UNKNOWN_VERSION;
			}
		}

		return version;
	}

}
