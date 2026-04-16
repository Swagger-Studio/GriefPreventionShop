package com.swaggerstudio.griefpreventionshop.utils;

import java.text.DecimalFormat;

public class NumberUtil {

    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static String formatCurrency(double amount, String symbol) {
        String formatted;
        if (amount >= 1_000_000_000) {
            formatted = df.format(amount / 1_000_000_000.0) + " Billion";
        } else if (amount >= 1_000_000) {
            formatted = df.format(amount / 1_000_000.0) + " Million";
        } else if (amount >= 1_000) {
            formatted = df.format(amount / 1_000.0) + "K";
        } else {
            formatted = df.format(amount);
        }
        return symbol + formatted;
    }
}
