package com.emailtodb.emailtodb.services;

import com.emailtodb.emailtodb.entities.EmailAttachment;
import com.emailtodb.emailtodb.repositories.EmailAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class FileDownloadService {

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private ResourceLoader resourceLoader;

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
            System.out.println("No attachment found with id: " + attachmentId);
        }
    }

    public void downloadAllFiles(String outputFolderName) throws IOException {

        List<EmailAttachment> attachments = emailAttachmentRepository.findAll();
        outputFolderName = "output";
        for (EmailAttachment attachment : attachments) {

            byte[] fileContent = attachment.getFileContent();

            // Construct the output path using the file name and extension
            // Resource resource = resourceLoader.getResource("classpath:" + outputFolderName);
            //File outputDirectory = resource.getFile();

            String outputPath = "E:\\Professional\\utility\\Email-to-Db\\EmailToDb\\src\\main\\resources\\output" + "\\" + attachment.getFileName() + attachment.getFileContentHash() + "." + attachment.getFileExtension();


            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
