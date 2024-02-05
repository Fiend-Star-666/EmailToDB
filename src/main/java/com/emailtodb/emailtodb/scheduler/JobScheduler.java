package com.emailtodb.emailtodb.scheduler;

import com.emailtodb.emailtodb.services.DataMigrationService;
import com.emailtodb.emailtodb.services.EmailSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@Component
@EnableScheduling
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);
    private final ReentrantLock lock = new ReentrantLock();

    private final EmailSummaryService emailService;
    private final DataMigrationService dataMigrationService;

    public JobScheduler(EmailSummaryService emailService, DataMigrationService dataMigrationService) {
        this.emailService = emailService;
        this.dataMigrationService = dataMigrationService;
    }

    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000, initialDelay = 60 * 1000) // 60 seconds delay
    public void fetchEmailsRegularly() throws IOException {
        if (lock.tryLock()) {
            try {
                fetchAndProcessEmails();
                regularlyMigrateData(); // Calling regularlyMigrateData() after fetchAndProcessEmails() completes successfully
            } finally {
                lock.unlock();
            }
        }
    }

    public void regularlyMigrateData() {
        if (lock.tryLock()) {
            try {
                migrateDataToFinalTables();
            } finally {
                lock.unlock();
            }
        }
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 30 * 1000))
    private void fetchAndProcessEmails() throws IOException {
        emailService.fetchAndSaveEmailsConditionally();
        logger.info("Fetched and saved emails successfully if any");
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 30 * 1000))
    private void migrateDataToFinalTables() {
        logger.info("Trying to Migrate data to Prod tables");
        dataMigrationService.migrateGuidanceData();
        logger.info("Migrated data successfully if any");
    }
}
