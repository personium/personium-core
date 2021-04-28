/**
 * Personium
 * Copyright 2017-2021 Personium Project Authors
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
 * Unit Test class for RelayAction.
 */
@Category({ Unit.class })
public class RelayActionTest {

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
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = service;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test createEvent().
     * Normal test.
     */
    @Test
    public void createEvent_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        int size = 6;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
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
        Map<String, Object> map = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(map.size(), is(size));
        assertThat(map.get("External"), is(event.getExternal()));
        assertThat(map.get("Schema"), is(event.getSchema().get()));
        assertThat(map.get("Subject"), is(event.getSubject().get()));
        assertThat(map.get("Type"), is(event.getType().get()));
        assertThat(map.get("Object"), is(event.getObject().get()));
        assertThat(map.get("Info"), is(event.getInfo().get()));
    }

    /**
     * Test createEvent().
     * Normal test.
     * schema is null.
     */
    @Test
    public void createEvent_Normal_schema_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        int size = 5;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        PersoniumEvent event = new PersoniumEvent.Builder()
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
        Map<String, Object> map = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(map.size(), is(size));
        assertThat(map.get("External"), is(event.getExternal()));
        assertThat(map.get("Subject"), is(event.getSubject().get()));
        assertThat(map.get("Type"), is(event.getType().get()));
        assertThat(map.get("Object"), is(event.getObject().get()));
        assertThat(map.get("Info"), is(event.getInfo().get()));
    }

    /**
     * Test createEvent().
     * Normal test.
     * subject is null.
     */
    @Test
    public void createEvent_Normal_subject_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        int size = 5;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        PersoniumEvent event = new PersoniumEvent.Builder()
                .schema("schema")
                .type("type")
                .object("object")
                .info("info")
                .requestKey("requestKey")
                .eventId("eventid")
                .ruleChain("rulechain")
                .via("via")
                .roles("roles")
                .build();
        Map<String, Object> map = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(map.size(), is(size));
        assertThat(map.get("External"), is(event.getExternal()));
        assertThat(map.get("Schema"), is(event.getSchema().get()));
        assertThat(map.get("Type"), is(event.getType().get()));
        assertThat(map.get("Object"), is(event.getObject().get()));
        assertThat(map.get("Info"), is(event.getInfo().get()));
    }

}
