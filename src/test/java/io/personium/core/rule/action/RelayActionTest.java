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

import org.apache.http.client.methods.HttpPost;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
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
 * Unit Test class for RelayAction.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PersoniumUnitConfig.class })
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
        String cellName = "cell";
        String engineHost = "personium.engine";
        String enginePort = "8080";
        String enginePath = "personium-engine";

        // --------------------
        // Expected result
        // --------------------
        String expected = String.format("http://%s:%s/%s/%s/__/system/relay",
                engineHost, enginePort, enginePath, cellName);

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(cellName).when(cell).getName();
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(engineHost).when(PersoniumUnitConfig.class, "getEngineHost");
        PowerMockito.doReturn(enginePort).when(PersoniumUnitConfig.class, "getEnginePort");
        PowerMockito.doReturn(enginePath).when(PersoniumUnitConfig.class, "getEnginePath");

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(cell, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getRequestUrl().
     * Normal test.
     * cell is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getRequestUrl_Normal_cell_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";
        String engineHost = "personium.engine";
        String enginePort = "8080";
        String enginePath = "personium-engine";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(engineHost).when(PersoniumUnitConfig.class, "getEngineHost");
        PowerMockito.doReturn(enginePort).when(PersoniumUnitConfig.class, "getEnginePort");
        PowerMockito.doReturn(enginePath).when(PersoniumUnitConfig.class, "getEnginePath");

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        String result = action.getRequestUrl();

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
    }

    /**
     * Test setHeaders().
     * Normal test.
     */
    @Test
    public void setHeaders_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";
        String cellName = "cell";
        String unitUrl = "http://personium/";
        String cellUrl = unitUrl + cellName + "/";
        String requestUrl = "http://localhost:8080/personium-engine/cell/__/system/relay";

        // --------------------
        // Expected result
        // --------------------
        String xBaseurl = unitUrl;
        String xRequestUri = cellUrl + "__/relay";
        String xPersoniumBoxSchema = "http://personium/appcell/";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(unitUrl).when(cell).getUnitUrl();
        doReturn(cellUrl).when(cell).getUrl();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(cell, ai);
        HttpPost req = new HttpPost(requestUrl);
        PersoniumEvent event = new PersoniumEvent(true, xPersoniumBoxSchema,
                null, null, null, null, null, null, null, null, null);
        action.setHeaders(req, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(req.getLastHeader("X-Baseurl").getValue(), is(xBaseurl));
        assertThat(req.getLastHeader("X-Request-Uri").getValue(), is(xRequestUri));
        assertThat(req.getLastHeader("X-Personium-Box-Schema").getValue(), is(xPersoniumBoxSchema));
    }

    /**
     * Test setHeaders().
     * Normal test.
     * cell is null.
     */
    @Test
    public void setHeaders_Normal_cell_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";
        String requestUrl = "http://localhost:8080/personium-engine/cell/__/system/relay";

        // --------------------
        // Expected result
        // --------------------

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        HttpPost req = new HttpPost(requestUrl);
        action.setHeaders(req, null);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(req.getLastHeader("X-Baseurl"));
        assertNull(req.getLastHeader("X-Request-Uri"));
        assertNull(req.getLastHeader("X-Personium-Box-Schema"));
    }

    /**
     * Test setHeaders().
     * Normal test.
     * req is null.
     */
    @Test
    public void setHeaders_Normal_req_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";
        String cellName = "cell";
        String unitUrl = "http://personium/";
        String cellUrl = unitUrl + cellName + "/";
        String requestUrl = "http://localhost:8080/personium-engine/cell/__/system/relay";

        // --------------------
        // Expected result
        // --------------------

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(unitUrl).when(cell).getUnitUrl();
        doReturn(cellUrl).when(cell).getUrl();

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(cell, ai);
        HttpPost req = null;
        action.setHeaders(req, null);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(req);
    }

    /**
     * Test addEvents().
     * Normal test.
     */
    @Test
    public void addEvents_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = service;
        int size = 1;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo("relay", service, null, null);
        RelayAction action = new RelayAction(null, ai);
        JSONObject json = new JSONObject();
        action.addEvents(json);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(json.size(), is(size));
        assertThat(json.get("TargetUrl"), is(expected));
    }

    /**
     * Test addEvents().
     * Normal test.
     * json is null.
     */
    @Test
    public void addEvents_Normal_json_is_null() {
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
        JSONObject json = null;
        action.addEvents(json);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(json);
    }

}
