package org.dendrocopos.chzzkbot.chzzk.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.net.URI;

@Service
@Slf4j
public class UrlOpenService {

    public void open(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop API not supported (GUI session may be unavailable).");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            // 운영에서는 logger 사용 권장
            e.printStackTrace();
            log.error("Failed to open url: {}", url);
        }
    }
}
