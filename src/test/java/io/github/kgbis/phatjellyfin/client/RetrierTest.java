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