package org.dendrocopos.chzzkbot.chzzk.chatentity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Audited
@Table(name = "command_message")
public class CommandMessageEntity {

    @Id
    private String cmdStr;

    private String cmdMsg;

    private boolean nickNameUse;

    private Long cooldown = 5000L;

    private LocalDateTime lastCommandTime = LocalDateTime.now();

    @PrePersist
    public void onPrePersist() {
        this.lastCommandTime = LocalDateTime.now();
        this.nickNameUse = false;
    }

}
