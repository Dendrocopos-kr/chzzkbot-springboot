package org.dendrocopos.chzzkbot.core.message.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class MessageVO {
    String cmdStr;
    String cmdMsg;
}
