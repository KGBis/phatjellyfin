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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.jspecify.annotations.NonNull;

import java.io.Writer;

public class WindowsConsoleWriter extends Writer {

	private final Pointer hConsole;

	public WindowsConsoleWriter() {
		this.hConsole = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
		tryActivatingAnsiSupport();
	}

	@Override
	public void write(char @NonNull [] cbuf, int off, int len) {
		if (len == 0)
			return;

		// Si hay offset, extraemos el sub-array correspondiente
		char[] bufferToWrite = cbuf;
		if (off != 0 || len != cbuf.length) {
			bufferToWrite = new char[len];
			System.arraycopy(cbuf, off, bufferToWrite, 0, len);
		}

		IntByReference charsWritten = new IntByReference();
		boolean success = Kernel32.INSTANCE.WriteConsoleW(hConsole, bufferToWrite, len, charsWritten, null);

		// fallback to System.out
		if (!success) {
			System.out.print(new String(bufferToWrite)); // NOSONAR
		}
	}

	@Override
	public void flush() {
		// Windows Console doesn't need flush
	}

	@Override
	public void close() {
		// handler shouldn't be closed
	}

	private void tryActivatingAnsiSupport() {
		IntByReference mode = new IntByReference();

		// 1. Obtenemos el modo actual pasándole la referencia
		if (Kernel32.INSTANCE.GetConsoleMode(hConsole, mode)) {

			// 2. Extraemos el int del modo e inyectamos el flag ANSI
			int currentMode = mode.getValue();
			int newMode = currentMode | Kernel32.ENABLE_VIRTUAL_TERMINAL_PROCESSING;

			// 3. Establecemos el nuevo modo pasando el int directo
			Kernel32.INSTANCE.SetConsoleMode(hConsole, newMode);
		}
	}

	@SuppressWarnings("java:S100")
	interface Kernel32 extends StdCallLibrary {

		int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

		WindowsConsoleWriter.Kernel32 INSTANCE = Native.load("kernel32", WindowsConsoleWriter.Kernel32.class);

		int STD_OUTPUT_HANDLE = -11;

		Pointer GetStdHandle(int nStdHandle);

		boolean GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);

		void SetConsoleMode(Pointer hConsoleHandle, int dwMode);

		boolean WriteConsoleW(Pointer hConsoleOutput, char[] lpBuffer, int nNumberOfCharsToWrite,
				IntByReference lpNumberOfCharsWritten, Pointer lpReserved);

	}

}
