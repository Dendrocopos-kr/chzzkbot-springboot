package org.dendrocopos.guribot.core.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApiHeader {
    private int resultCode;
    private String codeName;
}
