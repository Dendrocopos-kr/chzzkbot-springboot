package org.dendrocopos.chzzkbot.chzzk.chatservice;

import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.chatentity.DonationMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatentity.NormalMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatservice.impl.IMessageService;
import org.dendrocopos.chzzkbot.chzzk.repository.DonationMessageRepository;
import org.dendrocopos.chzzkbot.chzzk.repository.NormalMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.dendrocopos.chzzkbot.chzzk.utils.EntityUtils.*;

@Service
@RequiredArgsConstructor
public class MessageService implements IMessageService {
    private final DonationMessageRepository donationMessageRepository;
    private final NormalMessageRepository normalMessageRepository;

    public DonationMessageEntity saveDonationMessage(Map<String, Object> messageContent) {
        DonationMessageEntity donationMessage = DonationMessageEntity.builder()
                .uid(getUid(messageContent))
                .nickName(getNickname(messageContent))
                .msg(getMsg(messageContent))
                .donationType(getDonationType(messageContent))
                .cost(getCost(messageContent))
                .giftCount(getGiftCount(messageContent))
                .selectionType(getSelectType(messageContent))
                .build();
        return donationMessageRepository.save(donationMessage);
    }

    public NormalMessageEntity saveNormalMessage(Map<String, Object> messageContent) {
        NormalMessageEntity normalMessage = NormalMessageEntity.builder()
                .uid(getUid(messageContent))
                .nickName(getNickname(messageContent))
                .msg(getMsg(messageContent))
                .build();
        return normalMessageRepository.save(normalMessage);
    }
}
