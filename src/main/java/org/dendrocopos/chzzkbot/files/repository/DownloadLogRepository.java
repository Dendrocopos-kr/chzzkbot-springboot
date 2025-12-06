package org.dendrocopos.chzzkbot.files.repository;

import org.dendrocopos.chzzkbot.files.Entity.DownloadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DownloadLogRepository extends JpaRepository<DownloadLog, Long> {
    List<DownloadLog> findByClientIp(String clientIp);
}
