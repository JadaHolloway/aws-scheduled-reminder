# aws-scheduled-reminder

A serverless Java application that automatically sends scheduled email reminders. An AWS Lambda function scans a DynamoDB table for due reminders, publishes them to an SNS topic for email delivery, and marks each one as sent to prevent duplicate notifications.

## Architecture

```
CloudWatch Events (scheduled trigger)
          │
          ▼
   AWS Lambda (Java 17)
   ReminderHandler
          │
          ▼
   ReminderService
   ├── ReminderRepository ──► DynamoDB ("Reminders" table)
   │       Scan: reminderTime <= now AND isSent = false
   │       Update: set isSent = true after processing
   │
   └── NotificationService ──► Amazon SNS ──► Email subscribers
```

## Prerequisites

The following AWS resources must exist before deployment:

### DynamoDB Table

| Setting | Value |
|---|---|
| Table name | `Reminders` |
| Partition key | `userID` (String) |
| Sort key | `reminderTime` (String, ISO-8601 UTC — e.g. `2025-06-01T09:00:00Z`) |

Additional item attributes expected at runtime:

| Attribute | Type | Description |
|---|---|---|
| `message` | String | The reminder text to include in the email |
| `isSent` | Boolean | Set to `false` for unsent reminders; updated to `true` after delivery |

### SNS Topic

Create an SNS topic and add at least one email subscription. The topic ARN is configured in `ReminderHandler.java`.

### Lambda Function

| Setting | Value |
|---|---|
| Function name | `SendReminderFunction` |
| Runtime | Java 17 |
| Handler | `com.reminder.lambda.ReminderHandler::handleRequest` |

**Required IAM permissions for the Lambda execution role:**
- `dynamodb:Scan` on the `Reminders` table
- `dynamodb:UpdateItem` on the `Reminders` table
- `sns:Publish` on the SNS topic

**Trigger:** Add a CloudWatch Events (EventBridge) rule with a schedule expression — for example `rate(5 minutes)` — pointing to `SendReminderFunction`.

### GitHub Actions (CI/CD)

Deployment is automated via GitHub Actions using OIDC. You will need:

1. An IAM role (`aws-scheduled-reminder-GitHubRole`) configured to trust the GitHub OIDC provider for this repository.
2. The role must have permission to call `lambda:UpdateFunctionCode` on `SendReminderFunction`.

## Configuration

All configuration values are currently hardcoded. If you fork this project, update the following before deploying:

| Value | File | What to change |
|---|---|---|
| SNS topic ARN | `ReminderHandler.java` | Replace `SNS_TOPIC_ARN` with your topic ARN |
| DynamoDB table name | `ReminderHandler.java` | Replace `"Reminders"` in `dynamoDB.getTable(...)` |
| Lambda function name | `.github/workflows/deploy.yml` | Update the `function-name` in the deploy step |
| IAM role ARN | `.github/workflows/deploy.yml` | Update `role-to-assume` with your role ARN |
| AWS region | `.github/workflows/deploy.yml` | Update `aws-region` if deploying outside `us-east-2` |

## Project Structure

```
src/main/java/com/reminder/lambda/
├── ReminderHandler.java        # Lambda entry point; wires up AWS clients
├── ReminderService.java        # Orchestrates fetch → notify → mark-sent flow
├── ReminderRepository.java     # DynamoDB data access
├── NotificationService.java    # SNS publish logic
├── IReminderRepository.java    # Repository interface (enables unit testing)
└── INotificationService.java   # Notification interface (enables unit testing)
```

## Local Development

### Build

```bash
mvn clean install
```

This produces a fat JAR (via Maven Shade Plugin) under `target/` that can be uploaded directly to Lambda.

### Run Tests

```bash
mvn test
```

Unit tests use Mockito to inject mock implementations of `IReminderRepository` and `INotificationService`, so no AWS credentials are needed to run them locally.

## Deployment

Deployment to Lambda is handled automatically by the GitHub Actions workflow (`.github/workflows/deploy.yml`) on every push to `main`. The workflow:

1. Checks out the code and sets up JDK 17
2. Assumes the IAM role via OIDC (no long-lived AWS credentials stored in GitHub)
3. Builds the project with `mvn clean install`
4. Zips the fat JAR and deploys it to `SendReminderFunction` using the AWS CLI

Manual deployment (if needed):

```bash
mvn clean install
zip deployment.zip target/aws-scheduled-reminder-*.jar
aws lambda update-function-code \
  --function-name SendReminderFunction \
  --zip-file fileb://deployment.zip \
  --region us-east-2
```
