//package com.emailtodb.emailtodb.config;
//
//import com.azure.storage.blob.BlobContainerClient;
//import com.azure.storage.blob.BlobServiceClient;
//import com.azure.storage.blob.sas.BlobContainerSasPermission;
//import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
//import com.azure.storage.common.sas.SasProtocol;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//
//@Configuration
//public class SaSTokensGeneration {
//
//    @Autowired
//    private AzureStorageConfig azureStorageConfig;
//
//    public String generateSasToken(String containerName) {
//        BlobServiceClient blobServiceClient = azureStorageConfig.createBlobServiceClient();
//        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
//
//        BlobContainerSasPermission permissions = new BlobContainerSasPermission().setListPermission(true);
//
//        OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
//        SasProtocol sasProtocol = SasProtocol.HTTPS_ONLY;
//
//        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions)
//                .setProtocol(sasProtocol);
//
//        return blobContainerClient.generateSas(sasSignatureValues);
//    }
//}