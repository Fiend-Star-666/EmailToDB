package com.emailtodb.emailtodb.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    @Column(nullable = true)
    private short SourceId = 14; // constant

    private short GuidanceStatusID = 1; // constant

    @Column(nullable = true)
    private short ProgressId;

    private short ResponseTypeId = 2; // constant

    private Date DueDateOverride; // 8 business days from DateAdded

    @Column(length = Integer.MAX_VALUE)
    private String Comments;

    @Column(nullable = true)
    private Date CreatedDate;

    @Column(nullable = true)
    private int CreatedBy = 1;

    private Date UpdatedDate;

    @Column(nullable = true)
    private int UpdatedBy;

    @Column(length = 10)
    private String LinkedGuidanceNo;

    @Column(length = 5)
    private String ExpectedGuidance;

    private Date CompletedDate;

    @Column(nullable = true)
    private int CompletedBy;

    @Column(nullable = true)
    private int CompanyId = 1; // constant

    @Column(length = 5)
    private String AdditionalGroup;

    @OneToMany(mappedBy = "guidance", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GuidanceDocumentHistory> guidanceDocumentHistory;

    public void setDateAdded(Date dateAdded) {
        if (dateAdded == null) {
            throw new IllegalArgumentException("dateAdded cannot be null");
        }
        this.DateAdded = dateAdded;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setTime(dateAdded);
        int businessDaysToAdd = 8;
        while (businessDaysToAdd > 0) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                // Add a check for holidays here
                businessDaysToAdd--;
            }
        }
        this.DueDateOverride = cal.getTime();
    }
}
