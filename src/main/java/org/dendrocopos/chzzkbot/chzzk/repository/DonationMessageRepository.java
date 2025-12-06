package org.dendrocopos.chzzkbot.chzzk.repository;

import org.dendrocopos.chzzkbot.chzzk.entity.DonationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationMessageRepository extends JpaRepository<DonationMessageEntity, String> {

    Long countByMsg(String msg);
}
