package com.emailtodb.emailtodb.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.emailtodb.emailtodb.config.AzureStorageConfig;
import com.emailtodb.emailtodb.entities.EmailAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AzureStorageConfig azureStorageConfig;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public EmailAttachment uploadFile(EmailAttachment attachment) {

        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // Generate the blob name
        String blobName = generateBlobName(attachment);

        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try {
            blobClient.upload(new ByteArrayInputStream(attachment.getFileContent()), attachment.getFileContent().length);
            logger.info("blobName" + blobClient.getBlobName());
            attachment.setFileUrl(blobClient.getBlobUrl());
            attachment.setFileLocation(blobClient.getBlobName());
            return attachment;
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 409 && e.getErrorCode().equals(BlobErrorCode.BLOB_ALREADY_EXISTS)) {
                // Log the error and continue
                logger.warn("Blob already exists: " + attachment.getFileName());
                attachment.setFileUrl(blobClient.getBlobUrl());
                attachment.setFileLocation(blobClient.getBlobName());
                return attachment;
            }
            logger.error("Error uploading file to Azure Blob Storage: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error uploading file to Azure Blob Storage: " + e.getMessage());
        }
        return null;
    }

    private String generateBlobName(EmailAttachment attachment) {
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
        return String.format("%s/%s/%s", datePath, attachment.getEmailMessage().getMessageId(), attachment.getFileName());
    }

    public void listAllContainers() {
        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();
        List<String> containerNames = new ArrayList<>();
        for (BlobContainerItem blobContainerItem : blobServiceClient.listBlobContainers()) {
            containerNames.add(blobContainerItem.getName());
        }
        logger.info("Containers: " + containerNames);
    }

    public void deleteContainer() {

        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();

        blobServiceClient.deleteBlobContainer(containerName);
        logger.info("Container deleted: " + containerName);
    }

    public void createContainer() {
        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            blobServiceClient.createBlobContainer(containerName);
            logger.info("Container created: " + containerName);
        } else {
            logger.info("Container already exists: " + containerName);
        }
    }

}