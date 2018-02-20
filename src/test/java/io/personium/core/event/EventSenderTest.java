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
 * Unit Test class for EventSender.
 */
@Category({ Unit.class })
public class EventSenderTest {

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
        PersoniumEvent event = EventSender.convertToEvent(message);

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
                "schema string", "subject string", "type string", "object string", "info string",
                "requestkey string", "eventid string", "rulechain string", "via string", "roles string");
        event.setCellId("cell id");
        event.setTime();

        // --------------------
        // Mock settings
        // --------------------
        MapMessage message = mock(MapMessage.class);
        doReturn(true).when(message).itemExists(EventSender.KEY_REQUESTKEY);
        doReturn(true).when(message).itemExists(EventSender.KEY_EVENTID);
        doReturn(true).when(message).itemExists(EventSender.KEY_RULECHAIN);
        doReturn(true).when(message).itemExists(EventSender.KEY_VIA);
        doReturn(true).when(message).itemExists(EventSender.KEY_ROLES);
        doReturn(true).when(message).itemExists(EventSender.KEY_EXTERNAL);
        doReturn(true).when(message).itemExists(EventSender.KEY_SCHEMA);
        doReturn(true).when(message).itemExists(EventSender.KEY_SUBJECT);
        doReturn(true).when(message).itemExists(EventSender.KEY_TYPE);
        doReturn(true).when(message).itemExists(EventSender.KEY_OBJECT);
        doReturn(true).when(message).itemExists(EventSender.KEY_INFO);
        doReturn(true).when(message).itemExists(EventSender.KEY_CELLID);
        doReturn(true).when(message).itemExists(EventSender.KEY_TIME);
        doReturn(event.getRequestKey()).when(message).getString(EventSender.KEY_REQUESTKEY);
        doReturn(event.getEventId()).when(message).getString(EventSender.KEY_EVENTID);
        doReturn(event.getRuleChain()).when(message).getString(EventSender.KEY_RULECHAIN);
        doReturn(event.getVia()).when(message).getString(EventSender.KEY_VIA);
        doReturn(event.getRoles()).when(message).getString(EventSender.KEY_ROLES);
        doReturn(event.getExternal()).when(message).getBoolean(EventSender.KEY_EXTERNAL);
        doReturn(event.getSchema()).when(message).getString(EventSender.KEY_SCHEMA);
        doReturn(event.getSubject()).when(message).getString(EventSender.KEY_SUBJECT);
        doReturn(event.getType()).when(message).getString(EventSender.KEY_TYPE);
        doReturn(event.getObject()).when(message).getString(EventSender.KEY_OBJECT);
        doReturn(event.getInfo()).when(message).getString(EventSender.KEY_INFO);
        doReturn(event.getCellId()).when(message).getString(EventSender.KEY_CELLID);
        doReturn(event.getTime()).when(message).getLong(EventSender.KEY_TIME);

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventSender.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result.getRequestKey(), is(event.getRequestKey()));
        assertThat(result.getEventId(), is(event.getEventId()));
        assertThat(result.getRuleChain(), is(event.getRuleChain()));
        assertThat(result.getVia(), is(event.getVia()));
        assertThat(result.getRoles(), is(event.getRoles()));
        assertThat(result.getExternal(), is(event.getExternal()));
        assertThat(result.getSchema(), is(event.getSchema()));
        assertThat(result.getSubject(), is(event.getSubject()));
        assertThat(result.getType(), is(event.getType()));
        assertThat(result.getObject(), is(event.getObject()));
        assertThat(result.getInfo(), is(event.getInfo()));
        assertThat(result.getCellId(), is(event.getCellId()));
        assertThat(result.getTime(), is(event.getTime()));
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
        doThrow(new JMSException("dummy")).when(message).itemExists(EventSender.KEY_REQUESTKEY);

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventSender.convertToEvent(message);

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
                null, null, "type string", "object string", "info string",
                "requestkey string", "eventid string", "rulechain string", "via string", "roles string");
        event.setCellId("cell id");
        event.setTime();

        // --------------------
        // Mock settings
        // --------------------
        MapMessage message = mock(MapMessage.class);
        doReturn(true).when(message).itemExists(EventSender.KEY_REQUESTKEY);
        doReturn(true).when(message).itemExists(EventSender.KEY_EVENTID);
        doReturn(true).when(message).itemExists(EventSender.KEY_RULECHAIN);
        doReturn(true).when(message).itemExists(EventSender.KEY_VIA);
        doReturn(true).when(message).itemExists(EventSender.KEY_ROLES);
        doReturn(true).when(message).itemExists(EventSender.KEY_EXTERNAL);
        doReturn(false).when(message).itemExists(EventSender.KEY_SCHEMA);
        doReturn(true).when(message).itemExists(EventSender.KEY_SUBJECT);
        doReturn(true).when(message).itemExists(EventSender.KEY_TYPE);
        doReturn(true).when(message).itemExists(EventSender.KEY_OBJECT);
        doReturn(true).when(message).itemExists(EventSender.KEY_INFO);
        doReturn(true).when(message).itemExists(EventSender.KEY_CELLID);
        doReturn(true).when(message).itemExists(EventSender.KEY_TIME);
        doReturn(event.getRequestKey()).when(message).getString(EventSender.KEY_REQUESTKEY);
        doReturn(event.getEventId()).when(message).getString(EventSender.KEY_EVENTID);
        doReturn(event.getRuleChain()).when(message).getString(EventSender.KEY_RULECHAIN);
        doReturn(event.getVia()).when(message).getString(EventSender.KEY_VIA);
        doReturn(event.getRoles()).when(message).getString(EventSender.KEY_ROLES);
        doReturn(event.getExternal()).when(message).getBoolean(EventSender.KEY_EXTERNAL);
        doReturn(event.getSchema()).when(message).getString(EventSender.KEY_SCHEMA);
        doReturn(event.getSubject()).when(message).getString(EventSender.KEY_SUBJECT);
        doReturn(event.getType()).when(message).getString(EventSender.KEY_TYPE);
        doReturn(event.getObject()).when(message).getString(EventSender.KEY_OBJECT);
        doReturn(event.getInfo()).when(message).getString(EventSender.KEY_INFO);
        doReturn(event.getCellId()).when(message).getString(EventSender.KEY_CELLID);
        doReturn(event.getTime()).when(message).getLong(EventSender.KEY_TIME);

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent result = EventSender.convertToEvent(message);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result.getRequestKey(), is(event.getRequestKey()));
        assertThat(result.getEventId(), is(event.getEventId()));
        assertThat(result.getRuleChain(), is(event.getRuleChain()));
        assertThat(result.getVia(), is(event.getVia()));
        assertThat(result.getRoles(), is(event.getRoles()));
        assertThat(result.getExternal(), is(event.getExternal()));
        assertThat(result.getSchema(), is(event.getSchema()));
        assertThat(result.getSubject(), is(event.getSubject()));
        assertThat(result.getType(), is(event.getType()));
        assertThat(result.getObject(), is(event.getObject()));
        assertThat(result.getInfo(), is(event.getInfo()));
        assertThat(result.getCellId(), is(event.getCellId()));
        assertThat(result.getTime(), is(event.getTime()));
    }

}
