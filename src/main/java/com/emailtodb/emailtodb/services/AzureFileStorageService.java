package com.emailtodb.emailtodb.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.emailtodb.emailtodb.config.AzureStorageConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.entities.Guidance;
import com.emailtodb.emailtodb.entities.GuidanceDocumentHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Service
public class AzureFileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(AzureFileStorageService.class);

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient containerClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public AzureFileStorageService(AzureStorageConfig azureStorageConfig, @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = azureStorageConfig.createBlobServiceClient();
        logger.info("Azure Blob Storage Service initialized");
        logger.info("Container name: {}", containerName);
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }


    public EmailAttachment uploadFile(EmailAttachment attachment) throws BlobStorageException {

        // Generate the blob name
        String blobName = generateBlobNameStaging(attachment);

        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try {
            blobClient.upload(new ByteArrayInputStream(attachment.getFileContent()), attachment.getFileContent().length);
            attachment.setFileUrl(blobClient.getBlobUrl());
            attachment.setFileLocation(blobClient.getBlobName());
            return attachment;
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 409 && e.getErrorCode().equals(BlobErrorCode.BLOB_ALREADY_EXISTS)) {
                logger.warn("Blob already exists: {}", attachment.getFileContentHash());
                attachment.setFileUrl(blobClient.getBlobUrl());
                attachment.setFileLocation(blobClient.getBlobName());
                return attachment;
            }
            logger.error("Error uploading file to Azure Blob Storage: {}", e.getMessage());
            throw e;
        }
    }

    public String copyMigratedFile(EmailAttachment attachment, GuidanceDocumentHistory guidanceDocumentHistory) {

        String sourceBlobLocation = attachment.getFileLocation();

        BlobClient sourceBlobClient = containerClient.getBlobClient(sourceBlobLocation);

        String destinationBlobName = generateMigratedBlobName(attachment.getFileName(), guidanceDocumentHistory);

        BlobClient destinationBlobClient = containerClient.getBlobClient(destinationBlobName);

        if (sourceBlobClient.exists()) {
            String sourceBlobUrl = sourceBlobClient.getBlobUrl();
            try {
                destinationBlobClient.copyFromUrl(sourceBlobUrl);
            } catch (BlobStorageException e) {
                if (e.getStatusCode() == 409 && e.getErrorCode().equals(BlobErrorCode.BLOB_ALREADY_EXISTS)) {
                    logger.warn("Blob already exists in Migrated Location: {}", attachment.getFileContentHash());
                }
                logger.error("Error copying file to Azure Blob Storage: {}", e.getMessage());
                throw e;
            }
            return destinationBlobClient.getBlobName();
        } else {
            logger.warn("Source blob does not exist: {}", sourceBlobLocation);
        }
        return "";
    }

    private String generateBlobNameStaging(EmailAttachment attachment) {
        // Create a Locale object for USA
        Locale locale = new Locale("en", "US");

        // Create a TimeZone object for New York (Eastern Time Zone)
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York");

        // Create a SimpleDateFormat object with the desired format and locale
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MMMM/dd", locale);

        // Set the time zone to the DateFormat
        dateFormat.setTimeZone(timeZone);

        // Get the date the email was received in the specified format, locale, and time zone
        String datePath = dateFormat.format(attachment.getEmailMessage().getDateReceived());

        // Combine the date path, email ID and file name to create the blob name
        return String.format("staging/%s/%s/%s", datePath, attachment.getEmailMessage().getMessageId(), attachment.getFileName());
    }

    private String generateMigratedBlobName(String fileName, GuidanceDocumentHistory guidanceDocumentHistory) {
        Guidance guidance = guidanceDocumentHistory.getGuidance();
        return String.format("Uploads/%d/GDS/%d/Guidance%d/%s", guidance.getCompanyId(), guidance.getGuidanceId(), guidance.getGuidanceId(), fileName);
    }

    public void listAllContainers() {
        List<String> containerNames = new ArrayList<>();
        for (BlobContainerItem blobContainerItem : blobServiceClient.listBlobContainers()) {
            containerNames.add(blobContainerItem.getName());
        }
        logger.info("Containers: {}", containerNames);
    }

    public void deleteContainer() {
        blobServiceClient.deleteBlobContainer(containerName);
        logger.info("Container deleted: {}", containerName);
    }

    public void createContainer() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            blobServiceClient.createBlobContainer(containerName);
            logger.info("Container created: {}", containerName);
        } else {
            logger.info("Container already exists: {}", containerName);
        }
    }

}
