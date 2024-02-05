package com.emailtodb.emailtodb.services;

import com.azure.storage.blob.BlobServiceClient;
import com.emailtodb.emailtodb.config.AzureStorageConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(FileMigrationService.class);

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private AzureFileStorageService azureFileStorageService;

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient containerClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public FileMigrationService(AzureStorageConfig azureStorageConfig, @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = azureStorageConfig.createBlobServiceClient();
        logger.info("Azure Blob Storage Service initialized");
        logger.info("Container name: {}", containerName);
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

//    public void migrateFiles() {
//        List<EmailAttachment> emailAttachments = emailAttachmentRepository.findAll();
//
//        for (EmailAttachment emailAttachment : emailAttachments) {
//            String oldLocation = emailAttachment.getFileLocation();
//            String fileName = oldLocation.substring(oldLocation.lastIndexOf("/"));
//
//            // Assuming the GuidanceId and CompanyId are available in the EmailAttachment entity
//            String newLocation = String.format("Uploads/%d/GDS/%d/%s", emailAttachment.getGuidance().getCompanyId(), emailAttachment.getGuidance().getGuidanceId(), fileName);
//
//            try {
//
//                BlobContainerClient containerClient = azureFileStorageService.getContainerClient();
//                BlobClient targetBlobClient = containerClient.getBlobClient(newLocation);
//                targetBlobClient.copyFromUrl(emailAttachment.getFileUrl());
//
//                emailAttachment.setFileUrl(targetBlobClient.getBlobUrl());
//                emailAttachment.setFileLocation(newLocation);
//                emailAttachmentRepository.save(emailAttachment);
//            } catch (Exception e) {
//                logger.error("Error while migrating file: " + oldLocation, e);
//            }
//        }
//    }
}