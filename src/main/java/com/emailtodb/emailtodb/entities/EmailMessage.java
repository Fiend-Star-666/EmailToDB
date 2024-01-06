package com.emailtodb.emailtodb.entities;


import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "email_messages")
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "email_from", nullable = false)
    private String from;

    @Column(name = "email_to", nullable = false)
    private String to;

    @Column(name = "cc")
    private String cc;

    @Column(name = "bcc")
    private String bcc;

    @Column(name = "date_sent", nullable = false)
    private Date dateSent;

    @Column(name = "seen")
    private Boolean seen;

    @Column(name = "answered")
    private Boolean answered;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "draft")
    private Boolean draft;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @OneToMany(mappedBy = "emailMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailAttachment> emailAttachments;
}
