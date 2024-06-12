package org.dendrocopos.chzzkbot.chzzk.repository;

import org.dendrocopos.chzzkbot.chzzk.chatentity.CommandMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandMessageRepository extends JpaRepository<CommandMessageEntity, String> {

}
