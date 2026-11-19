package com.mori.dashboard.util;

public final class NumberUtil {

    private NumberUtil() {
    }

    /** 四舍五入到指定小数位（加微小偏移以抵消二进制浮点误差，如 2.675 → 2.68）。 */
    public static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round((value + 1e-9) * factor) / factor;
    }

    /** 安全除法：除数为 0 时返回 0，结果保留指定小数位。 */
    public static double divide(double dividend, double divisor, int digits) {
        return divisor == 0 ? 0 : round(dividend / divisor, digits);
    }

    /** 占比百分比（part / total × 100）；total 为 0 时返回 0。 */
    public static double percent(double part, double total, int digits) {
        return total == 0 ? 0 : round(part / total * 100, digits);
    }

    /** 环比百分比，保留 1 位小数；上一期为 0 时无法比较，返回 null。 */
    public static Double percentChange(double current, double previous) {
        if (previous == 0) {
            return null;
        }
        return round((current - previous) / previous * 100, 1);
    }
}
