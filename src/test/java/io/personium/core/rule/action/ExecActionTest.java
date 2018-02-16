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
import static org.mockito.Mockito.mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.json.simple.JSONObject;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
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
        String enginePort = "8080";
        String enginePath = "personium-engine";
        String cellUrl = "http://personium/cell/";

        // --------------------
        // Expected result
        // --------------------
        String expected = String.format("http://%s:%s/%s/%s/%s/service/%s",
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
        String requestUrl = "http://localhost:8080/personium-engine/cell/box/service/svc";

        // --------------------
        // Expected result
        // --------------------

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
        HttpPost req = new HttpPost(requestUrl);
        action.setHeaders(req, null);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(req.getLastHeader("X-Baseurl"));
        assertNull(req.getLastHeader("X-Request-Uri"));
        assertNull(req.getLastHeader("X-Personium-Fs-Path"));
        assertNull(req.getLastHeader("X-Personium-Fs-Routing-Id"));
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

        // --------------------
        // Expected result
        // --------------------

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(cell, ai);
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

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
        JSONObject json = new JSONObject();
        action.addEvents(json);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(json.isEmpty(), is(true));
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
        ActionInfo ai = new ActionInfo(null, service, null, null);
        ExecAction action = new ExecAction(null, ai);
        JSONObject json = null;
        action.addEvents(json);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(json);
    }

}
