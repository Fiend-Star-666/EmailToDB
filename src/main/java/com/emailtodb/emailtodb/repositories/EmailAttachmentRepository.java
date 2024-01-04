package com.emailtodb.emailtodb.repositories;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {
    Optional<EmailAttachment> findByFileContentHash(String fileContentHash);
}

