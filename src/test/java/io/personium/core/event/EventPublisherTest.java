/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for EventPublisher.
 */
@Category({ Unit.class })
public class EventPublisherTest {

    /**
     * Test convertToEvent().
     * Normal test.
     * message is null.
     */
    @Test
    public void convertToEvent_Normal_message_is_null() {
        // --------------------
        // Test method args
        // --------------------
        MapMessage message = null;

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent event = EventPublisher.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(event);
    }

    /**
     * Test convertToEvent().
     * Normal test.
     * message is valid.
     * @throws Exception exception thrown by mockito
     */
    @Test
    public void convertToEvent_Normal_message_is_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        PersoniumEvent event = new PersoniumEvent(true,
                "schema string", "subject string", "type string",
                "object string", "info string", "requestkey string", "eventid string", "rulechain string");
        event.setCellId("cell id");

        // --------------------
        // Mock settings
        // --------------------
        MapMessage message = mock(MapMessage.class);
        doReturn(true).when(message).itemExists("RequestKey");
        doReturn(true).when(message).itemExists("EventId");
        doReturn(true).when(message).itemExists("RuleChain");
        doReturn(true).when(message).itemExists("External");
        doReturn(true).when(message).itemExists("Schema");
        doReturn(true).when(message).itemExists("Subject");
        doReturn(true).when(message).itemExists("Type");
        doReturn(true).when(message).itemExists("Object");
        doReturn(true).when(message).itemExists("Info");
        doReturn(true).when(message).itemExists("cellId");
        doReturn(event.getRequestKey()).when(message).getString("RequestKey");
        doReturn(event.getEventId()).when(message).getString("EventId");
        doReturn(event.getRuleChain()).when(message).getString("RuleChain");
        doReturn(event.getExternal()).when(message).getBoolean("External");
        doReturn(event.getSchema()).when(message).getString("Schema");
        doReturn(event.getSubject()).when(message).getString("Subject");
        doReturn(event.getType()).when(message).getString("Type");
        doReturn(event.getObject()).when(message).getString("Object");
        doReturn(event.getInfo()).when(message).getString("Info");
        doReturn(event.getCellId()).when(message).getString("cellId");

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventPublisher.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result.getRequestKey(), is(event.getRequestKey()));
        assertThat(result.getEventId(), is(event.getEventId()));
        assertThat(result.getRuleChain(), is(event.getRuleChain()));
        assertThat(result.getExternal(), is(event.getExternal()));
        assertThat(result.getSchema(), is(event.getSchema()));
        assertThat(result.getSubject(), is(event.getSubject()));
        assertThat(result.getType(), is(event.getType()));
        assertThat(result.getObject(), is(event.getObject()));
        assertThat(result.getInfo(), is(event.getInfo()));
        assertThat(result.getCellId(), is(event.getCellId()));
    }

    /**
     * Test convertToEvent().
     * Normal test.
     * JMSException occurred.
     * @throws Exception exception thrown by mockito
     */
    @Test
    public void convertToEvent_Normal_jmsexception_occurred() throws Exception {
        // --------------------
        // Test method args
        // --------------------

        // --------------------
        // Mock settings
        // --------------------
        MapMessage message = mock(MapMessage.class);
        doThrow(new JMSException("dummy")).when(message).itemExists("RequestKey");

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventPublisher.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
    }

    /**
     * Test convertToEvent().
     * Normal test.
     * some entries of message is null.
     * @throws Exception exception thrown by mockito
     */
    @Test
    public void convertToEvent_Normal_some_entries_of_message_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        PersoniumEvent event = new PersoniumEvent(false,
                null, null, "type string",
                "object string", "info string", "requestkey string", "eventid string", "rulechain string");
        event.setCellId("cell id");

        // --------------------
        // Mock settings
        // --------------------
        MapMessage message = mock(MapMessage.class);
        doReturn(true).when(message).itemExists("RequestKey");
        doReturn(true).when(message).itemExists("EventId");
        doReturn(true).when(message).itemExists("RuleChain");
        doReturn(true).when(message).itemExists("External");
        doReturn(false).when(message).itemExists("Schema");
        doReturn(true).when(message).itemExists("Subject");
        doReturn(true).when(message).itemExists("Type");
        doReturn(true).when(message).itemExists("Object");
        doReturn(true).when(message).itemExists("Info");
        doReturn(true).when(message).itemExists("cellId");
        doReturn(event.getRequestKey()).when(message).getString("RequestKey");
        doReturn(event.getEventId()).when(message).getString("EventId");
        doReturn(event.getRuleChain()).when(message).getString("RuleChain");
        doReturn(event.getExternal()).when(message).getBoolean("External");
        doReturn(event.getSchema()).when(message).getString("Schema");
        doReturn(event.getSubject()).when(message).getString("Subject");
        doReturn(event.getType()).when(message).getString("Type");
        doReturn(event.getObject()).when(message).getString("Object");
        doReturn(event.getInfo()).when(message).getString("Info");
        doReturn(event.getCellId()).when(message).getString("cellId");

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventPublisher.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result.getRequestKey(), is(event.getRequestKey()));
        assertThat(result.getEventId(), is(event.getEventId()));
        assertThat(result.getRuleChain(), is(event.getRuleChain()));
        assertThat(result.getExternal(), is(event.getExternal()));
        assertThat(result.getSchema(), is(event.getSchema()));
        assertThat(result.getSubject(), is(event.getSubject()));
        assertThat(result.getType(), is(event.getType()));
        assertThat(result.getObject(), is(event.getObject()));
        assertThat(result.getInfo(), is(event.getInfo()));
        assertThat(result.getCellId(), is(event.getCellId()));
    }

}
