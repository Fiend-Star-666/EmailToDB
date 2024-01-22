package com.emailtodb.emailtodb.config;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@Configuration
public class DriveConfig {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final String SERVICE_SECRET_FILE = "/service_secrets.json";

    private static final String serviceSecretsJson = System.getenv("service-secrets");

    @Value("${gmail.user.email}")
    private String userEmail;

    private static final int READ_TIMEOUT = (int) Duration.ofMinutes(5).toMillis();  // 5 minutes

    private static final int CONNECTION_TIMEOUT = (int) Duration.ofMinutes(5).toMillis();  // 5 minutes

    /*
    To delegate domain-wide authority to your service account, follow these steps:
    1) Go to your Google Admin console at admin.google.com. Note: You must be an administrator of the G Suite domain to access the Admin console.
    2) From the Admin console Home page, go to Security > API controls.
    3) In the Domain wide delegation pane, select Manage Domain Wide Delegation.
    4) Click Add new.
    5)In the Client ID field, enter the client ID obtained from your service account JSON key file.
    6) In the OAuth Scopes field, enter the scopes for the APIs that the service account should access. For the Gmail API, you can use https://mail.google.com/.
    7) Click Authorize.
    8) Now, your service account has domain-wide delegation of authority and can access user data for users in your G Suite domain.
     */
    public Drive getDriveServiceAccount() throws IOException {

        // Convert the JSON string to InputStream
        InputStream serviceSecretsStream = new ByteArrayInputStream(serviceSecretsJson.getBytes(StandardCharsets.UTF_8));

        // Load credentials from the client_secret.json file
        GoogleCredentials credentials = GoogleCredentials.fromStream(Objects.requireNonNull(serviceSecretsStream))
                .createScoped(DriveScopes.all())
                //.createScoped(DriveScopes.DRIVE_READONLY)
                .createDelegated(this.userEmail); // replace with the user you want to impersonate;

        HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credentials) {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                super.initialize(httpRequest);
                httpRequest.setReadTimeout(READ_TIMEOUT);
                httpRequest.setConnectTimeout(CONNECTION_TIMEOUT);
            }
        };

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, httpRequestInitializer)
                .setApplicationName("Email to DB Application")
                .build();

    }
}
