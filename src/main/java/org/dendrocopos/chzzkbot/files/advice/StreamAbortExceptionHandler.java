package org.dendrocopos.chzzkbot.files.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

@ControllerAdvice
@Slf4j
public class StreamAbortExceptionHandler{

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) throws AsyncRequestNotUsableException {
        Throwable root = e.getCause();
        if (root instanceof IOException) {
            String msg = root.getMessage();
            if (msg != null && isClientAbortMessage(msg)) {
                log.debug("Client Aborted: {}", msg);
                // 정상적인 클라이언트 이탈: 조용히 무시(또는 DEBUG 로그만)
                return;
            }
        }
        // 그 외는 그대로 던져서 기존 처리/로그 유지
        throw e;
    }

    private boolean isClientAbortMessage(String msg) {
        String m = msg.toLowerCase();
        return m.contains("connection reset")
                || m.contains("broken pipe")
                || m.contains("forcibly closed")
                || msg.contains("사용자의 호스트 시스템");
    }
}
