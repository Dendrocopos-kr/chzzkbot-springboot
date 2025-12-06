package org.dendrocopos.chzzkbot.chzzk.repository;

import org.dendrocopos.chzzkbot.chzzk.entity.CommandMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandMessageRepository extends JpaRepository<CommandMessageEntity, String> {

}
