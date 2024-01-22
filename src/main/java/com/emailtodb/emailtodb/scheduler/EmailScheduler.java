package com.emailtodb.emailtodb.scheduler;

import com.emailtodb.emailtodb.services.EmailSummaryService;
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

//    @Autowired
//    private FileDownloadService fileDownloadService;

    //@Scheduled(cron = "0 0 */12 * * *") // Runs every 12 hours, adjust as needed
    //@Scheduled(fixedDelay = 60*60*12*1000, initialDelay = 1000)
    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    // Runs every 12 hours, with delay of 1 minute after application start up (to allow for email fetching)
    public void fetchEmailsRegularly() {
        for (int i = 0; i < 5; i++) {
            try {
                emailService.fetchAndSaveEmailsConditionally();
                logger.info("Fetched and saved emails successfully");
                break; // If successful, break the loop
            } catch (Exception e) {
                logger.error("Attempt " + (i + 1) + ": Error occurred while fetching and saving emails: " + e);
                if (i == 4) {
                    logger.error("Failed to fetch and save emails after 5 attempts", e);
                }
            }
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
