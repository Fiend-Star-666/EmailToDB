package com.emailtodb.emailtodb.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Entity
@Table(name = "Guidance")
public class Guidance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int GuidanceId;

    @Column(length = Integer.MAX_VALUE)
    private String Name; // maps to EmailSubject
    private Date DateAdded;

    @Column(length = Integer.MAX_VALUE)
    private String Description; //maps to EmailBriefBody if not empty otherwise it maps to EmailBody
    private short SourceId = 14; // constant
    private short GuidanceStatusID = 1; // constant
    private short ProgressId;
    private short ResponseTypeId = 2; // constant
    private Date DueDateOverride; // 8 business days from DateAdded

    @Column(length = Integer.MAX_VALUE)
    private String Comments;
    private Date CreatedDate;
    private int CreatedBy;
    private Date UpdatedDate;
    private int UpdatedBy;

    @Column(length = 10)
    private String LinkedGuidanceNo;

    @Column(length = 5)
    private String ExpectedGuidance;
    private Date CompletedDate;
    private int CompletedBy;
    private int CompanyId = 10; // constant

    @Column(length = 5)
    private String AdditionalGroup;

    @OneToMany(mappedBy = "guidance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GuidanceDocumentHistory> guidanceDocumentHistory;

    public void setDateAdded(Date dateAdded) {
        this.DateAdded = dateAdded;
        // calculate DueDateOverride as 8 business days from DateAdded
        this.DueDateOverride = new Date(dateAdded.getTime() + TimeUnit.DAYS.toMillis(8));
    }
}
