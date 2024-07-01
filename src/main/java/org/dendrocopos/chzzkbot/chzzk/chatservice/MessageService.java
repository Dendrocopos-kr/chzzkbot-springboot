package org.dendrocopos.chzzkbot.chzzk.chatservice;

import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.chatentity.DonationMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.chatentity.NormalMessageEntity;
import org.dendrocopos.chzzkbot.chzzk.repository.DonationMessageRepository;
import org.dendrocopos.chzzkbot.chzzk.repository.NormalMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.dendrocopos.chzzkbot.chzzk.utils.EntityUtils.*;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final DonationMessageRepository donationMessageRepository;
    private final NormalMessageRepository normalMessageRepository;

    public void saveDonationMessage(Map<String, Object> messageContent) {
        DonationMessageEntity donationMessage = DonationMessageEntity.builder()
                .uid(getUid(messageContent))
                .nickName(getNickname(messageContent))
                .msg(getMsg(messageContent))
                .donationType(getDonationType(messageContent))
                .cost(getCost(messageContent))
                .build();
        donationMessageRepository.save(donationMessage);
    }

    public void saveNormalMessage(Map<String, Object> messageContent) {
        NormalMessageEntity normalMessage = NormalMessageEntity.builder()
                .uid(getUid(messageContent))
                .nickName(getNickname(messageContent))
                .msg(getMsg(messageContent))
                .build();
        normalMessageRepository.save(normalMessage);
    }

    // include methods here to build NormalMessageEntity and DonationMessageEntity
}
