package com.emailtodb.emailtodb;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.services.EmailService;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    @Mock
    private EmailMessageRepository emailMessageRepository;
    @Mock
    private EmailAttachmentRepository emailAttachmentRepository;
    @InjectMocks
    private EmailService emailService;

    @Test
    void contextLoads() {
    }

    @Test
    public void fetchAndSaveEmailsConditionallyTest() throws Exception {
        // Arrange
        EmailMessage mockEmailMessage = new EmailMessage();
        Message mockMessage = new Message();

        //... setup mockMessage and mockPart ...
        MessagePart mockPart = new MessagePart();
        MessagePartBody partBody = new MessagePartBody();
        partBody.setData("mockData");
        mockPart.setBody(partBody);
        mockMessage.setPayload(new MessagePart().setParts(Collections.singletonList(mockPart)));

        when(emailMessageRepository.findTopByOrderByDateSentDesc()).thenReturn(Optional.of(mockEmailMessage));
        when(emailMessageRepository.save(any(EmailMessage.class))).thenReturn(new EmailMessage());
        when(emailAttachmentRepository.save(any(EmailAttachment.class))).thenReturn(new EmailAttachment());

        List<Message> messages = new ArrayList<>();
        messages.add(mockMessage); // Add mock message to the list

        // Assuming fetchMessagesSince() and fetchMessages() are made accessible/overridable for testing
//        when(emailService.fetchMessagesSince(any(Gmail.class), anyString(), any(Date.class))).thenReturn(messages);
//        when(emailService.fetchMessages(any(Gmail.class))).thenReturn(messages);

        // Act
        emailService.fetchAndSaveEmailsConditionally();

        // Assert
        verify(emailMessageRepository, atLeast(1)).save(any(EmailMessage.class));
        verify(emailAttachmentRepository, atLeast(1)).save(any(EmailAttachment.class));
    }

}
