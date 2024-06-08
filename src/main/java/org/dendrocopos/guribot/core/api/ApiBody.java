package org.dendrocopos.guribot.core.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApiBody<T> {
    private T data;
    private T msg;
}
