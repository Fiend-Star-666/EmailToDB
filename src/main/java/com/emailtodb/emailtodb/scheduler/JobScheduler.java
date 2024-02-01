package com.emailtodb.emailtodb.scheduler;

import com.emailtodb.emailtodb.services.DataMigrationService;
import com.emailtodb.emailtodb.services.EmailSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
@EnableScheduling
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    // Lock to prevent multiple instances of the same job from running concurrently
    private final ReentrantLock lock = new ReentrantLock();


    @Autowired
    private EmailSummaryService emailService;

    @Autowired
    private DataMigrationService dataMigrationService;

    //@Scheduled(cron = "0 0 */12 * * *") // Runs every 12 hours, adjust as needed
    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000, initialDelay = 90 * 1000)
    // Runs every 12 hours, with delay of 1 minute after application start up (to allow for email fetching)
    public void fetchEmailsRegularlyAndMigrateData() {
        if (lock.tryLock()) {
            try {

                for (int i = 0; i < 5; i++) {
                    try {
                        emailService.fetchAndSaveEmailsConditionally();
                        logger.info("Fetched and saved emails successfully");
                        try {
                            logger.info("Migrating data to final tables");
                            migrateDataToFinalTables();
                            logger.info("Migrated data to final tables successfully");
                            break; // If successful, break the loop
                        }
                        catch (Exception e) {
                            logger.error("Error occurred while migrating data to final tables: {}", e.getMessage());
                        }
                        break; // If successful, break the loop
                    } catch (Exception e) {
                        logger.error("Attempt {}: Error occurred while fetching and saving emails: {}", (i + 1), e.getMessage());
                        if (i == 4) {
                            logger.error("Failed to fetch and save emails after 5 attempts", e);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    //@Scheduled(cron = "0 0 */10 * * *") // Runs every 10 hours, adjust as needed
    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000, initialDelay = 90 * 1000)
    public void migrateDataToFinalTables() {
        if (lock.tryLock()) {
            try {
                // Fetch data from the staging tables
                // Process the data and insert it into the Guidance and GuidanceDocumentHistory tables
                for (int i = 0; i < 5; i++) {
                    try {
                        dataMigrationService.migrateGuidanceData();
                        logger.info("Migrated data successfully");
                        break; // If successful, break the loop
                    } catch (Exception e) {
                        logger.error("Attempt {}: Error occurred while migrating data: {}", (i + 1), e.getMessage());
                        if (i == 4) {
                            logger.error("Failed to migrate data after 5 attempts", e);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
