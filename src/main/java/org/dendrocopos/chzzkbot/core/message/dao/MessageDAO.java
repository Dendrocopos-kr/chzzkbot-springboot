package org.dendrocopos.chzzkbot.core.message.dao;

import org.springframework.stereotype.Repository;

//@Repository
public interface MessageDAO {
    String selectCommandMessage01(String message);
}
