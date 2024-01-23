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
    private static final String USER_ID = "me";

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

    private int totalEmailsAdded = 0;
    private int totalEmailsFailedToLoad = 0;

    public void fetchAndSaveEmailsConditionally() throws IOException {

        logger.info("Conditionally fetching and saving emails started");

        int totalEmailsRead = 0;
        totalEmailsAdded = 0;
        totalEmailsFailedToLoad = 0;

        Optional<EmailMessage> latestEmail = emailMessageRepository.findTopByOrderByDateReceivedDesc();
        List<Message> newMessages = fetchNewMessages(latestEmail);
        String summary = processMessages(newMessages, totalEmailsRead);
        sendSummaryEmailIfNecessary(summary);

        logger.info("Conditional fetching and saving emails completed");

    }

    private List<Message> fetchNewMessages(Optional<EmailMessage> latestEmail) throws IOException {
        Gmail gmail = gmailConfig.getGmailServiceAccount();
        if (latestEmail.isPresent()) {
            Date sinceDate = latestEmail.get().getDateReceived();
            if (sinceDate != null) {
                return emailFetchService.fetchMessagesSince(gmail, USER_ID, sinceDate);
            }
        }
        return emailFetchService.fetchMessages(gmail);
    }

    private String processMessages(List<Message> newMessages, int totalEmailsRead) throws IOException {

        StringBuilder successSummary = new StringBuilder();
        StringBuilder failedSummary = new StringBuilder();

        for (Message message : newMessages) {

            List<String> labelIds = emailLabelService.getLabelIdByName(USER_ID, PROCESSED_LABEL);

            Set<String> labelIdsSet = new HashSet<>(labelIds);
            Set<String> messageLabelIdsSet = new HashSet<>(message.getLabelIds());

            if (messageLabelIdsSet.containsAll(labelIdsSet)) {
                continue;
            }

            totalEmailsRead++;

            EmailMessage emailMessage = emailSaveService.extractEmailMessageFromGmailMessage(message);

            try {
                if (emailMessageRepository.existsByMessageId(emailMessage.getMessageId())) {
                    logger.info("Email message with ID {} already exists", emailMessage.getMessageId());
                } else {
                    if (emailMessage.getBody().contains(emailFilter) || emailMessage.getFrom().contains(emailFilter)) {
                        emailSaveService.saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);
                        totalEmailsAdded++;
                        successSummary.append("Successfully Loaded the email with messageID: ").append(emailMessage.getMessageId())
                                .append(System.lineSeparator()).append("Subject: ").append(emailMessage.getSubject())
                                .append(System.lineSeparator()).append(System.lineSeparator()).append(System.lineSeparator());
                        if (emailMessage.getEmailAttachments() != null && !emailMessage.getEmailAttachments().isEmpty())
                            successSummary.append("Number of Email Attachments:").append(emailMessage.getEmailAttachments().size()).append(System.lineSeparator());
                    }
                }

                emailLabelService.labelEmailAsProvidedLabel(USER_ID, message.getId(), PROCESSED_LABEL);

            } catch (Exception e) {
                totalEmailsFailedToLoad++;
                logger.error("Error while saving email message and its attachments: " + e.getMessage());
                failedSummary.append(" Failed to Load the email with messageID: ").append(emailMessage.getMessageId())
                        .append(System.lineSeparator()).append("Subject: ").append(emailMessage.getSubject())
                        .append(System.lineSeparator()).append(System.lineSeparator()).append(System.lineSeparator());

                // Label the email as failed to process
                try {
                    emailLabelService.labelEmailAsProvidedLabel(USER_ID, message.getId(), FAILED_TO_PROCESS_LABEL);
                } catch (IOException ioException) {
                    logger.error("Error while labeling email as failed to process: " + ioException.getMessage());
                }
            }
        }

        StringBuilder summary = new StringBuilder();
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

        return summary.toString();
    }

    private void sendSummaryEmailIfNecessary(String summary) throws IOException {
        if (!summary.isEmpty() && (totalEmailsAdded > 0 || totalEmailsFailedToLoad > 0)) {
            sendSummaryEmail(summary);
        }else{
            logger.info("No summary email sent as there were no new emails");
        }
    }

    // Send summary email
    private void sendSummaryEmail(String summary) throws IOException {

        Locale locale = new Locale("en", "US"); // Locale for USA
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York"); // Time zone for Florida (Eastern Time Zone)
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy , EEEE, HH:mm:ss", locale);
        dateFormat.setTimeZone(timeZone); // Set the time zone to the DateFormat
        String formattedDate = dateFormat.format(new Date());

        String subject = "Summary for The Date: " + formattedDate;

        Message message = createMessageWithEmail(subject, summary);

        try {
            gmailConfig.getGmailServiceAccount().users().messages().send(USER_ID, message).execute();
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