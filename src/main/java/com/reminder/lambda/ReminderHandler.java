package com.reminder.lambda;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;

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

    private final ReminderService reminderService;

    private final String SNS_TOPIC_ARN =
        "arn:aws:sns:us-east-2:311141561637:ReminderTopic";

    // ----------- Constructors -----------

    /**
     * Initializes the ReminderHandler by creating and configuring AWS service
     * clients
     * for DynamoDB and SNS, and establishing a connection to the reminders
     * table.
     */
    public ReminderHandler() {
        var dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard()
            .build());
        var snsClient = AmazonSNSClientBuilder.standard().build();
        var table = dynamoDB.getTable("Reminders");

        IReminderRepository repo = new ReminderRepository(table);
        INotificationService notifier = new NotificationService(snsClient,
            SNS_TOPIC_ARN);

        this.reminderService = new ReminderService(repo, notifier);
    }


    /**
     * constructor for DI (used in tests)
     * 
     * @param reminderService
     */
    public ReminderHandler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    // ----------- Methods -----------


    /**
     * AWS lambda entry point - orchestrates logic
     */
    @Override
    public String handleRequest(Object input, Context context) {
        int count = reminderService.processDueReminders(context);
        return "Processed " + count + " reminder" + (count == 1 ? "" : "s")
            + ".";
    }

}
