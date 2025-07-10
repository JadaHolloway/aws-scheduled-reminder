package com.reminder.lambda;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Concrete implementation of the INotificationService interface
 * 
 * @author Jada Holloway
 * @version 1.0
 */
public class NotificationService implements INotificationService {
    /**
     * AWS client used to interact with the Simple Notification Service (SNS)
     * Used for**: Publishing messages (e.g., sending email reminders via SNS
     * topic)
     */
    private final AmazonSNS snsClient;

    // ----------- Constructor -----------

    /**
     * Amazon Resource Name (ARN) of the SNS topic used to send email
     * notifications.
     */
    private final String SNS_TOPIC_ARN;

    public NotificationService(AmazonSNS snsClient, String SNS_TOPIC_ARN) {
        this.snsClient = snsClient;
        this.SNS_TOPIC_ARN = SNS_TOPIC_ARN;
    }

    // ----------- Method -----------


    @Override
    public void sendReminder(Item item, Context context) {
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
        // TODO Auto-generated method stub

    }
}
