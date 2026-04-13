package org.example.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpiringKeyTest {


    @Test
    void returnsPositiveForFutureTimestamp() {

        Long timestamp = System.currentTimeMillis() + 1000;

        ExpiringKey key = new ExpiringKey("test", timestamp);

        assertTrue(key.getDelay(TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void returnsNegativeForPastTimestamp() {
        Long timestamp = System.currentTimeMillis() - 1000;

        ExpiringKey key = new ExpiringKey("test", timestamp);

        assertTrue(key.getDelay(TimeUnit.MILLISECONDS) < 0);
    }

}
