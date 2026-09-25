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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.github.kgbis.phatjellyfin.output.Console;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import static io.github.kgbis.phatjellyfin.Application.APP_NAME;

@Singleton
@Slf4j
public class CliParser {

    private final Console console;

    @Inject
    public CliParser(Console console) {
        this.console = console;
    }

    public CliArguments parseCommandLine(String[] args, Consumer<JCommander> onHelp) {
        CliArguments cliArguments = new CliArguments();
        JCommander jCommander = JCommander.newBuilder()
                .programName(APP_NAME)
                .allowAbbreviatedOptions(false)
                .addObject(cliArguments)
                .build();

        CliUsageFormatter usageFormatter = new CliUsageFormatter(jCommander, console);

        jCommander.setCaseSensitiveOptions(false);
        jCommander.setUsageFormatter(usageFormatter);

        try {
            jCommander.parse(args);
        } catch (ParameterException parameterException) {
            usageFormatter.error(parameterException);
            System.exit(-1);
        }

        if (cliArguments.isHelp() || args.length == 0) {
            onHelp.accept(jCommander);
        }

        return cliArguments;
    }

}

