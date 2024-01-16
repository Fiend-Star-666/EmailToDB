package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class FileDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadService.class);


    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    public void downloadFile(Long attachmentId, String outputDirectory) {

        Optional<EmailAttachment> optionalAttachment = emailAttachmentRepository.findById(attachmentId);

        if (optionalAttachment.isPresent()) {
            EmailAttachment attachment = optionalAttachment.get();
            byte[] fileContent = attachment.getFileContent();

            // Construct the output path using the file name and extension
            String outputPath = outputDirectory + "/" + attachment.getFileName() + "." + attachment.getFileExtension();

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("No attachment found with id: " + attachmentId);
        }
    }

    public void downloadAllFiles(String outputFolderName) throws IOException {

        List<EmailAttachment> attachments = emailAttachmentRepository.findAll();
        for (EmailAttachment attachment : attachments) {

            byte[] fileContent = attachment.getFileContent();

            String outputPath = "E:\\Professional\\utility\\Email-to-Db\\EmailToDb\\src\\main\\resources\\output" + "\\" + attachment.getFileName();


            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(fileContent);
                logger.info("File Saved successfully");
            } catch (IOException e) {
                logger.error("Error occurred while writing file: " + e.getMessage());
            }

        }
    }
}
