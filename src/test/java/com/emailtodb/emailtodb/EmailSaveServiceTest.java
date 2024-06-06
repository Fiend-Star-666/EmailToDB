package com.emailtodb.emailtodb;

import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.services.EmailAttachmentSaveService;
import com.emailtodb.emailtodb.services.EmailSaveService;
import com.emailtodb.emailtodb.services.MessagePartProcessingService;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailSaveServiceTest {

    @InjectMocks
    private EmailSaveService emailSaveService;

    @Mock
    private MessagePartProcessingService messagePartProcessingService;

    @Mock
    private EmailAttachmentSaveService emailAttachmentSaveService;

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldExtractEmailMessageFromGmailMessage() {
        Message message = new Message();
        message.setId("123");
        message.setPayload(new MessagePart().setHeaders(Arrays.asList(
                new MessagePartHeader().setName("Subject").setValue("Test Subject"),
                new MessagePartHeader().setName("From").setValue("test@example.com"),
                new MessagePartHeader().setName("To").setValue("to@example.com"),
                new MessagePartHeader().setName("Cc").setValue("cc@example.com"),
                new MessagePartHeader().setName("Bcc").setValue("bcc@example.com"),
                new MessagePartHeader().setName("Date").setValue("Fri, 1 Jan 2021 00:00:00 +0000")
        )));

        when(messagePartProcessingService.getBody(any())).thenReturn("Test Body");
        when(messagePartProcessingService.fetchBriefBody(any())).thenReturn("Test Brief Body");

        EmailMessage emailMessage = emailSaveService.extractEmailMessageFromGmailMessage(message);

        assertEquals("123", emailMessage.getMessageId());
        assertEquals("Test Subject", emailMessage.getSubject());
        assertEquals("test@example.com", emailMessage.getFrom());
        assertEquals("to@example.com", emailMessage.getTo());
        assertEquals("cc@example.com", emailMessage.getCc());
        assertEquals("bcc@example.com", emailMessage.getBcc());
        assertEquals("Test Body", emailMessage.getBody());
        assertEquals("Test Brief Body", emailMessage.getBriefBody());
    }

    @Test
    void shouldHandleNullBody() {
        Message message = new Message();
        message.setId("123");
        message.setPayload(new MessagePart().setHeaders(Arrays.asList(
                new MessagePartHeader().setName("Subject").setValue("Test Subject"),
                new MessagePartHeader().setName("From").setValue("test@example.com"),
                new MessagePartHeader().setName("To").setValue("to@example.com"),
                new MessagePartHeader().setName("Cc").setValue("cc@example.com"),
                new MessagePartHeader().setName("Bcc").setValue("bcc@example.com"),
                new MessagePartHeader().setName("Date").setValue("Fri, 1 Jan 2021 00:00:00 +0000")
        )));

        when(messagePartProcessingService.getBody(any())).thenReturn(null);
        when(messagePartProcessingService.fetchBriefBody(any())).thenReturn(null);

        EmailMessage emailMessage = emailSaveService.extractEmailMessageFromGmailMessage(message);

        assertEquals("", emailMessage.getBody());
        assertEquals("", emailMessage.getBriefBody());
    }

    @Test
    void shouldHandleInvalidDate() {
        Message message = new Message();
        message.setId("123");
        message.setPayload(new MessagePart().setHeaders(Arrays.asList(
                new MessagePartHeader().setName("Subject").setValue("Test Subject"),
                new MessagePartHeader().setName("From").setValue("test@example.com"),
                new MessagePartHeader().setName("To").setValue("to@example.com"),
                new MessagePartHeader().setName("Cc").setValue("cc@example.com"),
                new MessagePartHeader().setName("Bcc").setValue("bcc@example.com"),
                new MessagePartHeader().setName("Date").setValue("Invalid Date")
        )));

        when(messagePartProcessingService.getBody(any())).thenReturn("Test Body");
        when(messagePartProcessingService.fetchBriefBody(any())).thenReturn("Test Brief Body");

        EmailMessage emailMessage = emailSaveService.extractEmailMessageFromGmailMessage(message);

        assertNull(emailMessage.getDateReceived());
    }

    @Test
    void shouldSaveEmailMessageAndItsAttachmentsIfNotExists() throws NoSuchAlgorithmException, IOException {
        Message message = new Message();
        message.setId("123");

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setMessageId("123");

        when(emailMessageRepository.findByMessageId(anyString())).thenReturn(Optional.empty());

        emailSaveService.saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);

        verify(emailMessageRepository, times(1)).save(any(EmailMessage.class));
        verify(emailAttachmentSaveService, times(1)).saveEmailAttachmentsIfNotExists(any(Message.class), any(EmailMessage.class));
    }

    @Test
    void shouldNotSaveEmailMessageIfAlreadyExists() throws NoSuchAlgorithmException, IOException {
        Message message = new Message();
        message.setId("123");

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setMessageId("123");

        when(emailMessageRepository.findByMessageId(anyString())).thenReturn(Optional.of(emailMessage));

        emailSaveService.saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);

        verify(emailMessageRepository, times(0)).save(any(EmailMessage.class));
        verify(emailAttachmentSaveService, times(0)).saveEmailAttachmentsIfNotExists(any(Message.class), any(EmailMessage.class));
    }

    @Test
    void shouldRollbackTransactionWhenErrorOccursWhileSavingAttachments() throws NoSuchAlgorithmException, IOException {
        Message message = new Message();
        message.setId("123");

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setMessageId("123");

        when(emailMessageRepository.findByMessageId(anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException()).when(emailAttachmentSaveService).saveEmailAttachmentsIfNotExists(any(Message.class), any(EmailMessage.class));

        emailSaveService.saveEmailMessageAndItsAttachmentsIfNotExists(message, emailMessage);

        verify(emailMessageRepository, times(1)).delete(any(EmailMessage.class));
    }
}
