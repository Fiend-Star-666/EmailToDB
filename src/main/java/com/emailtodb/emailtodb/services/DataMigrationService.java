package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.entities.Guidance;
import com.emailtodb.emailtodb.entities.GuidanceDocumentHistory;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.repositories.GuidanceDocumentHistoryRepository;
import com.emailtodb.emailtodb.repositories.GuidanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DataMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

    private final EmailMessageRepository emailMessageRepository;
    private final GuidanceRepository guidanceRepository;
    private final GuidanceDocumentHistoryRepository guidanceDocumentHistoryRepository;
    private final AzureFileStorageService azureFileStorageService;

    public DataMigrationService(EmailMessageRepository emailMessageRepository,
                                GuidanceRepository guidanceRepository,
                                GuidanceDocumentHistoryRepository guidanceDocumentHistoryRepository,
                                AzureFileStorageService azureFileStorageService) {
        this.emailMessageRepository = emailMessageRepository;
        this.guidanceRepository = guidanceRepository;
        this.guidanceDocumentHistoryRepository = guidanceDocumentHistoryRepository;
        this.azureFileStorageService = azureFileStorageService;
    }

    public void migrateGuidanceData() {
        logger.info("Starting data migration");
        Optional<List<EmailMessage>> emailMessages = emailMessageRepository.findByStatusMigrateAndStatusUploadStaging(false, true);

        if (emailMessages.isPresent()) {
            for (EmailMessage emailMessage : emailMessages.get()) {
                Guidance guidance = convertEmailToGuidance(emailMessage);
                emailMessage.setStatusMigrate(true);

                insertIntoGuidanceAndUpdateEmailStatus(guidance, emailMessage);

                // Process the attachments
                for (EmailAttachment emailAttachment : emailMessage.getEmailAttachments()) {
                    GuidanceDocumentHistory guidanceDocumentHistory = convertEmailAttachmentToGuidanceDocumentHistory(emailAttachment, guidance);
                    insertIntoGuidanceDocumentHistory(guidanceDocumentHistory);
                }
            }
            logger.info("Data migration completed");
        } else {
            logger.info("No email messages to migrate");
        }
    }

    private void insertIntoGuidanceAndUpdateEmailStatus(Guidance guidance, EmailMessage emailMessage) {
        emailMessageRepository.save(emailMessage);
        guidanceRepository.save(guidance);
    }

    private void insertIntoGuidanceDocumentHistory(GuidanceDocumentHistory guidanceDocumentHistory) {
        guidanceDocumentHistoryRepository.save(guidanceDocumentHistory);
    }

    public Guidance convertEmailToGuidance(EmailMessage emailMessage) {
        Guidance guidance = new Guidance();
        guidance.setName(emailMessage.getSubject());

        if (!emailMessage.getBriefBody().isEmpty()) {
            guidance.setDescription(emailMessage.getBriefBody());
        } else {
            logger.info("Email body is empty");
            guidance.setDescription(emailMessage.getBody());
        }

        guidance.setDateAdded(emailMessage.getDateReceived());

        return guidance;
    }


    private GuidanceDocumentHistory convertEmailAttachmentToGuidanceDocumentHistory(EmailAttachment emailAttachment, Guidance guidance) {

        GuidanceDocumentHistory guidanceDocumentHistory = new GuidanceDocumentHistory();
        guidanceDocumentHistory.setGuidance(guidance);

        String fileURL = azureFileStorageService.copyMigratedFile(emailAttachment, guidanceDocumentHistory);

        if (fileURL.isEmpty()) {
            logger.warn("File URL is empty");
        } else {
            guidanceDocumentHistory.setDocumentURL(fileURL);
        }

        guidanceDocumentHistory.setCreatedDate(new Date()); // Set the current date as the created date
        guidanceDocumentHistory.setCreatedBy(1); // Set the created by user id. Adjust this according to your requirements

        return guidanceDocumentHistory;
    }
}
