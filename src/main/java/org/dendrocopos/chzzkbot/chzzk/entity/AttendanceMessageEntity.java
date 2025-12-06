package org.dendrocopos.chzzkbot.chzzk.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.envers.Audited;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Audited
@Table(name = "attendance_message")
public class AttendanceMessageEntity {

    @Id
    private String cmdStr;

    private String uid;
    
    private boolean nickNameUse;

    private boolean attendanceAt;

    private String followDate;
}
