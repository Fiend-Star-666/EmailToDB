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

    //@Scheduled(cron = "0 0 */12 * * *") // Runs every 12 hours, adjust as needed
    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000, initialDelay = 90 * 1000)
    // Runs every 12 hours, with delay of 1 minute after application start up (to allow for email fetching)
    public void fetchEmailsRegularly() {
        for (int i = 0; i < 5; i++) {
            try {
                emailService.fetchAndSaveEmailsConditionally();
                logger.info("Fetched and saved emails successfully");
                break; // If successful, break the loop
            } catch (Exception e) {
                logger.error("Attempt {}: Error occurred while fetching and saving emails: {}", (i + 1), e.getMessage());
                if (i == 4) {
                    logger.error("Failed to fetch and save emails after 5 attempts", e);
                }
            }
        }
    }

}
