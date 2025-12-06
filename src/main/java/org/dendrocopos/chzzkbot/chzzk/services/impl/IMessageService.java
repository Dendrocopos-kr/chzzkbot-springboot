package org.dendrocopos.chzzkbot.chzzk.services.impl;

import org.dendrocopos.chzzkbot.chzzk.entity.DonationMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.entity.NormalMessageEntity;

import java.util.Map;

public interface IMessageService {
    DonationMessageEntity saveDonationMessage(Map<String, Object> messageContent);
    NormalMessageEntity saveNormalMessage(Map<String, Object> messageContent);
}
