package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmailSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSummaryService.class);
    private static final String PROCESSED_LABEL = "Processed";
    private static final String FAILED_TO_PROCESS_LABEL = "FailedToProcess";

    @Autowired
    private EmailMessageRepository emailMessageRepository;
    @Autowired
    private GmailConfig gmailConfig;
    @Autowired
    private EmailLabelService emailLabelService;
    @Autowired
    private EmailSaveService emailSaveService;
    @Autowired
    private EmailFetchService emailFetchService;

    @Value("${gmail.sender.emailFilter}")
    private String emailFilter;
    @Value("${gmail.user.email}")
    private String userEmail;
    @Value("${gmail.user.email.summary.to}")
    private List<String> toEmails;
    @Value("${gmail.user.email.summary.cc}")
    private List<String> ccEmails;

    public void fetchAndSaveEmailsConditionally() throws IOException {

        StringBuilder summary = new StringBuilder();
        StringBuilder successSummary = new StringBuilder();
        StringBuilder failedSummary = new StringBuilder();

        int totalEmailsRead = 0;
        int totalEmailsAdded = 0;
        int totalEmailsFailedToLoad = 0;

        logger.info("Conditionally fetching and saving emails started");

        summary.append(System.lineSeparator()).append("Email Processing Summary:").append(System.lineSeparator());

        //In the context of the Gmail API, "me" is an alias for the authenticated user who is making the request
        String userId = "me";

        Optional<EmailMessage> latestEmail = emailMessageRepository.findTopByOrderByDateReceivedDesc();

        Gmail gmail = gmailConfig.getGmailServiceAccount();

        List<Message> newMessages = new ArrayList<>();

        if (latestEmail.isPresent()) {

            Date sinceDate = latestEmail.get().getDateReceived();

            if (sinceDate != null) {
                newMessages = emailFetchService.fetchMessagesSince(gmail, userId, sinceDate);
            }
        } else {
            newMessages = emailFetchService.fetchMessages(gmail);
        }

        logger.info("Fetched " + newMessages.size() + " new messages");

        if (newMessages.isEmpty()) {
            logger.info("No new messages");
            return;
        }

        for (Message message : newMessages) {

            totalEmailsRead++;

            EmailMessage emailMessage = emailSaveService.extractEmailMessageFromGmailMessage(message);

            // Check if the "body" or the "From" field contains the string emailFilter
            if (emailMessage.getBody().contains(emailFilter) || emailMessage.getFrom().contains(emailFilter)) {
                totalEmailsAdded++;
                try {

                    emailSaveService.saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);

                    emailLabelService.labelEmailAsProvidedLabel(userId, message.getId(), PROCESSED_LABEL);

                    successSummary.append("Successfully Loaded the email with messageID: ").append(emailMessage.getMessageId()).append(System.lineSeparator()).
                            append("Subject: ").append(emailMessage.getSubject()).append(System.lineSeparator()).append(System.lineSeparator()).append(System.lineSeparator());

                    if (emailMessage.getEmailAttachments() != null)
                        summary.append("Number of Email Attachments:").append(emailMessage.getEmailAttachments().size()).append(System.lineSeparator());

                } catch (Exception e) {
                    totalEmailsFailedToLoad++;
                    logger.error("Error while saving email message and its attachments: " + e.getMessage());

                    emailLabelService.labelEmailAsProvidedLabel(userId, message.getId(), FAILED_TO_PROCESS_LABEL);

                    failedSummary.append(" Failed to Load the email with messageID: ").append(emailMessage.getMessageId()).append(System.lineSeparator())
                            .append("Subject: ").append(emailMessage.getSubject()).append(System.lineSeparator()).append(System.lineSeparator()).append(System.lineSeparator());
                }
            }
        }

        summary.append("-------------------------------------").append(System.lineSeparator());
        summary.append("Total emails read from Inbox: ").append(totalEmailsRead).append(System.lineSeparator());
        summary.append("Total emails added to database after Filtering: ").append(totalEmailsAdded).append(System.lineSeparator());
        summary.append("Total emails failed to load: ").append(totalEmailsFailedToLoad).append(System.lineSeparator());
        summary.append("-------------------------------------").append(System.lineSeparator()).append(System.lineSeparator());

        summary.append("Successful Emails Logs:").append(System.lineSeparator());

        if (totalEmailsAdded == 0) {
            summary.append("No emails added to database after Filtering").append(System.lineSeparator());
        } else summary.append(successSummary).append(System.lineSeparator());

        summary.append("------------------------").append(System.lineSeparator());


        summary.append("Failed Emails Summary:").append(System.lineSeparator());
        if (totalEmailsFailedToLoad == 0) {
            summary.append("No failures in Loading Emails").append(System.lineSeparator());
        } else summary.append(failedSummary).append(System.lineSeparator());

        summary.append(System.lineSeparator()).append("------------------------").append(System.lineSeparator());


        sendSummaryEmail(userId, summary.toString());

        logger.info("Conditional fetching and saving emails completed");

    }

    // Send summary email
    private void sendSummaryEmail(String userId, String summary) throws IOException {

        Locale locale = new Locale("en", "US"); // Locale for USA
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York"); // Time zone for Florida (Eastern Time Zone)
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy , EEEE, HH:mm:ss", locale);
        dateFormat.setTimeZone(timeZone); // Set the time zone to the DateFormat
        String formattedDate = dateFormat.format(new Date());

        String subject = "Summary for The Date: " + formattedDate;

        Message message = createMessageWithEmail(subject, summary);

        try {
            gmailConfig.getGmailServiceAccount().users().messages().send(userId, message).execute();
            logger.info("Summary email sent");
        } catch (Exception e) {
            logger.error("Error sending summary email: {}", e.getMessage());
            throw e;
        }
    }

    private Message createMessageWithEmail(String subject, String bodyText) {

        String cc = String.join(", ", ccEmails);
        String to = String.join(", ", toEmails);

        String emailContent = String.format("From: %s%nTo: %s%nCc: %s%nSubject: %s%n%n%s", userEmail, to, cc, subject, bodyText);

        byte[] emailBytes = emailContent.getBytes(StandardCharsets.UTF_8);
        String encodedEmail = Base64.getEncoder().encodeToString(emailBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

}
