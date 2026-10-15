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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * A simple retrier class with a linear delay after each unsuccessful attempt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class Retrier {

	private static final int MAX_ATTEMPTS = 3;

	private static final long RETRY_DELAY_MS = 500;

	public static <T> T retry(IOSupplier<T> supplier, String opMsg) throws IOException, InterruptedException {
		int attempt = 0;
		while (true) {
			attempt++;

			try {
				return supplier.get();
			}
			catch (IOException e) {
				if (attempt >= MAX_ATTEMPTS) {
					throw e;
				}

				long millis = RETRY_DELAY_MS * attempt;
				log.warn("Error {} (attempt {}/{}). Retrying in {}ms...", opMsg, attempt, MAX_ATTEMPTS, millis);
				// noinspection BusyWait
				Thread.sleep(millis);
			}
		}
	}

}
