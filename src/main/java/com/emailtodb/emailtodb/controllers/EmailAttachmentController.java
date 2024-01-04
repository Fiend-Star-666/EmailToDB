package com.emailtodb.emailtodb.controllers;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.services.EmailAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/attachments")
public class EmailAttachmentController {

    @Autowired
    private EmailAttachmentService service;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            EmailAttachment attachment = service.saveAttachment(file);
            return ResponseEntity.ok("File uploaded successfully: " + attachment.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not upload the file: " + file.getOriginalFilename() + "!");
        }
    }
}

