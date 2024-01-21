package com.emailtodb.emailtodb.scheduler;

import com.emailtodb.emailtodb.services.EmailSummaryService;
import com.emailtodb.emailtodb.services.FileDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class EmailScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);


    @Autowired
    private EmailSummaryService emailService;

    @Autowired
    private FileDownloadService fileDownloadService;

    //@Scheduled(cron = "0 0 */12 * * *") // Runs every 12 hours, adjust as needed
    //@Scheduled(fixedDelay = 60*60*12*1000, initialDelay = 1000)
    @Scheduled(fixedDelay = 20 * 60 * 1000, initialDelay = 60*1000)
    public void fetchEmailsRegularly() {
        try {
            emailService.fetchAndSaveEmailsConditionally();
            logger.info("Fetched and saved emails successfully");
        } catch (Exception e) {
            logger.error("Error occurred while fetching and saving emails: " + e.getMessage());
        }
    }

    // Runs every 12 hours, with delay of 5 minutes after application start up (to allow for email fetching)
//    @Scheduled(fixedDelay = 12 * 60 * 1000 * 60, initialDelay = 1000 * 60 * 5)
//    public void savingEmailAttachmentsRegularly() {
//        try {
//            fileDownloadService.downloadAllFiles("output");
//            logger.info("Fetched and saved attachments successfully");
//        } catch (Exception e) {
//            logger.error("Error occurred while fetching and saving attachments: " + e.getMessage());
//        }
//    }
}
