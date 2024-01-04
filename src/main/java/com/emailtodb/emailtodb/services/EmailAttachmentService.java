package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
public class EmailAttachmentService {

    @Autowired
    private EmailAttachmentRepository repository;

    public EmailAttachment saveAttachment(MultipartFile file) throws IOException {
        EmailAttachment attachment = new EmailAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileExtension(getFileExtension(Objects.requireNonNull(file.getOriginalFilename())));
        attachment.setFileContent(file.getBytes());

        return repository.save(attachment);
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}