package com.emailtodb.emailtodb.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "GuidanceDocumentHistory")
public class GuidanceDocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int DocumentId;

    @ManyToOne
    @JoinColumn(name = "GuidanceId", nullable = false)
    private Guidance guidance;

    //@Column(length = 200)
    private String DocumentURL;
    private Date CreatedDate;
    private int CreatedBy;
    private Date UpdatedDate;
    private int UpdatedBy;
}
