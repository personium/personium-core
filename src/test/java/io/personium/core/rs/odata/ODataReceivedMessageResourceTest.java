/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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
package io.personium.core.rs.odata;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.rs.cell.MessageResource;
import io.personium.test.categories.Unit;

/**
 * ODataReceivedMessageResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ODataReceivedMessageResource.class, AccessContext.class })
@Category({ Unit.class })
public class ODataReceivedMessageResourceTest {

    /** Target class of unit test. */
    private ODataReceivedMessageResource oDataReceivedMessageResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        oDataReceivedMessageResource = PowerMockito.spy(new ODataReceivedMessageResource(null, null, null));
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * Type is message.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_type_is_message() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_UNREAD);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is relation.add.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_requesttype_is_relation_add() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RELATION_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), null);
        requestObjectMap.put(RequestObject.P_CLASS_URL.getName(), "http://personium/Cell/__relation/__/relationName/");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "http://personium/ExtCell/");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is role.add.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_requesttype_is_role_add() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_ROLE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), null);
        requestObjectMap.put(RequestObject.P_CLASS_URL.getName(), "http://personium/Cell/__role/__/roleName/");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "http://personium/ExtCell/");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is rule.add.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_requesttype_is_rule_add() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_LOG);
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is rule.add.
     * RequestRule.Action is log.
     * RequestRule.Object is localbox.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_rule_add_object_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_LOG);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is rule.add.
     * RequestRule.Action is exec.
     * RequestRule.Object is localbox.
     * RequestRule.TargetUrl is localbox.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_rule_add_object_localbox_targeturl_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_EXEC);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "personium-localbox:/col/service");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is rule.add.
     * RequestRule.Action is relay.
     * RequestRule.Object is localbox.
     * RequestRule.TargetUrl is localunit.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_rule_add_action_relay_object_localbox_service_localunit()
            throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_RELAY);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "personium-localunit:/cell/box/col/service");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Normal test.
     * RequestType is rule.remove.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Normal_requesttype_is_rule_remove() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_REMOVE);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * Type is message.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_type_is_message() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_READ);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is relation.add.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_requesttype_is_relation_add() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_APPROVED);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RELATION_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), null);
        requestObjectMap.put(RequestObject.P_CLASS_URL.getName(), "http://personium/Cell/__relation/__/relationName/");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "http://personium/ExtCell/");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is role.add.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_requesttype_is_role_add() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_REJECTED);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_ROLE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), null);
        requestObjectMap.put(RequestObject.P_CLASS_URL.getName(), "http://personium/Cell/__role/__/roleName/");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "http://personium/ExtCell/");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Status is not none.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__status_not_none() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_REJECTED);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_LOG);
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Action is null.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__action_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), null);
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                oDataReceivedMessageResource.concatRequestObjectPropertyName(Rule.P_ACTION.getName()));

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Action is exec.
     * Object is localcell.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__object_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_EXEC);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "personium-localbox:/col/entity");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                oDataReceivedMessageResource.concatRequestObjectPropertyName(Rule.P_OBJECT.getName()));

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Action is exec.
     * Object is localbox.
     * TargetUrl is localcell.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__object_localbox_targeturl_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_EXEC);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "personium-localcell:/box/col/entity");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                oDataReceivedMessageResource.concatRequestObjectPropertyName(Rule.P_TARGETURL.getName()));

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Action is exec.
     * Object is localbox.
     * TargetUrl is null.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__object_localbox_targeturl_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_EXEC);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), null);
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                oDataReceivedMessageResource.concatRequestObjectPropertyName(Rule.P_TARGETURL.getName()));

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.add.
     * Action is relay.
     * Object is localbox.
     * TargetUrl is invalid.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_add__action_relay_object_localbox_targeturl_invalid()
            throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_NONE);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_ADD);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectMap.put(Rule.P_ACTION.getName(), Rule.ACTION_RELAY);
        requestObjectMap.put(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity");
        requestObjectMap.put(RequestObject.P_TARGET_URL.getName(), "/personium/cell/box/col/service");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                oDataReceivedMessageResource.concatRequestObjectPropertyName(Rule.P_TARGETURL.getName()));

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedMessage().
     * Error test.
     * RequestType is rule.remove.
     * Status not none.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedMessage_Error_rule_remove_action_status_not_none() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001");
        props.put(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        props.put(ReceivedMessage.P_BODY.getName(), "body");
        props.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        props.put(ReceivedMessage.P_STATUS.getName(), ReceivedMessage.STATUS_READ);

        List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();
        Map<String, String> requestObjectMap = new HashMap<String, String>();
        requestObjectMap.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RULE_REMOVE);
        requestObjectMap.put(RequestObject.P_NAME.getName(), "ruleName");
        requestObjectPropMapList.add(requestObjectMap);

        doReturn(props).when(oDataReceivedMessageResource).getPropMap();
        MessageResource messageResource = new MessageResource(null, null);
        doReturn(messageResource).when(oDataReceivedMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataReceivedMessageResource,
                "validateReceivedBoxBoundSchema", messageResource, "http://personium/schema001");
        doNothing().when(oDataReceivedMessageResource).validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        doReturn(requestObjectPropMapList).when(oDataReceivedMessageResource).getRequestObjectPropMapList();

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                ReceivedMessage.P_STATUS.getName());

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedMessage");
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReceivedBoxBoundSchema().
     * Schema is not empty.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedBoxBoundSchema_Normal_schema_is_not_empty() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        String schema = "http://personium/schema001";

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = new Box(null, null, null, null, 0L);
        CellEsImpl mockCell = mock(CellEsImpl.class);
        doReturn(mockBox).when(mockCell).getBoxForSchema(schema);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);
        doReturn(mockCell).when(mockAccessContext).getCell();
        doReturn(mockAccessContext).when(messageResource).getAccessContext();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedBoxBoundSchema",
                MessageResource.class, String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource, messageResource, schema);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(mockCell, times(1)).getBoxForSchema(schema);
    }

    /**
     * Test validateReceivedBoxBoundSchema().
     * Schema is empty.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedBoxBoundSchema_Normal_schema_is_empty() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        String schema = "";

        // --------------------
        // Mock settings
        // --------------------
        CellEsImpl mockCell = mock(CellEsImpl.class);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
     // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedBoxBoundSchema",
                MessageResource.class, String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(oDataReceivedMessageResource, messageResource, schema);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(mockCell, never()).getBoxForSchema(schema);
    }

    /**
     * Test validateReceivedBoxBoundSchema().
     * Error test.
     * Box is null.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateReceivedBoxBoundSchema_Error_box_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        String schema = "http://personium/schema001";

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = null;
        CellEsImpl mockCell = mock(CellEsImpl.class);
        doReturn(mockBox).when(mockCell).getBoxForSchema(schema);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);
        doReturn(mockCell).when(mockAccessContext).getCell();
        doReturn(mockAccessContext).when(messageResource).getAccessContext();

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataReceivedMessageResource.class.getDeclaredMethod("validateReceivedBoxBoundSchema",
                MessageResource.class, String.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataReceivedMessageResource, messageResource, schema);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage
                    .BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }
}
