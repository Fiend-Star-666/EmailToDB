package com.emailtodb.emailtodb.repositories;

import com.emailtodb.emailtodb.entities.GuidanceDocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidanceDocumentHistoryRepository extends JpaRepository<GuidanceDocumentHistory, Long> {
}
