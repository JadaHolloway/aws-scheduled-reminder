package com.reminder.lambda;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;

/**
 * Interface representing a notification service for sending reminders.
 * 
 * 
 * @author Jada Holloway
 * @version 1.0
 */
public interface INotificationService {

    /**
     * Sends a reminder notification based on the provided DynamoDB item.
     *
     * @param item
     *            The DynamoDB item representing the reminder to be sent.
     *            Expected to
     *            contain fields like recipient name, email, reminder message,
     *            etc.
     *
     * @param context
     *            The AWS Lambda context object, used for logging or tracing
     *            execution
     *            details.
     */
    void sendReminder(Item item, Context context);
}
