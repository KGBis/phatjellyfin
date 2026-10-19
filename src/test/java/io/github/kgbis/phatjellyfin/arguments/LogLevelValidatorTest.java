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
package io.github.kgbis.phatjellyfin.arguments;

import com.beust.jcommander.ParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLevelValidatorTest {

	private final LogLevelValidator validator = new LogLevelValidator();

	@Test
	void shouldAcceptValidLogLevels() {
		assertDoesNotThrow(() -> validator.validate("--logLevel", "OFF"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "TRACE"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "DEBUG"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "INFO"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "WARN"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "ERROR"));
	}

	@Test
	void shouldAcceptLowercaseLogLevels() {
		assertDoesNotThrow(() -> validator.validate("--logLevel", "off"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "trace"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "debug"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "info"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "warn"));
		assertDoesNotThrow(() -> validator.validate("--logLevel", "error"));
	}

	@Test
	void shouldRejectInvalidLogLevel() {
		ParameterException ex = assertThrows(ParameterException.class,
				() -> validator.validate("--logLevel", "VERBOSE"));

		assertTrue(ex.getMessage().contains("Invalid value"));
		assertTrue(ex.getMessage().contains("OFF"));
		assertTrue(ex.getMessage().contains("ERROR"));
	}

	@Test
	void shouldRejectEmptyValue() {
		assertThrows(ParameterException.class, () -> validator.validate("--logLevel", ""));
	}

}
