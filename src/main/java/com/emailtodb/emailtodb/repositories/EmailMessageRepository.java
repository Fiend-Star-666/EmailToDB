package com.emailtodb.emailtodb.repositories;

import com.emailtodb.emailtodb.entities.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
    Optional<EmailMessage> findTopByOrderByDateSentDesc();

    Optional<EmailMessage> findByMessageId(String id);

    Optional<List<EmailMessage>> findByDraft(boolean b);
}
