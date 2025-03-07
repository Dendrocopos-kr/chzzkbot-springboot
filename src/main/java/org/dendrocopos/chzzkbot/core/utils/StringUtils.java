package org.dendrocopos.chzzkbot.core.utils;

public class StringUtils {

    public static void appendTimeUnit(StringBuilder sb, long timeUnit, String unitName) {
        if (timeUnit > 0) {
            sb.append(timeUnit).append(unitName);
        }
    }
    public static String getSubstringAfterFirstSpace(String input) {
        int firstSpaceIndex = input.indexOf(" ");
        if (firstSpaceIndex == -1) {
            return ""; // 공백이 없으면 빈 문자열 반환
        }
        return input.substring(firstSpaceIndex + 1).trim();
    }
}
