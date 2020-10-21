/**
 * Personium
 * Copyright 2017-2020 Personium Project Authors
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.slf4j.LoggerFactory;

import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for ActionFactory.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ LoggerFactory.class })
public class ActionFactoryTest {

    /**
     * Test createAction().
     * Normal test.
     * action is log.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void createActionl_Normal_action_is_log() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        ActionInfo ai = new ActionInfo("log");
        String owner = "owner";
        String cellId = "0123456789";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(owner).when(cell).getOwnerNormalized();
        doReturn(cellId).when(cell).getId();
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(null).when(LoggerFactory.class, "getLogger", "io.personium.core.rule.action");

        // --------------------
        // Run method
        // --------------------
        Action result = ActionFactory.createAction(cell, ai);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, instanceOf(LogAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is log.info.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void createActionl_Normal_action_is_log_info() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        ActionInfo ai = new ActionInfo("log.info");
        String owner = "owner";
        String cellId = "0123456789";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(owner).when(cell).getOwnerNormalized();
        doReturn(cellId).when(cell).getId();
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(null).when(LoggerFactory.class, "getLogger", "io.personium.core.rule.action");

        // --------------------
        // Run method
        // --------------------
        Action result = ActionFactory.createAction(cell, ai);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, instanceOf(LogAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is log.warn.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void createActionl_Normal_action_is_log_warn() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        ActionInfo ai = new ActionInfo("log.warn");
        String owner = "owner";
        String cellId = "0123456789";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(owner).when(cell).getOwnerNormalized();
        doReturn(cellId).when(cell).getId();
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(null).when(LoggerFactory.class, "getLogger", "io.personium.core.rule.action");

        // --------------------
        // Run method
        // --------------------
        Action result = ActionFactory.createAction(cell, ai);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, instanceOf(LogAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is log.error.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void createActionl_Normal_action_is_log_error() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        ActionInfo ai = new ActionInfo("log.error");
        String owner = "owner";
        String cellId = "0123456789";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(owner).when(cell).getOwnerNormalized();
        doReturn(cellId).when(cell).getId();
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(null).when(LoggerFactory.class, "getLogger", "io.personium.core.rule.action");

        // --------------------
        // Run method
        // --------------------
        Action result = ActionFactory.createAction(cell, ai);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, instanceOf(LogAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is log.debug.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void createActionl_Normal_action_is_log_debug() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        ActionInfo ai = new ActionInfo("log.debug");
        String owner = "owner";
        String cellId = "0123456789";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(owner).when(cell).getOwnerNormalized();
        doReturn(cellId).when(cell).getId();
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(null).when(LoggerFactory.class, "getLogger", "io.personium.core.rule.action");

        // --------------------
        // Run method
        // --------------------
        Action result = ActionFactory.createAction(cell, ai);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(result);
    }

    /**
     * Test createAction().
     * Normal test.
     * action is exec.
     */
    @Test
    public void createAction_Normal_action_is_exec() {
        ActionInfo ai = new ActionInfo("exec", "http://personium/cell/box/col/service", null, null);
        assertThat(ActionFactory.createAction(null, ai), instanceOf(ExecAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is relay.
     */
    @Test
    public void createAction_Normal_action_is_relay() {
        ActionInfo ai = new ActionInfo("relay", "http://personium/cell/box/col/service", null, null);
        assertThat(ActionFactory.createAction(null, ai), instanceOf(RelayAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is relay.event.
     */
    @Test
    public void createAction_Normal_action_is_relayevent() {
        ActionInfo ai = new ActionInfo("relay.event", "http://personium/cell/", null, null);
        assertThat(ActionFactory.createAction(null, ai), instanceOf(RelayEventAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is relay.data.
     */
    @Test
    public void createAction_Normal_action_is_relaydata() {
        ActionInfo ai = new ActionInfo("relay.data", "http://personium/cell/box/col/ent", null, null);
        assertThat(ActionFactory.createAction(null, ai), instanceOf(RelayDataAction.class));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is invalid.
     */
    @Test
    public void createAction_Normal_action_is_invalid() {
        ActionInfo ai = new ActionInfo("invalid", "http://personium/cell/box/col/service", null, null);
        assertNull(ActionFactory.createAction(null, ai));
    }

    /**
     * Test createAction().
     * Normal test.
     * action is null.
     */
    @Test
    public void createAction_Normal_action_is_null() {
        ActionInfo ai = new ActionInfo(null, "http://personium/cell/box/col/service", null, null);
        assertNull(ActionFactory.createAction(null, ai));
    }
}
