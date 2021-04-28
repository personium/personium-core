/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.core.rule.action;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.event.PersoniumEvent;
import io.personium.core.rule.ActionInfo;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for RelayEventAction.
 */
@Category({ Unit.class })
public class RelayEventActionTest {

    /**
     * Test getRequestUrl().
     * Normal test.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getRequestUrl_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/";

        // --------------------
        // Expected result
        // --------------------
        String expected = service + "__event";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay.event", service, null, null);
        RelayEventAction action = new RelayEventAction(null, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test createEvent().
     * Normal test.
     * external is false.
     */
    @Test
    public void createEvent_Normal_external_is_false() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/";
        PersoniumEvent event = new PersoniumEvent.Builder()
                .schema("schema")
                .subject("subject")
                .type("type")
                .object("object")
                .info("info")
                .requestKey("requestKey")
                .eventId("eventid")
                .ruleChain("rulechain")
                .via("via")
                .roles("roles")
                .build();

        // --------------------
        // Expected result
        // --------------------
        int size = 3;
        String type = "relay.type";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay.event", service, null, null);
        RelayEventAction action = new RelayEventAction(null, ai);
        Map<String, Object> map = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(map.size(), is(size));
        assertThat(map.get("Type"), is(type));
        assertThat(map.get("Object"), is(event.getObject().get()));
        assertThat(map.get("Info"), is(event.getInfo().get()));
    }

    /**
     * Test createEvent().
     * Normal test.
     * external is true.
     */
    @Test
    public void createEvent_Normal_external_is_true() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/";
        PersoniumEvent event = new PersoniumEvent.Builder()
                .external()
                .schema("schema")
                .subject("subject")
                .type("type")
                .object("object")
                .info("info")
                .requestKey("requestKey")
                .eventId("eventid")
                .ruleChain("rulechain")
                .via("via")
                .roles("roles")
                .build();

        // --------------------
        // Expected result
        // --------------------
        int size = 3;
        String type = "relay.ext.type";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay.event", service, null, null);
        RelayEventAction action = new RelayEventAction(null, ai);
        Map<String, Object> map = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(map.size(), is(size));
        assertThat(map.get("Type"), is(type));
        assertThat(map.get("Object"), is(event.getObject().get()));
        assertThat(map.get("Info"), is(event.getInfo().get()));
    }

}
