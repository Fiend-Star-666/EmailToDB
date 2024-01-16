package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
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
    private GmailConfig gmailConfig;

    @Autowired
    private EmailAttachmentService emailAttachmentService;

    @Autowired
    private MessagePartProcessingService messagePartProcessingService;

    @Value("${gmail.sender.emailFilter}")
    private String emailFilter;

    public void fetchAndSaveEmailsConditionally() throws IOException, GeneralSecurityException {

        logger.info("Conditionally fetching and saving emails started");

        //In the context of the Gmail API, "me" is an alias for the authenticated user who is making the request
        String userId = "me";

        Optional<EmailMessage> latestEmail = emailMessageRepository.findTopByOrderByDateReceivedDesc();

        Gmail gmail = gmailConfig.getGmailServiceAccount();

        List<Message> newMessages = new ArrayList<>();

        if (latestEmail.isPresent()) {

            Date sinceDate = latestEmail.get().getDateReceived();

            if (sinceDate != null) {
                newMessages = fetchMessagesSince(gmail, userId, sinceDate);
            }
        } else {
            newMessages = fetchMessages(gmail);
        }

        logger.info("Fetched " + newMessages.size() + " new messages");

        if (newMessages.isEmpty()) {
            logger.info("No new messages");
            return;
        }

        for (Message message : newMessages) {

            EmailMessage emailMessage = extractEmailMessageFromGmailMessage(message);

            // Check if the "body" or the "From" field contains the string emailFilter
            if (emailMessage.getBody().contains(emailFilter) || emailMessage.getFrom().contains(emailFilter)) {
                saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);
            }
        }

        logger.info("Conditional fetching and saving emails completed");

    }

    public List<Message> fetchMessages(Gmail service) {
        logger.info("Fetching messages started");

        List<Message> messages = new ArrayList<>();
        try {
            // Define the user (typically 'me' for the authenticated user)
            String user = "me";

            if (service == null) {
                logger.error("Gmail service is null");
                return messages;
            }

            ListMessagesResponse messageResponse = service.users().messages().list(user).execute();

            List<Message> messageIds = messageResponse.getMessages();

            if (messageIds != null) {
                for (Message messageId : messageIds) {
                    // Fetch the full message using the ID
                    Message message = service.users().messages().get(user, messageId.getId()).execute();
                    messages.add(message);
                }
            }
            logger.info("Fetched " + messages.size() + " messages");

        } catch (IOException e) {
            logger.error("An error occurred: " + e);
        }

        logger.info("Fetching messages completed");
        return messages;
    }


    private void saveEmailMessageAndItsAttachmentsIfNotExists(Message message, EmailMessage emailMessage) throws NoSuchAlgorithmException, IOException {

        Optional<EmailMessage> existingEmailMessage = emailMessageRepository.findByMessageId(emailMessage.getMessageId());

        if (existingEmailMessage.isPresent()) {
            logger.info("Email message with ID " + emailMessage.getMessageId() + " already exists");
            return;
        }

        emailMessageRepository.save(emailMessage);
        logger.info("Saved email message");

        emailAttachmentService.saveEmailAttachmentsIfNotExists(message, emailMessage);

        // Check for Google Drive links in the message and its attachments
        //checkForGoogleDriveLinks(message);
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
        String body = messagePartProcessingService.getBody(message.getPayload());
        if (body == null) {
            body = "";
        }

        // Setting properties for emailMessage from the extracted message object
        emailMessage.setMessageId(message.getId());
        emailMessage.setSubject(subject);
        emailMessage.setFrom(from);
        emailMessage.setTo(to);
        emailMessage.setCc(cc);
        emailMessage.setBcc(bcc);
        emailMessage.setDateReceived(dateSent);
        emailMessage.setBody(body);

        logger.info("Extracted email message details");

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

//    private void checkForGoogleDriveLinks(Message message) {
//        List<MessagePart> parts = message.getPayload().getParts();
//        if (parts != null) {
//            for (MessagePart part : parts) {
//                Optional<String> fileIdOptional = messagePartProcessingService.getGoogleDriveFileIdsIfLink(part);
//                fileIdOptional.ifPresent(fileId -> {
//                    // Process the Google Drive file ID as needed
//                    // For example, you can print it to the console:
//                    System.out.println("Found Google Drive file ID: " + fileId);
//                });
//            }
//        }
//    }

}
