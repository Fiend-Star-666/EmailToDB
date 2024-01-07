package com.emailtodb.emailtodb;

import com.emailtodb.emailtodb.config.GmailConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.EmailMessage;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.emailtodb.emailtodb.repositories.EmailMessageRepository;
import com.emailtodb.emailtodb.services.EmailService;
import com.google.api.services.gmail.Gmail;
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

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    @Test
    void contextLoads() {
    }

    @Mock
    private EmailMessageRepository emailMessageRepository;
    @Mock
    private EmailAttachmentRepository emailAttachmentRepository;
    @Mock
    private GmailConfig gmailConfig;
    @InjectMocks
    private EmailService emailService;

    @Test
    public void fetchAndSaveEmailsConditionallyTest() throws Exception {
        // Arrange
        Message mockMessage = new Message();

        //... setup mockMessage and mockPart ...
        MessagePart mockPart = new MessagePart();
        MessagePartBody partBody = new MessagePartBody();
        partBody.setData("mockData");
        mockPart.setBody(partBody);
        mockMessage.setPayload(new MessagePart().setParts(Collections.singletonList(mockPart)));

        List<Message> messages = new ArrayList<>();
        messages.add(mockMessage); // Add mock message to the list

        // Mock the Gmail service
        Gmail mockGmailService = mock(Gmail.class);
        Gmail.Users mockUsers = mock(Gmail.Users.class);
        Gmail.Users.Messages mockMessages = mock(Gmail.Users.Messages.class);
        Gmail.Users.Messages.List mockList = mock(Gmail.Users.Messages.List.class);

        when(gmailConfig.getGmailServiceAccount()).thenReturn(mockGmailService);
        when(mockGmailService.users()).thenReturn(mockUsers);
        when(mockUsers.messages()).thenReturn(mockMessages);
        when(mockMessages.list(anyString())).thenReturn(mockList);

        // Act
        emailService.fetchAndSaveEmailsConditionally();

        // Assert
        verify(emailMessageRepository, atLeast(1)).save(any(EmailMessage.class));
        verify(emailAttachmentRepository, atLeast(1)).save(any(EmailAttachment.class));
    }

}
