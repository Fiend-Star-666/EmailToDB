package com.emailtodb.emailtodb.scheduler;

import com.emailtodb.emailtodb.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
@EnableScheduling
public class EmailScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);


    @Autowired
    private EmailService emailService;

    //@Scheduled(cron = "0 0 */12 * * *") // Runs every 12 hours, adjust as needed
    //@Scheduled(fixedDelay = 60*60*12*1000, initialDelay = 1000)
    @Scheduled(fixedDelay = 12*60*1000, initialDelay = 1000)
    public void fetchEmailsRegularly() {
        try {
            emailService.fetchAndSaveEmailsConditionally();
            logger.info("Fetched and saved emails successfully");
        }
        catch (Exception e) {
            logger.error("Error occurred while fetching and saving emails: " + e.getMessage());
        }
    }
}
