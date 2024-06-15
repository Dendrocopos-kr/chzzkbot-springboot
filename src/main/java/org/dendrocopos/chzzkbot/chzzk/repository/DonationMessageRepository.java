package org.dendrocopos.chzzkbot.chzzk.repository;

import org.dendrocopos.chzzkbot.chzzk.chatentity.DonationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationMessageRepository extends JpaRepository<DonationMessageEntity, String> {

}
