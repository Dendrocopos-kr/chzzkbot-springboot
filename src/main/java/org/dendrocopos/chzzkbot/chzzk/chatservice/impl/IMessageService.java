package org.dendrocopos.chzzkbot.chzzk.chatservice.impl;

import org.dendrocopos.chzzkbot.chzzk.chatentity.DonationMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatentity.NormalMessageEntity;

import java.util.Map;

public interface IMessageService {
    DonationMessageEntity saveDonationMessage(Map<String, Object> messageContent);
    NormalMessageEntity saveNormalMessage(Map<String, Object> messageContent);
}
