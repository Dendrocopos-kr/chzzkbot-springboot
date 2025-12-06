package org.dendrocopos.chzzkbot.chzzk.repository;

import org.dendrocopos.chzzkbot.chzzk.entity.NormalMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NormalMessageRepository extends JpaRepository<NormalMessageEntity, String> {

    Long countByMsg(String msg);
}
