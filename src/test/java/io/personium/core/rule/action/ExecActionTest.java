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
package io.personium.core.rule.action;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.json.simple.JSONObject;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
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
        String cellName = "cell";
        String boxName = "box";
        String svcName = "service";
        String engineHost = "personium.engine";
        int enginePort = 8080;
        String enginePath = "personium-engine";
        String cellUrl = "http://personium/cell/";

        // --------------------
        // Expected result
        // --------------------
        String expected = String.format("http://%s:%d/%s/%s/%s/service/%s",
                engineHost, enginePort, enginePath, cellName, boxName, svcName);

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(engineHost).when(PersoniumUnitConfig.class, "getEngineHost");
        PowerMockito.doReturn(enginePort).when(PersoniumUnitConfig.class, "getEnginePort");
        PowerMockito.doReturn(enginePath).when(PersoniumUnitConfig.class, "getEnginePath");
        Cell cell = mock(Cell.class);
        PowerMockito.doReturn(cellUrl).when(cell).getUrl();
        PowerMockito.doReturn(cellName).when(cell).getName();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(cell, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getRequestUrl().
     * Normal test.
     * service is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getRequestUrl_Normal_service_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String service = null;
        String cellUrl = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        PowerMockito.doReturn(cellUrl).when(cell).getUrl();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(cell, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
    }

    /**
     * Test getRequestUrl().
     * Normal test.
     * service is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getRequestUrl_Normal_service_is_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String service = "/personium/cell";
        String cellUrl = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        PowerMockito.doReturn(cellUrl).when(cell).getUrl();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(cell, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
    }

    /**
     * Test getRequestUrl().
     * Normal test.
     * service is incorrect.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getRequestUrl_Normal_service_is_incorrect() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/svc";
        String cellUrl = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        PowerMockito.doReturn(cellUrl).when(cell).getUrl();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(cell, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
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
        JSONObject json = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(json.size(), is(size));
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
        JSONObject json = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(json.size(), is(size));
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
        JSONObject json = action.createEvent(event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(json.size(), is(size));
    }

}
