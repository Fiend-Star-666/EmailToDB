//package com.emailtodb.emailtodb.services;
//
//import com.azure.storage.blob.BlobClient;
//import com.azure.storage.blob.BlobContainerClient;
//import com.azure.storage.blob.BlobServiceClient;
//import com.azure.storage.blob.BlobServiceClientBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.nio.file.Path;
//
//@Service
//public class AzureFileStorageService {
//
//    private final BlobServiceClient blobServiceClient;
//    private final String containerName;
//
//    public AzureFileStorageService(@Value("${azure.storage.connection-string}") String connectionString,
//                                   @Value("${azure.storage.container-name}") String containerName) {
//        this.blobServiceClient = new BlobServiceClientBuilder()
//                .connectionString(connectionString)
//                .buildClient();
//        this.containerName = containerName;
//    }
//
//    public String uploadFile(Path filePath) {
//        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
//        String fileName = filePath.getFileName().toString();
//        BlobClient blobClient = containerClient.getBlobClient(fileName);
//
//        blobClient.uploadFromFile(filePath.toString());
//
//        return blobClient.getBlobUrl();
//    }
//}