package org.dendrocopos.chzzkbot.chzzk.chatentity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Audited
@Table(name = "normalMessage", indexes = {@Index(name = "index_normalMessage", columnList = "nickName")})
public class NormalMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_normal_chat")
    @SequenceGenerator(name = "seq_normal_chat", sequenceName = "normarl_sequence", allocationSize = 1)
    private BigDecimal seq;

    private String uid;

    private String nickName;

    @Column(columnDefinition = "TEXT")
    private String msg;

    private LocalDateTime inputTime;

    @PrePersist
    public void onPrePersist() {
        this.inputTime = LocalDateTime.now();
    }
}
