package com.reminder.lambda;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * AWS Lambda function that reads scheduled reminders from a DynamoDB table
 * and sends email notifications using Amazon SNS.
 *
 * Scans for reminders that are due, publishes them to an SNS topic,
 * and updates the table to mark them as sent.
 *
 * @author Jada Holloway
 * @version 1.0
 */
public class ReminderHandler implements RequestHandler<Object, String> {

    /**
     * Low-level client used to interact with Amazon DynamoDB service
     * Used for: Creating a connection to DynamoDB that can be wrapped by
     * higher-level interfaces like `DynamoDB`
     */
    AmazonDynamoDB dynamoDBClient;

    /**
     * High-level DynamoDB wrapper used to access and operate on DynamoDB tables
     * Used for: Accessing tables, querying, scanning, and performing operations
     * using object-style APIs
     */
    DynamoDB dynamoDB;

    /**
     * Represents the DynamoDB 'Reminder' table
     * Used for: Performing read/write operations on the `Reminders` table like
     * scan, update, and query.
     */
    Table table;

    /**
     * AWS client used to interact with the Simple Notification Service (SNS)
     * Used for**: Publishing messages (e.g., sending email reminders via SNS
     * topic)
     */
    AmazonSNS snsClient;

    /**
     * Name of the DynamoDB table storing scheduled reminders.
     */
    private static final String TABLE_NAME = "Reminders";

    /**
     * Amazon Resource Name (ARN) of the SNS topic used to send email
     * notifications.
     */
    private static final String SNS_TOPIC_ARN =
        "arn:aws:sns:us-east-2:311141561637:ReminderTopic";

    // ----------- Constructor -----------

    /**
     * Initializes the ReminderHandler by creating and configuring AWS service
     * clients
     * for DynamoDB and SNS, and establishing a connection to the reminders
     * table.
     */
    public ReminderHandler() {
        dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        dynamoDB = new DynamoDB(dynamoDBClient);
        snsClient = AmazonSNSClientBuilder.standard().build();
        table = dynamoDB.getTable(TABLE_NAME);
    }

    // ----------- Methods -----------


    /**
     * AWS lambda entry point - orchestrates logic
     */
    @Override
    public String handleRequest(Object input, Context context) {
        // Get current UTC time and format it as ISO-8601
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var currerntTime = now.format(DateTimeFormatter.ISO_INSTANT);

        var remindersList = getDueReminders(currerntTime);

        int count = 0; // Counter for processed reminders

        for (Item item : remindersList) {
            String userId = item.getString("userID");
            String reminderTime = item.getString("reminderTime");

            sendEmailReminder(item, context);

            markAsSent(userId, reminderTime, context);

            context.getLogger().log("Sending reminder to: " + userId);

            count++;
        }

        if (count == 1) {
            return "Processed " + count + " reminders.";
        }
        return "Processed " + count + " reminders.";
    }


    /**
     * Step 1: Fetch Data
     * Scans DynamoDB and returns reminders that are due (i.e., scheduled before
     * or at the current time and not yet sent).
     *
     * @param currentTimeISO
     *            The current UTC time in ISO-8601 format used to filter due
     *            reminders.
     * @return An ItemCollection containing reminders that are due and unsent.
     */
    private ItemCollection<ScanOutcome> getDueReminders(String currentTimeISO) {
        ScanSpec scanSpec = new ScanSpec().withFilterExpression(
            "reminderTime <= :now and isSent = :false").withValueMap(
                new ValueMap()
                    /**
                     * ValueMap - Maps placeholders (:var) to real values
                     * now - currentTimeISO
                     */
                    .withString(":now", currentTimeISO).withBoolean(":false",
                        false));

        /**
         * ItemCollection<T>
         * A special iterable result set used by AWS SDK for scan/query
         * operations.
         * It contains multiple Item objects, like a List<Item>, but it:
         * 
         * Is lazily loaded (fetches results page-by-page from DynamoDB)
         * 
         * Can be used in a for-each loop
         * 
         * Is not a java.util.List, but behaves similarly
         * 
         * ScanOutcome
         * A wrapper class that contains metadata about the scan
         */
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);

        return items;
    }


    /**
     * Step 2: Act on it
     * Publishes the reminder message to an SNS topic so that all subscribed
     * endpoints receive it.
     *
     * @param item
     *            The DynamoDB item containing the reminder details (userId,
     *            email, message, reminderTime).
     * @param context
     *            The Lambda execution context used for logging and runtime
     *            information.
     */
    private void sendEmailReminder(Item item, Context context) {
        String userId = item.getString("userID");
        String reminderTime = item.getString("reminderTime");
        String messageBody = item.getString("message");

        String message = String.format(
            "Hi %s, \n\nThis is your reminder:\n%s\n\nScheduled at: %s", userId,
            messageBody, reminderTime);

        PublishRequest publishRequest = new PublishRequest(SNS_TOPIC_ARN,
            message);

        try {
            snsClient.publish(publishRequest);

        }
        catch (SdkClientException e) {
            context.getLogger().log("Error publishing reminder: " + e
                .getMessage());
        }

    }


    /**
     * Step 3: mark it done
     * Updates DynamoDB to mark the reminder as sent by setting the 'sent'
     * attribute to true.
     *
     * @param userId
     *            The unique identifier of the user who owns the reminder.
     * @param reminderTime
     *            The scheduled time of the reminder, used as the sort key.
     */
    private void markAsSent(
        String userId,
        String reminderTime,
        Context context) {

        UpdateItemSpec updateSpec = new UpdateItemSpec().withPrimaryKey(
            "userID", userId, "reminderTime", reminderTime)
            .withUpdateExpression("set sent = :val").withValueMap(new ValueMap()
                .withBoolean(":val", true));

        try {
            table.updateItem(updateSpec);
        }
        catch (Exception e) {
            context.getLogger().log("Failed to mark reminder as sent for user "
                + userId + ": " + e.getMessage());
        }
    }
}
