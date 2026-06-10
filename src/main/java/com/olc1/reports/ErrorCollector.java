package com.olc1.reports;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {
    private static final List<GoLiteError> errors = new ArrayList<>();

    public static void addError(String type, String desc, int line, int col) {
        errors.add(new GoLiteError(type, desc, line, col));
    }

    public static List<GoLiteError> getErrors() {
        return errors;
    }

    public static void clear() {
        errors.clear();
    }
}
