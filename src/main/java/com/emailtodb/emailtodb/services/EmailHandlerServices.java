package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
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
public class EmailHandlerServices {

    private static final Logger logger = LoggerFactory.getLogger(EmailHandlerServices.class);

    private final GmailConfig gmailConfig;

    @Value("${gmail.user.email}")
    private String userEmail;
    @Value("${gmail.user.email.summary.to}")
    private List<String> toEmails;
    @Value("${gmail.user.email.summary.cc}")
    private List<String> ccEmails;

    @Autowired
    public EmailHandlerServices(GmailConfig gmailConfig) {
        this.gmailConfig = gmailConfig;
    }


    // Send summary email
    public void sendSummaryEmail(String userId, String summary) throws IOException {

        Locale locale = new Locale("en", "US"); // Locale for USA
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York"); // Time zone for Florida (Eastern Time Zone)
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy , EEEE, HH:mm:ss", locale);
        dateFormat.setTimeZone(timeZone); // Set the time zone to the DateFormat
        String formattedDate = dateFormat.format(new Date());

        String subject = "Summary for The Date: " + formattedDate;

        Message message = createMessageWithEmail(subject, summary);

        try {

            logger.info("Sending summary email");
            gmailConfig.getGmailServiceAccount().users().messages().send(userId, message).execute();

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

    public List<String> getLabelIdByName(String userId, String labelName) throws IOException {
        ListLabelsResponse listResponse = gmailConfig.getGmailServiceAccount().users().labels().list(userId).execute();
        List<Label> labels = listResponse.getLabels();
        for (Label label : labels) {
            if (label.getName().equalsIgnoreCase(labelName)) {
                return Collections.singletonList(label.getId());
            }
        }
        return new ArrayList<>();
    }

    // label the email
    public void labelEmailAsProvidedLabel(String userId, String messageId, String labelIds) throws IOException {
        logger.info("Labeling email as processed");
        try {
            List<String> labelIdsList = getLabelIdByName("me", labelIds);

            if (labelIdsList.isEmpty()) {
                logger.error("Label not found");
                return;
            }

            ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelIdsList);
            gmailConfig.getGmailServiceAccount().users().messages().modify(userId, messageId, mods).execute();
        } catch (Exception e) {
            logger.error("Error labeling email: {}", e.getMessage());
            throw e;
        }
    }
}
