package com.reminder.lambda;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;

/**
 * Service class responsible for processing due reminders.
 * 
 * Coordinates fetching reminders from the repository,
 * sending notifications, and updating their status in the database.
 * 
 * @author Jada Holloway
 * @version 1.0
 */
public class ReminderService {
    private final IReminderRepository repository;
    private final INotificationService notificationService;


    /**
     * Constructs a ReminderService with the provided repository and
     * notification service.
     *
     * @param repository
     *            the reminder repository used to fetch and update reminders
     * @param notificationService
     *            the service responsible for sending notifications
     */
    public ReminderService(
        IReminderRepository repository,
        INotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }



    /**
     * Processes all due reminders by:
     * 1. Retrieving reminders scheduled before the current UTC time
     * 2. Sending notifications for each reminder
     * 3. Marking each reminder as sent in the data store
     *
     * @param context
     *            the AWS Lambda context used for logging
     * @return the number of reminders processed
     */
    public int processDueReminders(Context context) {
        int count = 0;
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(
            DateTimeFormatter.ISO_INSTANT);
        for (Item item : repository.getDueReminders(now)) {
            notificationService.sendReminder(item, context);
            repository.markAsSent(item.getString("userID"), item.getString(
                "reminderTime"), context);
            count++;
        }
        return count;
    }
}
