package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.config.DriveConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailDriveService {

    @Autowired
    private DriveConfig driveConfig;

    @Autowired
    private EmailAttachmentService emailAttachmentService;

    @Autowired
    private MessagePartProcessingService messagePartProcessingService;

}
