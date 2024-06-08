package org.dendrocopos.chzzkbot.core.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApiBody<T> {
    private T data;
    private T msg;
}
