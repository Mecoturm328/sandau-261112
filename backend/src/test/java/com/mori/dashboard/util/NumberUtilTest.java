package com.mori.dashboard.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NumberUtilTest {

    @Test
    void roundsHalfUpDespiteBinaryError() {
        assertEquals(2.68, NumberUtil.round(2.675, 2));
        assertEquals(33.3, NumberUtil.round(33.333, 1));
    }

    @Test
    void divisionAndPercentGuardAgainstZero() {
        assertEquals(0, NumberUtil.divide(10, 0, 2));
        assertEquals(0, NumberUtil.percent(10, 0, 1));
        assertEquals(37.5, NumberUtil.percent(3, 8, 1));
        assertNull(NumberUtil.percentChange(10, 0));
        assertEquals(-50.0, NumberUtil.percentChange(5, 10));
    }
}
