/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.rule.ActionInfo;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for ExecAction.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PersoniumUnitConfig.class })
public class ExecActionTest {

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
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
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
    }

    /**
     * Test createEvent().
     * Normal test.
     * schema is null
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
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
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
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
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
    }

}
