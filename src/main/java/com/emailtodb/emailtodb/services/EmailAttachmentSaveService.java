package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.google.api.services.gmail.model.Message;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
public class EmailAttachmentSaveService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAttachmentSaveService.class);

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailAttachmentFetchService emailAttachmentFetchService;

    @Autowired
    private AzureFileStorageService azureFileStorageService;

    @Transactional
    public void saveEmailAttachmentsIfNotExists(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {

        //azureFileStorageService.createContainer();

        List<EmailAttachment> attachments = emailAttachmentFetchService.getAttachments(message, emailMessage);

        logger.info("emailMessage attachments: {}", attachments.size());

        // Inside saveEmailAttachmentsIfNotExists method
        for (EmailAttachment attachment : attachments) {
            Optional<EmailAttachment> existingEmailAttachment = emailAttachmentRepository.findByFileContentHash(attachment.getFileContentHash());
            if (existingEmailAttachment.isEmpty()) {
                try {

                    // Only save the attachment if it doesn't already exist
                    EmailAttachment uploadedAttachment = azureFileStorageService.uploadFile(attachment);

                    if (uploadedAttachment != null) {
                        logger.info("Saving new email attachment with hash: {}", attachment.getFileContentHash());
                        emailAttachmentRepository.save(attachment);
                    } else {
                        logger.error("Error uploading file to Azure Blob Storage");
                        throw new RuntimeException("Error uploading file to Azure Blob Storage: " + attachment.getFileContentHash());
                    }

                } catch (Exception e) {
                    logger.error("Error uploading file to Azure Blob Storage: {}", e.getMessage());
                    return;
                }
                logger.info("Saved new email attachment with hash: {}", attachment.getFileContentHash());
            } else {
                logger.info("Attachment with hash {} already exists, skipping save.", attachment.getFileContentHash());
            }
        }
    }


}
