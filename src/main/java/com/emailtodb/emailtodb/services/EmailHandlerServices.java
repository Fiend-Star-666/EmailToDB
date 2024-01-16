package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class EmailHandlerServices {

    private final GmailConfig gmailConfig;
    @Value("${gmail.user.email}")
    private String userEmail;
    @Value("${gmail.user.email.summary.to}")
    private String toEmail;
    @Value("${gmail.user.email.summary.cc}")
    private String ccEmail;

    @Autowired
    public EmailHandlerServices(GmailConfig gmailConfig) {
        this.gmailConfig = gmailConfig;
    }

    // Move email to another folder or label it
    public void labelEmailAsProcessed(String userId, String messageId, List<String> labelIds) throws IOException {
        ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelIds);
        gmailConfig.getGmailServiceAccount().users().messages().modify(userId, messageId, mods).execute();
    }

    // Send summary email
    public void sendSummaryEmail(String userId, String summary) throws IOException {

        String subject = "Summary for The Date:" + new Date();

        Message message = createMessageWithEmail(subject, summary);
        gmailConfig.getGmailServiceAccount().users().messages().send(userId, message).execute();
    }

    private Message createMessageWithEmail(String subject, String bodyText) {

        String emailContent = String.format("From: %s%nTo: %s%nCc: %s%nSubject: %s%n%n%s", userEmail, toEmail, ccEmail, subject, bodyText);

        byte[] emailBytes = emailContent.getBytes(StandardCharsets.UTF_8);
        String encodedEmail = Base64.getEncoder().encodeToString(emailBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
