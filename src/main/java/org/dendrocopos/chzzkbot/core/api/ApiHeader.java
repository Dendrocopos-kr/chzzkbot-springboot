package org.dendrocopos.chzzkbot.core.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApiHeader {
    private int resultCode;
    private String codeName;
}
