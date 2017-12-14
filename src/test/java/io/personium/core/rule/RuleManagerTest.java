/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.rule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.PersoniumEvent;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for RuleManager.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ LoggerFactory.class })
public class RuleManagerTest {

    /**
     * Test match().
     * Normal test.
     * argument is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_argument_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = null;
        PersoniumEvent event = null;

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class, "getLogger", RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(false));
    }

    /**
     * Test match().
     * Normal test.
     * external of RuleInfo is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_external_of_ruleinfo_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        PersoniumEvent event = new PersoniumEvent();

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(false));
    }

    /**
     * Test match().
     * Normal test.
     * external of event is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_external_of_event_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.FALSE;
        PersoniumEvent event = new PersoniumEvent(null,
            null, null, null, null, null, null);

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(false));
    }

    /**
     * Test match().
     * Normal test.
     * external matches with false.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_external_matches_with_false() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.FALSE;
        PersoniumEvent event = new PersoniumEvent(Boolean.FALSE,
                "schema string", "subject", "type", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(true));
    }

    /**
     * Test match().
     * Normal test.
     * external matches with true.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_external_matches_with_true() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.TRUE;
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "schema", "subject", "type", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(true));
    }

    /**
     * Test match().
     * Normal test.
     * schema matches.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_schema_matches() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        RuleManager.BoxInfo bi = rman.new BoxInfo();
        bi.schema = "http://personium/dummyCell/";
        ri.external = Boolean.TRUE;
        ri.box = bi;
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "http://personium/dummyCell/", "subject", "type", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(true));
    }

    /**
     * Test match().
     * Normal test.
     * schema not matches.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_schema_not_matches() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        RuleManager.BoxInfo bi = rman.new BoxInfo();
        bi.schema = "http://personium/dummyCell/";
        ri.external = Boolean.TRUE;
        ri.box = bi;
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "http://personium/dummyCell/invalid/", "subject", "type", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(false));
    }

    /**
     * Test match().
     * Normal test.
     * subject
     */

    /**
     * Test match().
     * Normal test.
     * type matches.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_type_matches() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.TRUE;
        ri.type = "cellctl.Rule.create";
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "http://personium/dummyCell/", "subject", "cellctl.Rule.create", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(true));
    }

    /**
     * Test match().
     * Normal test.
     * type partial matches.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_type_partial_matches() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.TRUE;
        ri.type = "cellctl.Rule";
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "http://personium/dummyCell/", "subject", "cellctl.Rule.create", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(true));
    }

    /**
     * Test match().
     * Normal test.
     * type not matches.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void match_Normal_type_not_matches() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        RuleManager rman = RuleManager.getInstance();
        RuleManager.RuleInfo ri = rman.new RuleInfo();
        ri.external = Boolean.TRUE;
        ri.type = "cellctl.Rule.create.some";
        PersoniumEvent event = new PersoniumEvent(Boolean.TRUE,
                "http://personium/dummyCell/", "subject", "cellctl.Rule.create", "object", "info", "requestKey");

        // --------------------
        // Mock settings
        // --------------------
        Logger logger = PowerMockito.mock(Logger.class);
        PowerMockito.spy(LoggerFactory.class);
        PowerMockito.doReturn(logger).when(LoggerFactory.class);
        LoggerFactory.getLogger(RuleManager.class);
        PowerMockito.doNothing().when(logger).debug(anyString());

        // --------------------
        // Run method
        // --------------------
        Method match = RuleManager.class.getDeclaredMethod("match", RuleManager.RuleInfo.class, PersoniumEvent.class);
        match.setAccessible(true);
        boolean result = (boolean) match.invoke(rman, ri, event);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(false));
    }
}
