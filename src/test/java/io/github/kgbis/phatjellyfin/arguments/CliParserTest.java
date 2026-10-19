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

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import io.github.kgbis.phatjellyfin.output.Console;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliParserTest {

	private CliParser cliParser;

	@BeforeEach
	void setUp() {
		Console console = new Console();
		cliParser = new CliParser(console);
	}

	@Test
	@ExpectSystemExitWithStatus(-1)
	void when_noPathArgumentIsPassed_then_programShowsUsageAndExits() { // NOSONAR
		cliParser.parseCommandLine(new String[] {}, this::onHelp);
	}

	@ParameterizedTest
	@ValueSource(strings = { "--help", "-h", "--usage", "-u" })
	@ExpectSystemExitWithStatus(0)
	void when_helpArgumentIsPassed_then_programShowsUsageAndExits(String arg) { // NOSONAR
		cliParser.parseCommandLine(new String[] { arg }, this::onHelp);
	}

	@Test
	@ExpectSystemExitWithStatus(-1)
	void when_invalidLogLevel_then_programShowsUsageAndExits() { // NOSONAR
		cliParser.parseCommandLine(new String[] { "--logLevel", "FOO", "/a/good/path/argument" }, this::onHelp);
	}

	@Test
	@ExpectSystemExitWithStatus(-1)
	void when_invalidPath_then_programShowsUsageAndExits() { // NOSONAR
		String wrongPath = "'c:/music library/'";
		cliParser.parseCommandLine(new String[] { "/a/good/path/argument", wrongPath }, this::onHelp);
	}

	@Test
	void when_logLevelIsSet_then_parseLogLevel() {
		String[] cmdLine = { "--logLevel", "WARN", "/a/good/path/argument" };
		CliArguments args = cliParser.parseCommandLine(cmdLine, this::onHelp);

		assertEquals(Level.WARN, args.getLogLevel());
	}

	@ParameterizedTest
	@ValueSource(strings = { "-r", "--recursive" })
	void when_recursiveIsSet_then_parseRecursive(String arg) {
		String[] cmdLine = { arg, "/a/good/path/argument" };
		CliArguments args = cliParser.parseCommandLine(cmdLine, this::onHelp);

		assertTrue(args.isRecursive());
	}

	@ParameterizedTest
	@ValueSource(strings = { "-a", "--apply" })
	void when_applyIsSet_then_parseApply(String arg) {
		String[] cmdLine = { arg, "/a/good/path/argument" };
		CliArguments args = cliParser.parseCommandLine(cmdLine, this::onHelp);

		assertTrue(args.isApply());
	}

	private void onHelp(JCommander jc) {
		jc.usage();
		System.exit(0);
	}

}
