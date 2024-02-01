package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.Guidance;
import com.emailtodb.emailtodb.entities.GuidanceDocumentHistory;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataMigrationService {

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

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
    }

    // Implement these methods based on your specific requirements
    private Guidance processEmailMessage(EmailMessage emailMessage) {
        // ...
        return null;
    }

    private void insertIntoGuidance(Guidance guidance) {
        // ...
    }

    private GuidanceDocumentHistory processEmailAttachment(EmailAttachment emailAttachment) {
        // ...
        return null;
    }

    private void insertIntoGuidanceDocumentHistory(GuidanceDocumentHistory guidanceDocumentHistory) {
        // ...
    }

    public Guidance convertEmailToGuidance(EmailMessage emailMessage) {
        Guidance guidance = new Guidance();
        guidance.setName(emailMessage.getSubject());

        if(!emailMessage.getBriefBody().isEmpty()){
            guidance.setDescription(emailMessage.getBriefBody());
        } else {
            guidance.setDescription(emailMessage.getBody());
        }

        guidance.setDateAdded(emailMessage.getDateReceived());

        return guidance;
    }

    public void migrateGuidanceData() {

    }
}
