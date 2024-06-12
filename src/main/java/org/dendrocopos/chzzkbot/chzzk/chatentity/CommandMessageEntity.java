package org.dendrocopos.chzzkbot.chzzk.chatentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "commandMessage")
public class CommandMessageEntity {

    @Id
    private String cmdStr;

    private String cmdMsg;

    private boolean nickNameUse;
}
