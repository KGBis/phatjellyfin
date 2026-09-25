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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

@Singleton
public class Console {

	// ANSI Escape Codes for Colors
	public static final String RESET = "\u001B[0m";

	public static final String RED = "\u001B[31m";

	public static final String ANSI_DEL = "\033[2K";

	private static final int BAR_LENGTH = 40;

	private final PrintWriter out;

	private final BufferedReader in;

	// @SuppressWarnings("unused") // Guice
	@Inject
	public Console() {
		this(System.in, System.out); // NOSONAR
	}

	Console(InputStream in, PrintStream out) {
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = new PrintWriter(out, true);
	}

	public void print(String message) {
		out.print(message);
		out.flush();
	}

	public void printSameLine(String message) {
		out.print("\r" + ANSI_DEL + message);
		out.flush();
	}

	public void println(String message) {
		out.println(message);
	}

	public void printAndStopWatch(String message, StopWatch stopWatch) {
		out.println(message);
		println(stopWatch);
	}

	public void println(StopWatch stopWatch) {
		stopWatch.stop();
		out.println("Process finished in " + stopWatch.formatTime());
		out.flush();
	}

	public void printErrorAndStopWatch(@Nonnull String message, @Nonnull StopWatch stopWatch) {
		out.println(RED + "ERROR: " + message + RESET);
		out.println();
		println(stopWatch);
	}

	private String readLine() {
		try {
			return in.readLine();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Wait for user input. Yes as default if ENTER
	 * @param message Question message
	 * @return If user answered yes or no
	 */
	public boolean confirm(String message) {
		while (true) {
			print(message + " [Y/n]: ");

			String answer = readLine();

			if (answer == null) {
				return false;
			}

			if (answer.isEmpty() || "y".equalsIgnoreCase(answer)) {
				return true;
			}

			if ("n".equalsIgnoreCase(answer)) {
				return false;
			}

			println("Please answer 'y' or 'n'.");
		}
	}

	public void progress(int current, int total) {
		int percentage = (current * 100) / total;
		int bars = (current * BAR_LENGTH) / total;

		StringBuilder bar = new StringBuilder("[");
		bar.repeat("#", bars);
		bar.repeat(".", BAR_LENGTH - bars);
		bar.append("]");

		printSameLine("%s %3d%%".formatted(bar, percentage));
	}

}
