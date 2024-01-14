package com.emailtodb.emailtodb.services;

import com.google.api.services.gmail.model.MessagePart;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MessagePartProcessingService {

    public Optional<String> getGoogleDriveFileIdIfLink(MessagePart part) {
        // Get the body of the part
        String body = getBody(part);
        if (body == null) {
            return Optional.empty();
        }

        // Check if the body is a Google Drive link
        if (!body.matches("https://drive\\.google\\.com/file/d/[^/]+")) {
            return Optional.empty();
        }

        // Extract the file ID from the Google Drive link
        int start = body.indexOf("https://drive.google.com/file/d/") + "https://drive.google.com/file/d/".length();
        int end = body.indexOf('/', start);
        if (end == -1) {
            end = body.length();
        }
        String fileId = body.substring(start, end);

        return Optional.of(fileId);
    }

    public String getBody(MessagePart part) {
        if (part.getMimeType().equals("text/plain") || part.getMimeType().equals("text/html")) {
            return new String(Base64.decodeBase64(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart nestedPart : part.getParts()) {
                String body = getBody(nestedPart);
                if (body != null) {
                    return body;
                }
            }
        }
        return null;
    }
}
