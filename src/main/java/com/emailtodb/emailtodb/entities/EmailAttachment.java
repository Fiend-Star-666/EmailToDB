package com.emailtodb.emailtodb.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "email_attachment")
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_message_id", nullable = false)
    private EmailMessage emailMessage;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_extension", nullable = false)
    private String fileExtension;

    @Lob
    @Column(name = "file_content", nullable = false)
    private byte[] fileContent;

    @Column(name = "file_content_hash", nullable = false)
    private String fileContentHash;
}
