package com.emailtodb.emailtodb.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Objects;

@Configuration
public class GmailConfig {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final String CLIENT_SECRET_FILE = "/client_secrets.json";

    private static final String SERVICE_SECRET_FILE = "/service_secrets.json";

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    @Value("${gmail.user.email}")
    private String userEmail;

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
    public  Gmail getGmailServiceAccount() throws IOException {

        // Load credentials from the client_secret.json file
        GoogleCredentials credentials = GoogleCredentials.fromStream(Objects.requireNonNull(GmailConfig.class.getResourceAsStream(SERVICE_SECRET_FILE)))
                //.createScoped(GmailScopes.all())
                .createScoped(GmailScopes.GMAIL_READONLY)
                .createDelegated(this.userEmail); // replace with the user you want to impersonate;

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName("Email to DB Application")
                .build();

    }

    public static Gmail getGmailClientAccount() throws IOException, GeneralSecurityException {

        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = getCredentials(httpTransport);

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Email to DB Application")
                .build();

    }

    private static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {

        // Load client secrets.
        Reader clientSecretReader = new InputStreamReader(Objects.requireNonNull(GmailConfig.class.getResourceAsStream(CLIENT_SECRET_FILE)));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(GmailScopes.GMAIL_READONLY))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

}
