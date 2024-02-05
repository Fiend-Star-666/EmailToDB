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

    private Date CreatedDate;

    private int CreatedBy;

    private Date UpdatedDate;

    @Column(nullable = true)
    private int UpdatedBy;
}
