package com.reminder.lambda;

import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.lambda.runtime.Context;

/**
 * Interface representing the contract for interacting with the reminders data
 * store.
 * 
 * Provides methods for querying reminders that are due and marking them as
 * sent.
 * This abstraction enables loose coupling between business logic and the
 * underlying
 * data access layer, allowing for easier testing and future scalability.
 * 
 * 
 * @author Jada Holloway
 * @version 1.0
 */
public interface IReminderRepository {

    /**
     * Retrieves reminders that are due to be sent.
     * 
     * A reminder is considered due if its scheduled reminder time is earlier
     * than or equal
     * to the provided current time and it has not yet been marked as sent.
     *
     * @param currentTimeISO
     *            The current UTC time in ISO-8601 format used as a filter
     *            threshold.
     * @return
     *         An {@link ItemCollection} of {@link ScanOutcome} representing the
     *         unsent reminders that are due.
     */
    ItemCollection<ScanOutcome> getDueReminders(String currentTimeISO);


    /**
     * Marks a reminder as sent in the data store.
     * 
     * This method updates the `sent` attribute of a reminder to `true`,
     * ensuring it will not be re-sent in future executions.
     *
     * @param userId
     *            The user identifier, used as the primary partition key.
     * @param reminderTime
     *            The scheduled reminder time, used as the sort key.
     * @param context
     *            The AWS Lambda context object, used for logging and tracing
     *            execution.
     */
    void markAsSent(String userId, String reminderTime, Context context);
}
