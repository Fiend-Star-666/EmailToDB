package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import jakarta.transaction.Transactional;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.net.URLDecoder.decode;

@Service
public class EmailAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAttachmentService.class);

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private GmailConfig gmailConfig;

    public List<EmailAttachment> getAttachments(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {
        logger.info("Getting attachments started");
        List<EmailAttachment> attachments = new ArrayList<>();
        getAttachmentsRecursive(message.getPayload(), emailMessage, attachments);
        logger.info("Getting attachments completed");
        return attachments;
    }

    private void getAttachmentsRecursive(MessagePart part, EmailMessage emailMessage, List<EmailAttachment> attachments) throws NoSuchAlgorithmException, IOException {
        if (part.getBody().getAttachmentId() != null && part.getFilename() != null) {
            logger.info("Attachment found-----------------------------------------");
            EmailAttachment attachment = new EmailAttachment();
            byte[] fileByteArray;

            if (part.getBody().getAttachmentId() != null) {
                MessagePartBody attachmentBody = gmailConfig.getGmailServiceAccount().users().messages().attachments().get("me", emailMessage.getMessageId(), part.getBody().getAttachmentId()).execute();
                fileByteArray = Base64.decodeBase64(attachmentBody.getData());
                attachment.setFileName(part.getFilename());
            } else {
                fileByteArray = Base64.decodeBase64(part.getBody().getData());
                attachment.setFileName(part.getFilename());
            }

            // Generate SHA-256 hash of the file content
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileByteArray);
            String fileContentHash = bytesToHex(hash);

            String fileExtension = "unknown";
            String decodedFileName = "unknown";

            if (part.getFilename() != null && !part.getFilename().isEmpty()) {
                decodedFileName = decode(part.getFilename(), StandardCharsets.UTF_8);
                logger.info("Extracted filename (decoded): " + decodedFileName);
                attachment.setFileName(decodedFileName);
                int lastDotIndex = decodedFileName.lastIndexOf('.');

                if (lastDotIndex > 0 && lastDotIndex < decodedFileName.length() - 1) {
                    fileExtension = decodedFileName.substring(lastDotIndex + 1);
                    logger.info("Extracted file extension: " + fileExtension);
                } else {
                    logger.warn("No file extension found for filename: " + decodedFileName);
                }
            } else {
                logger.warn("Filename is null or empty for partId: " + part.getPartId());
            }
            attachment.setFileName(decodedFileName);
            attachment.setFileExtension(fileExtension);
            attachment.setFileContent(fileByteArray);
            attachment.setEmailMessage(emailMessage); // Set the relation to the email message

            // Check if an attachment with the same file content hash already exists
            Optional<EmailAttachment> existingAttachment = emailAttachmentRepository.findByFileContentHash(fileContentHash);
            if (existingAttachment.isPresent()) {
                // Skip this attachment because it already exists in the database
                return;
            }

            attachment.setFileContentHash(fileContentHash);
            attachments.add(attachment);
            logger.info("Attachment added");
        }

        if (part.getParts() != null) {
            for (MessagePart nestedPart : part.getParts()) {
                getAttachmentsRecursive(nestedPart, emailMessage, attachments);
            }
        }
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Transactional
    public void saveEmailAttachmentsIfNotExists(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {

        List<EmailAttachment> attachments = getAttachments(message, emailMessage);

        logger.info("emailMessage attachments: " + attachments.size());

        // Inside saveEmailAttachmentsIfNotExists method
        for (EmailAttachment attachment : attachments) {
            Optional<EmailAttachment> existingEmailAttachment = emailAttachmentRepository.findByFileContentHash(attachment.getFileContentHash());
            if (existingEmailAttachment.isEmpty()) {
                // Only save the attachment if it doesn't already exist
                emailAttachmentRepository.save(attachment);
                //emailAttachmentRepository.flush(); // Force save to database immediately
                logger.info("Saved new email attachment with hash: " + attachment.getFileContentHash());
            } else {
                logger.info("Attachment with hash " + attachment.getFileContentHash() + " already exists, skipping save.");
            }
        }
    }

}