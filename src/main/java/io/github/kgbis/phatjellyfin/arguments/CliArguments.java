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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class CliArguments {

    @Parameter(names = {"-h", "--help", "-u", "--usage"}, help = true, description = "Show this usage help")
    private boolean help;

    @Parameter(names = {"-a", "--apply"}, description = "Apply changes to Jellyfin")
    private boolean apply = false;

    @Parameter(names = {"-r", "--recursive"}, description = "Scan folder recursively")
    private boolean recursive = false;

    @Parameter(names = { "-c", "--console" }, description = "Log to console too. Log to file will still be used.")
    private boolean logToConsole = false;

    @Parameter(required = true, arity = 1, description = "<folder>", converter = PathConverter.class, validateWith = PathValidator.class)
    private Path folder;
}
