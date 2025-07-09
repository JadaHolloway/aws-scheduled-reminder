package com.reminder.lambda;

import static org.junit.Assert.*;
import org.junit.Test;

public class ReminderHandlerTest {

    @Test
    public void testHandleRequestReturnsNotNull() {
        ReminderHandler handler = new ReminderHandler();
        String result = handler.handleRequest(null, null);
        assertNotNull(result);
    }
}
