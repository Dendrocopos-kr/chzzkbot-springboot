package org.dendrocopos.chzzkbot.chzzk.entity;

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
@Table(name = "donation_message")
public class DonationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "donation_seq")
    @SequenceGenerator(name = "donation_seq", sequenceName = "donation_sequence")
    private BigDecimal seq;

    private String uid;

    private String nickName;

    @Column(columnDefinition = "TEXT")
    private String msg;

    private String donationType;

    private String selectionType;

    private String giftCount;

    private String cost;

    private LocalDateTime inputTime;

    @PrePersist
    public void onPrePersist() {
        this.inputTime = LocalDateTime.now();
    }
}
