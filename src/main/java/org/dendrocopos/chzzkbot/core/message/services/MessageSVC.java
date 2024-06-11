package org.dendrocopos.chzzkbot.core.message.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.core.message.dao.MessageDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MessageSVC {
    private final MessageDAO messageDAO;

    public MessageSVC(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public String selectCommandMessage01(String message) {
        return messageDAO.selectCommandMessage01(message);
    }
}
