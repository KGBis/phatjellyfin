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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetrierTest {

    @Test
    void shouldRetryWhenIOExceptionIsThrown() throws Exception {
        AtomicInteger attempts = new AtomicInteger();

        String result = Retrier.retry(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IOException("Temporary error");
            }
            return "OK";
        }, "test operation");

        assertEquals("OK", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldNotRetryWhenIOExceptionIsNotThrown() throws Exception {
        String result = Retrier.retry(() -> "OK", "test operation");
        assertEquals("OK", result);
    }

}