package com.emailtodb.emailtodb.services;

import com.google.api.services.gmail.model.MessagePart;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessagePartProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagePartProcessingService.class);

    @Value("${email.body.regex.filter}")
    private String bodyRegexFilter;

    public List<String> getGoogleDriveFileIdsIfLink(MessagePart part) {

        // Get the body of the part
        String body = getBody(part);

        List<String> fileIds = new ArrayList<>();

        if (body == null) {
            return fileIds;
        }

        // Remove angle brackets if present
        body = body.replaceAll("[<>]", "");

        // Regex to match Google Drive link and capture file ID directly
        String regex = "https://drive\\.google\\.com/file/d/([^/]+)(/view\\?usp=drive_web)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        while (matcher.find()) {
            // Extract the file ID directly from the regex group
            String fileId = matcher.group(1);
            fileIds.add(fileId);
        }

        return fileIds;
    }

    public String getBody(MessagePart part) {
        if ("text/plain".equals(part.getMimeType()) || "text/html".equals(part.getMimeType())) {
            String body = new String(Base64.decodeBase64(part.getBody().getData()));

            // Regex to match the specific part of the email body
            String regex = bodyRegexFilter;
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(body);

            if (matcher.find()) {
                // Extract the specific part of the email body
                return matcher.group(1);
            }
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
