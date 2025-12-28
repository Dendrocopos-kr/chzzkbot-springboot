package org.dendrocopos.chzzkbot.chzzk.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

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

    @Builder.Default
    private Long cooldown = 5000L;

    @Builder.Default
    private LocalDateTime lastCommandTime = LocalDateTime.now();

    @Builder.Default
    private Long counting = 0L;

    @Builder.Default
    private Boolean enabled = true;

    @PrePersist
    public void onPrePersist() {
        this.lastCommandTime = LocalDateTime.now();
        this.nickNameUse = false;
        this.counting = 0L;
        this.enabled = true;
    }

}
