package com.reminder.lambda;

import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;

/**
 * Concrete implementation of the IReminderRepository interface
 * 
 * @author Jada Holloway
 * @version 1.0
 */
public class ReminderRepository implements IReminderRepository {
    /**
     * Represents the DynamoDB 'Reminder' table
     * Used for: Performing read/write operations on the `Reminders` table like
     * scan, update, and query.
     */
    private final Table table;

    // ----------- Constructor -----------

    public ReminderRepository(Table table) {
        this.table = table;
    }

    // ----------- Methods -----------


    @Override
    public ItemCollection<ScanOutcome> getDueReminders(String currentTimeISO) {
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


    @Override
    public void markAsSent(
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
