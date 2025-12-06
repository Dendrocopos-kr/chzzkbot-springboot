package org.dendrocopos.chzzkbot.files.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.dendrocopos.chzzkbot.files.DTO.FileItem;
import org.dendrocopos.chzzkbot.files.enums.DownloadType;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Table(name = "download_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class DownloadLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fullPath;
    private long size;
    private String mimeType;
    private Instant lastModified;

    private String clientIp;
    private Instant downloadedAt;
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private DownloadType type;

    public static DownloadLog from(FileItem fileItem, String clientIp, boolean success, DownloadType type) {
        return DownloadLog.builder()
                .fileName(fileItem.getName())
                .fullPath(fileItem.getFullPath())
                .size(fileItem.getSize())
                .mimeType(fileItem.getMimeType())
                .lastModified(fileItem.getLastModified())
                .clientIp(clientIp)
                .downloadedAt(Instant.now())
                .success(success)
                .type(type)
                .build();
    }
}
