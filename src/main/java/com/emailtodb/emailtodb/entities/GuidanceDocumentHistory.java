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

    private String DocumentURL;

    @Column(nullable = true)
    private Date CreatedDate;

    private int CreatedBy = 1;

    private Date UpdatedDate;

    @Column(nullable = true)
    private int UpdatedBy;
}
