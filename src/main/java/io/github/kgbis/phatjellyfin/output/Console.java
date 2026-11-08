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

import com.sun.jna.Platform;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import static io.github.kgbis.phatjellyfin.output.MessageManager.INCORRECT_ANSWER;
import static io.github.kgbis.phatjellyfin.output.MessageManager.PROCESS_FINISHED;

@Singleton
public class Console {

	public static final String ANSI_DEL = "\033[2K";

	public static final String RED = "\u001B[31m";

	public static final String RESET = "\u001B[0m";

	private static final int BAR_LENGTH = 40;

	private final BufferedReader in;

	private final PrintWriter out;

	@Inject
	public Console() {
		PrintWriter writer = Platform.isWindows() ? new PrintWriter(new WindowsConsoleWriter(), true)
				: new PrintWriter(System.out, true); // NOSONAR
		this(System.in, writer);

	}

	Console(InputStream in, PrintWriter writer) {
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = writer;
	}

	public void clearCurrentLine() {
		out.print("\r" + ANSI_DEL);
	}

	public void print(String message) {
		out.print(message);
		out.flush();
	}

	public void printSameLine(String message) {
		clearCurrentLine();
		print(message);
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
		out.println(PROCESS_FINISHED.formatted(stopWatch.formatTime()));
		out.flush();
	}

	public void printErrorAndStopWatch(@Nonnull String message, @Nonnull StopWatch stopWatch) {
		out.println(RED + "ERROR: " + message + RESET);
		out.println();
		println(stopWatch);
	}

	/**
	 * Wait for user input. Yes as default if ENTER
	 * @param message Question message
	 * @return If user answered yes or no
	 */
	public boolean confirm(String message) {
		while (true) {
			print(message);

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

			println(INCORRECT_ANSWER);
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

	private String readLine() {
		try {
			return in.readLine();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
