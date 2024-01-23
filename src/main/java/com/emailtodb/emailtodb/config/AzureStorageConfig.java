package com.emailtodb.emailtodb.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfig {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    public BlobServiceClient createBlobServiceClient() {

        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
//                .addPolicy(new HttpLoggingPolicy(
//                        new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
//                ))
                .addPolicy((context, next) -> {
                    context.setData("Azure-Storage-Log-String-To-Sign", true);
                    return next.process();
                })
                .buildClient();

    }
}
