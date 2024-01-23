package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class EmailLabelService {

    private static final Logger logger = LoggerFactory.getLogger(EmailLabelService.class);

    private static final String USER_ID = "me";

    private final GmailConfig gmailConfig;

    @Autowired
    public EmailLabelService(GmailConfig gmailConfig) {
        this.gmailConfig = gmailConfig;
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
            List<String> labelIdsList = getLabelIdByName(USER_ID, labelIds);

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
