package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public List<EmailAttachment> getAttachments(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException {

        logger.info("Getting attachments started");
        List<EmailAttachment> attachments = new ArrayList<>();
        MessagePart payload = message.getPayload();

        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                if (part.getFilename() != null && part.getBody().getData() != null) {
                    EmailAttachment attachment = new EmailAttachment();
                    byte[] fileByteArray = Base64.decodeBase64(part.getBody().getData());
                    String fileName = part.getFilename();
                    String fileExtension = "";
                    int lastDotIndex = fileName.lastIndexOf('.');
                    if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                        fileExtension = fileName.substring(lastDotIndex + 1);
                    }
                    attachment.setFileName(fileName);
                    attachment.setFileExtension(fileExtension);
                    attachment.setFileContent(fileByteArray);
                    attachment.setEmailMessage(emailMessage); // Set the relation to the email message

                    // Generate SHA-256 hash of the file content
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(fileByteArray);
                    String fileContentHash = bytesToHex(hash);

                    // Check if an attachment with the same file content hash already exists
                    Optional<EmailAttachment> existingAttachment = emailAttachmentRepository.findByFileContentHash(fileContentHash);
                    if (existingAttachment.isPresent()) {
                        // Skip this attachment because it already exists in the database
                        continue;
                    }

                    attachment.setFileContentHash(fileContentHash);
                    attachments.add(attachment);
                    logger.info("Attachment added");
                }
            }
        }
        logger.info("Getting attachments completed");
        return attachments;
    }

    public List<Message> fetchMessages(Gmail service) {
        logger.info("Fetching messages started");

        List<Message> messages = new ArrayList<>();
        try {
            // Define the user (typically 'me' for the authenticated user)
            String user = "me";

            // Fetch only a limited number of messages for demonstration; adjust as needed
            ListMessagesResponse messageResponse = service.users().messages().list(user).setMaxResults(10L).execute();
            List<Message> messageIds = messageResponse.getMessages();

            if (messageIds != null) {
                for (Message messageId : messageIds) {
                    // Fetch the full message using the ID
                    Message message = service.users().messages().get(user, messageId.getId()).execute();
                    messages.add(message);
                    logger.info("Fetched message with ID: " + message.getId());
                }
            }
        } catch (IOException e) {
            logger.error("An error occurred: " + e);
        }

        logger.info("Fetching messages completed");
        return messages;
    }

    public void fetchAndSaveEmailsConditionally() throws IOException, GeneralSecurityException {

        logger.info("Conditionally fetching and saving emails started");

        String userId = "me";
        Optional<EmailMessage> latestEmail = emailMessageRepository.findTopByOrderByDateSentDesc();

        Gmail gmail = GmailConfig.getGmailClientAccount();

        List<Message> newMessages;

        if (latestEmail.isPresent()) {
            Date sinceDate = latestEmail.get().getDateSent();

            if (sinceDate != null) {
                newMessages = fetchMessagesSince(gmail, userId, sinceDate);
            } else {
                newMessages = fetchMessages(gmail);
            }
        } else {
            newMessages = fetchMessages(gmail);
        }
        logger.info("Fetched " + newMessages.size() + " new messages");

        for (Message message : newMessages) {

            EmailMessage emailMessage = extractEmailMessageFromGmailMessage(message);
            saveEmailMessageIfNotExists(emailMessage);
            saveEmailAttachmentsIfNotExists(message, emailMessage);
        }

        logger.info("Conditional fetching and saving emails completed");

    }


    private void saveEmailAttachmentsIfNotExists(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException {
        List<EmailAttachment> attachments = getAttachments(message, emailMessage);

        logger.info("emailMessage attachments: " + attachments.size());

        for (EmailAttachment attachment : attachments) {
            Optional<EmailAttachment> existingEmailAttachment = emailAttachmentRepository.findByFileContentHash(attachment.getFileContentHash());

            if (existingEmailAttachment.isPresent()) {
                logger.info("Email attachment with name " + attachment.getFileName() + " already exists");
                continue;
            }
            emailAttachmentRepository.save(attachment);
            logger.info("Saved email attachment");
        }
    }

    private void saveEmailMessageIfNotExists(EmailMessage emailMessage) {

        Optional<EmailMessage> existingEmailMessage = emailMessageRepository.findByMessageId(emailMessage.getMessageId());

        if (existingEmailMessage.isPresent()) {
            logger.info("Email message with ID " + emailMessage.getMessageId() + " already exists");
            return;
        }

        emailMessageRepository.save(emailMessage);
        logger.info("Saved email message");

    }

    private EmailMessage extractEmailMessageFromGmailMessage(Message message) {

        EmailMessage emailMessage = new EmailMessage();

        // Extracting message details from the Gmail Message object
        String subject = "";
        String from = "";
        String to = "";
        String cc = "";
        String bcc = "";
        Date dateSent = null;
        String body = "";

        // Extract headers for subject, from, to, cc, bcc, and date
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            switch (header.getName()) {
                case "Subject":
                    subject = header.getValue();
                    break;
                case "From":
                    from = header.getValue();
                    break;
                case "To":
                    to = header.getValue();
                    break;
                case "Cc":
                    cc = header.getValue();
                    break;
                case "Bcc":
                    bcc = header.getValue();
                    break;
                case "Date":
                    // Parsing the date from the header, adjust the format as needed
                    SimpleDateFormat parser = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
                    try {
                        dateSent = parser.parse(header.getValue());
                    } catch (ParseException e) {
                        logger.error("Error parsing date: " + e.getMessage());
                    }
                    break;
            }
        }

        // Extract the body of the email
        // Assuming the body is in a text/plain part
        for (MessagePart part : message.getPayload().getParts()) {
            if ("text/plain".equals(part.getMimeType())) {
                body = new String(Base64.decodeBase64(part.getBody().getData()));
                break;
            }
        }

        // Setting properties for emailMessage from the extracted message object
        emailMessage.setMessageId(message.getId());
        emailMessage.setSubject(subject);
        emailMessage.setFrom(from);
        emailMessage.setTo(to);
        emailMessage.setCc(cc);
        emailMessage.setBcc(bcc);
        emailMessage.setDateSent(dateSent);
        emailMessage.setBody(body);
        logger.info("Extracted email message details");

        // Set seen, answered, deleted, and draft fields
        List<String> labelIds = message.getLabelIds();
        emailMessage.setSeen(labelIds.contains("SEEN"));
        emailMessage.setAnswered(labelIds.contains("ANSWERED"));
        emailMessage.setDeleted(labelIds.contains("TRASH"));
        emailMessage.setDraft(labelIds.contains("DRAFT"));

        return emailMessage;
    }

    public List<Message> fetchMessagesSince(Gmail service, String userId, Date sinceDate) throws IOException {
        logger.info("Fetching messages since " + sinceDate);
        SimpleDateFormat gmailDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        gmailDateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // Gmail uses GMT

        String query = "after:" + gmailDateFormat.format(sinceDate);
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

        List<Message> messages = new ArrayList<>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ(query).setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        // Fetch the full details for each message
        List<Message> detailedMessages = new ArrayList<>();
        for (Message message : messages) {
            Message detailedMessage = service.users().messages().get(userId, message.getId()).execute();
            detailedMessages.add(detailedMessage);
        }
        return detailedMessages;
    }
}
