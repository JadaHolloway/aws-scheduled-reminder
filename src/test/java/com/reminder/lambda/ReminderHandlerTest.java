package com.reminder.lambda;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class ReminderHandlerTest {

    @Test
    public void testHandleRequestReturnsNotNull() {
        // Arrange
        ReminderHandler handler = new ReminderHandler();

        Context mockContext = mock(Context.class);
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);

        // Act
        String result = handler.handleRequest(null, mockContext);

        // Assert
        assertNotNull(result);
        verify(mockLogger, atLeastOnce()).log(contains("Sending reminder to:"));
    }
}
