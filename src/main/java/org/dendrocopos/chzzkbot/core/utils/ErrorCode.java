package org.dendrocopos.chzzkbot.core.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    FAIL(999,"실패"),
    NOT_FOUND_USER(600,"NOT_FOUND_USER"),
    NOT_FOUND_TOKEN(601,"NOT_FOUND_TOKEN")
    ;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;
    private String message;
}
