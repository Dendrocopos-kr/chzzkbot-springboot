package org.dendrocopos.chzzkbot.files.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem {
    private String name;
    private String fullPath;
    private String parentPath;
    private boolean directory;
    private long size;
    private Instant lastModified;
    private String mimeType;
    // 보기 좋은 문자열 예: "12.3 MB"
    private String humanSize;
    private boolean downloadable;
}