package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.Guidance;
import com.emailtodb.emailtodb.entities.GuidanceDocumentHistory;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.emailtodb.emailtodb.repositories.GuidanceDocumentHistoryRepository;
import com.emailtodb.emailtodb.repositories.GuidanceRepository;
import com.emailtodb.emailtodb.scheduler.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DataMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);


    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private GuidanceRepository guidanceRepository;

    @Autowired
    private GuidanceDocumentHistoryRepository guidanceDocumentHistoryRepository;

    @Autowired
    private FileMigrationService fileMigrationService;

    public void migrateData() {

        // Fetch data from the staging tables
        List<EmailMessage> emailMessages = emailMessageRepository.findAll();
        List<EmailAttachment> emailAttachments = emailAttachmentRepository.findAll();

        // Process the data and insert it into the Guidance and GuidanceDocumentHistory tables
        for (EmailMessage emailMessage : emailMessages) {
            // Process the emailMessage and insert it into the Guidance table
            // You need to implement the processEmailMessage and insertIntoGuidance methods
            Guidance guidance = processEmailMessage(emailMessage);
            insertIntoGuidance(guidance);
        }

        for (EmailAttachment emailAttachment : emailAttachments) {
            // Process the emailAttachment and insert it into the GuidanceDocumentHistory table
            // You need to implement the processEmailAttachment and insertIntoGuidanceDocumentHistory methods
            GuidanceDocumentHistory guidanceDocumentHistory = processEmailAttachment(emailAttachment);
            insertIntoGuidanceDocumentHistory(guidanceDocumentHistory);
        }

//        fileMigrationService.migrateFiles();

    }

    // Implement these methods based on your specific requirements
    private Guidance processEmailMessage(EmailMessage emailMessage) {
        // ...
        return null;
    }

    private void insertIntoGuidance(Guidance guidance) {
        guidanceRepository.save(guidance);
    }

    private GuidanceDocumentHistory processEmailAttachment(EmailAttachment emailAttachment) {
        // ...
        return null;
    }

    private void insertIntoGuidanceDocumentHistory(GuidanceDocumentHistory guidanceDocumentHistory) {
        guidanceDocumentHistoryRepository.save(guidanceDocumentHistory);
    }

    public Guidance convertEmailToGuidance(EmailMessage emailMessage) {
        Guidance guidance = new Guidance();
        guidance.setName(emailMessage.getSubject());

        if(!emailMessage.getBriefBody().isEmpty()){
            guidance.setDescription(emailMessage.getBriefBody());
        } else {
            logger.info("Email body is empty");
            guidance.setDescription(emailMessage.getBody());
        }

        guidance.setDateAdded(emailMessage.getDateReceived());

        return guidance;
    }

    public void migrateGuidanceData() {
        List<EmailMessage> emailMessages = emailMessageRepository.findAll();

        for (EmailMessage emailMessage : emailMessages) {
            Guidance guidance = convertEmailToGuidance(emailMessage);
            insertIntoGuidance(guidance);

            // Process the attachments
            for (EmailAttachment emailAttachment : emailMessage.getEmailAttachments()) {
                GuidanceDocumentHistory guidanceDocumentHistory = convertEmailAttachmentToGuidanceDocumentHistory(emailAttachment, guidance);
                insertIntoGuidanceDocumentHistory(guidanceDocumentHistory);
            }
        }

//        fileMigrationService.migrateFiles();
    }

    private GuidanceDocumentHistory convertEmailAttachmentToGuidanceDocumentHistory(EmailAttachment emailAttachment, Guidance guidance) {

        GuidanceDocumentHistory guidanceDocumentHistory = new GuidanceDocumentHistory();
        guidanceDocumentHistory.setGuidance(guidance);
        guidanceDocumentHistory.setDocumentURL(emailAttachment.getFileLocation());
        guidanceDocumentHistory.setCreatedDate(new Date()); // Set the current date as the created date
        guidanceDocumentHistory.setCreatedBy(1); // Set the created by user id. Adjust this according to your requirements

        return guidanceDocumentHistory;
    }
}
