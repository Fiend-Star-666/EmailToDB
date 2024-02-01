package com.emailtodb.emailtodb.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "email_attachments")
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "email_message_id", nullable = false)
    private EmailMessage emailMessage;

    @Column(name = "file_name", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileName;

    @Column(name = "file_extension", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileExtension;

    // @Lob
    // @Column(name = "file_content", nullable = false)
    @Transient  // This annotation makes fileContent non-persistent
    private byte[] fileContent;

    @Column(name = "file_url", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileUrl;

    @Column(name = "file_location", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileLocation;

    @Column(name = "file_content_hash", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String fileContentHash;
}
