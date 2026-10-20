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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class LogLevelValidator implements IParameterValidator {

	private static final Set<String> VALID_LEVELS = Set.of("OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR");

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!VALID_LEVELS.contains(StringUtils.upperCase(value))) {
			throw new ParameterException("Invalid value '" + value + "'. Level must be one of: " + VALID_LEVELS);
		}
	}

}
