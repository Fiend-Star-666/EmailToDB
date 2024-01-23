package com.emailtodb.emailtodb.services;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmailFetchService {

    private static final Logger logger = LoggerFactory.getLogger(EmailFetchService.class);

    private static final String USER_ID = "me";

    public List<Message> fetchMessages(Gmail service) {
        logger.info("Fetching messages started");

        List<Message> messages = new ArrayList<>();
        try {

            if (service == null) {
                logger.error("Gmail service is null");
                return messages;
            }

            ListMessagesResponse messageResponse = service.users().messages().list(USER_ID).setLabelIds(Collections.singletonList("INBOX")).execute();

            List<Message> messageIds = messageResponse.getMessages();

            if (messageIds != null) {
                for (Message messageId : messageIds) {
                    // Fetch the full message using the ID
                    Message message = service.users().messages().get(USER_ID, messageId.getId()).execute();
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

    public List<Message> fetchMessagesSince(Gmail service, String userId, Date sinceDate) throws IOException {

        logger.info("Fetching messages since " + sinceDate);

        SimpleDateFormat gmailDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        gmailDateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // Gmail uses GMT

        String query = "after:" + gmailDateFormat.format(sinceDate);
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).setLabelIds(Collections.singletonList("INBOX")).execute();

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
