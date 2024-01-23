package com.emailtodb.emailtodb.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.emailtodb.emailtodb.config.AzureStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class AzureFileStorageService {

    @Autowired
    private AzureStorageConfig azureStorageConfig;

    @Value("${azure.storage.container-name}")
    private String containerName;

    public String uploadFile(byte[] fileContent) {
        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();
        String blobName = UUID.randomUUID().toString();
        System.out.println("Container Name: " + containerName);
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName);
        blobClient.upload(new ByteArrayInputStream(fileContent), fileContent.length);
        return blobClient.getBlobUrl();
    }
}