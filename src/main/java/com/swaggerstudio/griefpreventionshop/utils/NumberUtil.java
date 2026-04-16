package com.swaggerstudio.griefpreventionshop.utils;

import java.text.DecimalFormat;

public class NumberUtil {

    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static String formatCurrency(double amount) {
        if (amount >= 1_000_000_000) {
            return df.format(amount / 1_000_000_000.0) + " Billion";
        }
        if (amount >= 1_000_000) {
            return df.format(amount / 1_000_000.0) + " Million";
        }
        if (amount >= 1_000) {
            return df.format(amount / 1_000.0) + "K";
        }
        return df.format(amount);
    }
}
