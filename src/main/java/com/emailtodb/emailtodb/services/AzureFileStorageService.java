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
import java.util.ArrayList;
import java.util.List;

@Service
public class AzureFileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(AzureFileStorageService.class);

    @Autowired
    private AzureStorageConfig azureStorageConfig;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String uploadFile(EmailAttachment attachment) {
        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        BlobClient blobClient = containerClient.getBlobClient(attachment.getFileName());

        try {
            blobClient.upload(new ByteArrayInputStream(attachment.getFileContent()), attachment.getFileContent().length);
            return blobClient.getBlobUrl();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 409 && e.getErrorCode().equals(BlobErrorCode.BLOB_ALREADY_EXISTS)) {
                // Log the error and continue
                logger.warn("Blob already exists: " + attachment.getFileName());
                return blobClient.getBlobUrl();
            }
            logger.error("Error uploading file to Azure Blob Storage: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Error uploading file to Azure Blob Storage: " + e.getMessage());
        }
        return null;
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