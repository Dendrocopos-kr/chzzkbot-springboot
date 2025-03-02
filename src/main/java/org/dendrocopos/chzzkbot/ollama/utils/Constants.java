package org.dendrocopos.chzzkbot.ollama.utils;

import org.springframework.beans.factory.annotation.Value;

public class Constants {
    @Value("${ollama.callCommand}")
    public static String COMMAND_AI;
}
