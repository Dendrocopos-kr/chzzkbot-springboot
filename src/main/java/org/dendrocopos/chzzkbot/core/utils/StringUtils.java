package org.dendrocopos.chzzkbot.core.utils;

public class StringUtils {

    public static void appendTimeUnit(StringBuilder sb, long timeUnit, String unitName) {
        if (timeUnit > 0) {
            sb.append(timeUnit).append(unitName);
        }
    }
}
