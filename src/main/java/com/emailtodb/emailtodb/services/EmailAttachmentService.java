package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.DriveConfig;
import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import jakarta.transaction.Transactional;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.net.URLDecoder.decode;

@Service
public class EmailAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAttachmentService.class);

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private GmailConfig gmailConfig;

    @Autowired
    private DriveConfig driveConfig;

    @Autowired
    private MessagePartProcessingService messagePartProcessingService;

    public List<EmailAttachment> getAttachments(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {
        logger.info("Getting attachments started");
        List<EmailAttachment> attachments = new ArrayList<>();
        getAttachmentsRecursive(message.getPayload(), emailMessage, attachments);
        logger.info("Getting attachments completed");
        return attachments;
    }

    private void getAttachmentsRecursive(MessagePart part, EmailMessage emailMessage, List<EmailAttachment> attachments) throws NoSuchAlgorithmException, IOException {
        processGoogleDriveLinks(part, emailMessage, attachments);
        processRegularAttachments(part, emailMessage, attachments);
        processNestedParts(part, emailMessage, attachments);
    }

    private void processGoogleDriveLinks(MessagePart part, EmailMessage emailMessage, List<EmailAttachment> attachments) throws IOException, NoSuchAlgorithmException {

        List<String> driveFileIds = messagePartProcessingService.getGoogleDriveFileIdsIfLink(part);

        for (String driveFileId : driveFileIds) {
            logger.info("Google Drive Link Found");
            EmailAttachment attachment = createDriveAttachment(driveFileId, emailMessage);
            attachments.add(attachment);
        }
    }

    private EmailAttachment createDriveAttachment(String driveFileId, EmailMessage emailMessage) throws IOException, NoSuchAlgorithmException {
        EmailAttachment attachment = new EmailAttachment();
        attachment.setEmailMessage(emailMessage);

        Drive driveService = driveConfig.getDriveServiceAccount();

        // Retrieve the file metadata from Google Drive
        File driveFile = driveService.files().get(driveFileId).execute();
        String fullFileName = driveFile.getName();
        attachment.setFileName(fullFileName);

        // Extract and set file extension, if available
        int lastDotIndex = fullFileName.lastIndexOf('.');
        String fileExtension = (lastDotIndex > 0 && lastDotIndex < fullFileName.length() - 1) ?
                fullFileName.substring(lastDotIndex + 1) : "";
        attachment.setFileExtension(fileExtension);

        // Download the file content
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(driveFileId).executeMediaAndDownloadTo(outputStream);
        byte[] fileContent = outputStream.toByteArray();
        attachment.setFileContent(fileContent);
        attachment.setFileContentHash(generateFileContentHash(fileContent));

        return attachment;
    }


    private void processRegularAttachments(MessagePart part, EmailMessage emailMessage, List<EmailAttachment> attachments) throws NoSuchAlgorithmException, IOException {
        if (part.getBody().getAttachmentId() != null && part.getFilename() != null) {
            logger.info("Attachment found");
            EmailAttachment attachment = createRegularAttachment(part, emailMessage);
            if (isNewAttachment(attachment.getFileContentHash())) {
                attachments.add(attachment);
                logger.info("Attachment added");
            }
        }
    }

    private EmailAttachment createRegularAttachment(MessagePart part, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {
        EmailAttachment attachment = new EmailAttachment();
        byte[] fileByteArray = decodeAttachmentBody(part,emailMessage);
        attachment.setFileContent(fileByteArray);
        attachment.setFileName(determineFileName(part));
        attachment.setFileExtension(determineFileExtension(part));
        attachment.setEmailMessage(emailMessage);
        attachment.setFileContentHash(generateFileContentHash(fileByteArray));
        return attachment;
    }

    private boolean isNewAttachment(String fileContentHash) {
        return emailAttachmentRepository.findByFileContentHash(fileContentHash).isEmpty();
    }

    private void processNestedParts(MessagePart part, EmailMessage emailMessage, List<EmailAttachment> attachments) throws NoSuchAlgorithmException, IOException {
        if (part.getParts() != null) {
            for (MessagePart nestedPart : part.getParts()) {
                getAttachmentsRecursive(nestedPart, emailMessage, attachments);
            }
        }
    }

// Add additional helper methods like decodeAttachmentBody, determineFileName, determineFileExtension, and generateFileContentHash here...
private byte[] decodeAttachmentBody(MessagePart part, EmailMessage emailMessage) throws IOException {
    MessagePartBody attachmentBody;
    if (part.getBody().getAttachmentId() != null) {
        attachmentBody = gmailConfig.getGmailServiceAccount().users().messages().attachments()
                .get("me", emailMessage.getMessageId(), part.getBody().getAttachmentId()).execute();
    } else {
        attachmentBody = part.getBody();
    }
    return Base64.decodeBase64(attachmentBody.getData());
}

    private String determineFileName(MessagePart part) {
        String fileName = part.getFilename();
        return (fileName != null && !fileName.isEmpty()) ? decode(fileName, StandardCharsets.UTF_8) : "unknown";
    }

    private String determineFileExtension(MessagePart part) {
        String decodedFileName = determineFileName(part);
        int lastDotIndex = decodedFileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < decodedFileName.length() - 1) {
            return decodedFileName.substring(lastDotIndex + 1);
        } else {
            logger.warn("No file extension found for filename: " + decodedFileName);
            return "unknown";
        }
    }

    private String generateFileContentHash(byte[] fileContent) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileContent);
        return bytesToHex(hash);
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
                logger.info("Saved new email attachment with hash: " + attachment.getFileContentHash());
            } else {
                logger.info("Attachment with hash " + attachment.getFileContentHash() + " already exists, skipping save.");
            }
        }
    }

}