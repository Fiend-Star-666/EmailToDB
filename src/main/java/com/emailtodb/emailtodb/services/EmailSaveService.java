package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class EmailSaveService {
    private static final Logger logger = LoggerFactory.getLogger(EmailSaveService.class);

    @Autowired
    private EmailAttachmentSaveService emailAttachmentSaveService;
    @Autowired
    private MessagePartProcessingService messagePartProcessingService;
    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Transactional
    public void saveEmailMessageAndItsAttachmentsIfNotExists(Message message, EmailMessage emailMessage) {

        Optional<EmailMessage> existingEmailMessage = emailMessageRepository.findByMessageId(emailMessage.getMessageId());

        if (existingEmailMessage.isPresent()) {
            logger.info("Email message with ID " + emailMessage.getMessageId() + " already exists");
            return;
        }

        try {
            emailMessageRepository.save(emailMessage);
            logger.info("Saved email message");

            try {

                emailAttachmentSaveService.saveEmailAttachmentsIfNotExists(message, emailMessage);

                if (emailMessage.getEmailAttachments() != null)
                    logger.info("Saved" + emailMessage.getEmailAttachments().size() + " email attachments");
                else logger.info("No email attachments to save");

            } catch (Exception e) {
                logger.error("Error while saving email attachments: " + e.getMessage());
                // Transaction will be rolled back, no need to manually delete the email message due to Transactional annotation
            }

        } catch (Exception e) {
            logger.error("Error while saving email message and its attachments: " + e.getMessage());
        }

    }

    public EmailMessage extractEmailMessageFromGmailMessage(Message message) {

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


}
