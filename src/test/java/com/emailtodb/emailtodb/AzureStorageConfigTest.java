package com.emailtodb.emailtodb;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.emailtodb.emailtodb.config.AzureStorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class AzureStorageConfigTest {

    @InjectMocks
    private AzureStorageConfig azureStorageConfig;

    @Mock
    private BlobServiceClientBuilder blobServiceClientBuilder;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldThrowExceptionForInvalidConnectionString() {
        when(blobServiceClientBuilder.connectionString(anyString())).thenThrow(new IllegalArgumentException("Invalid connection string."));

        assertThrows(IllegalArgumentException.class, () -> azureStorageConfig.createBlobServiceClient());
    }

//    @Test
//    public void shouldCreateBlobServiceClient() {
//        ReflectionTestUtils.setField(azureStorageConfig, "connectionString", "TestConnectionString");
//        BlobServiceClient result = azureStorageConfig.createBlobServiceClient();
//        assertNotNull(result);
//    }

    @Test
    void shouldHandleNullConnectionString() {
        ReflectionTestUtils.setField(azureStorageConfig, "connectionString", null);
        assertThrows(IllegalArgumentException.class, () -> azureStorageConfig.createBlobServiceClient());
    }

    @Test
    void shouldHandleEmptyConnectionString() {
        ReflectionTestUtils.setField(azureStorageConfig, "connectionString", "");
        assertThrows(IllegalArgumentException.class, () -> azureStorageConfig.createBlobServiceClient());
    }
}
