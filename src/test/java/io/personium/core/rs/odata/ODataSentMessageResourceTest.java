/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.OProperties;
import org.odata4j.edm.EdmSimpleType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.rs.cell.MessageResource;
import io.personium.test.categories.Unit;

/**
 * ODataSentMessageResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ODataSentMessageResource.class, AccessContext.class })
@Category({ Unit.class })
public class ODataSentMessageResourceTest {

    /** Target class of unit test. */
    private ODataSentMessageResource oDataSentMessageResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        oDataSentMessageResource = PowerMockito.spy(new ODataSentMessageResource(null, null, null, null, null));
    }

    /**
     * 送信先URLが最大送信許可数を超えている場合にPersoniumCoreExceptionが発生すること.
     * @throws Exception Unexpected error.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void 送信先URLが最大送信許可数を超えている場合にPersoniumCoreExceptionが発生すること() throws Exception {
        Method method = ODataSentMessageResource.class.getDeclaredMethod("checkMaxDestinationsSize", int.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, 1001);
    }

    /**
     * 送信先URLが最大送信許可数を超えていない場合にPersoniumCoreExceptionが発生しないこと.
     * @throws Exception Unexpected error.
     */
    @Test
    public final void 送信先URLが最大送信許可数を超えていない場合にPersoniumCoreExceptionが発生しないこと() throws Exception {
        Method method = ODataSentMessageResource.class.getDeclaredMethod("checkMaxDestinationsSize", int.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, 1000);
    }

    /**
     * Test validateSentMessage().
     * Normal test.
     * Type is message.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateSentMessage_Normal_type_is_message() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // Nothing.

        // --------------------
        // Mock settings
        // --------------------
        Map<String, String> props = new HashMap<String, String>();
        props.put(SentMessagePort.P_BOX_BOUND.getName(), "false");
        props.put(SentMessage.P_TO.getName(), "http://personium/user001");
        props.put(SentMessage.P_BODY.getName(), "body");
        props.put(SentMessage.P_TO_RELATION.getName(), null);
        props.put(SentMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);

        doReturn(props).when(oDataSentMessageResource).getPropMap();
        MessageResource messageResource = mock(MessageResource.class);
        doReturn(messageResource).when(oDataSentMessageResource).getMessageResource();
        PowerMockito.doNothing().when(oDataSentMessageResource,
                "validateSentBoxBoundSchema", messageResource, false);
        doNothing().when(oDataSentMessageResource).validateUriCsv(
                SentMessage.P_TO.getName(), "http://personium/user001");
        PowerMockito.doNothing().when(oDataSentMessageResource,
                "validateToAndToRelation", "http://personium/user001", null);

        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        doReturn("http://personium/").when(accessContext).getBaseUri();
        doReturn(accessContext).when(messageResource).getAccessContext();
        PowerMockito.doNothing().when(oDataSentMessageResource,
                "validateToValue", "http://personium/user001", "http://personium/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateSentMessage");
        method.setAccessible(true);
        // Run method
        method.invoke(oDataSentMessageResource);

        // --------------------
        // Confirm result
        // --------------------
        // Nothing.
    }

     // TODO add tests with RequestRule property on SentMessage

    /**
     * Test validateSentBoxBoundSchema().
     * boxboundFlag is true.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateSentBoxBoundSchema_Normal_boxboundFlag_is_true() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        boolean boxboundFlag = true;

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = new Box(null, null, null, null, 0L);
        String schema = "http://personium/schema001";
        CellEsImpl mockCell = mock(CellEsImpl.class);
        doReturn(mockBox).when(mockCell).getBoxForSchema(schema);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);
        doReturn(schema).when(mockAccessContext).getSchema();
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
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateSentBoxBoundSchema",
                MessageResource.class, boolean.class);
        method.setAccessible(true);
        // Run method
        method.invoke(oDataSentMessageResource, messageResource, boxboundFlag);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(mockAccessContext, times(1)).getSchema();
        verify(mockCell, times(1)).getBoxForSchema(schema);
    }

    /**
     * Test validateSentBoxBoundSchema().
     * boxboundFlag is false.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateSentBoxBoundSchema_Normal_boxboundFlag_is_false() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        boolean boxboundFlag = false;

        // --------------------
        // Mock settings
        // --------------------
        CellEsImpl mockCell = mock(CellEsImpl.class);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateSentBoxBoundSchema",
                MessageResource.class, boolean.class);
        method.setAccessible(true);
        // Run method
        method.invoke(oDataSentMessageResource, messageResource, boxboundFlag);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(mockAccessContext, never()).getSchema();
        verify(mockCell, never()).getBoxForSchema(anyString());
    }

    /**
     * Test validateSentBoxBoundSchema().
     * Error test.
     * Box is null.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validateSentBoxBoundSchema_Error_box_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = mock(MessageResource.class);
        boolean boxboundFlag = true;

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = null;
        String schema = "http://personium/schema001";
        CellEsImpl mockCell = mock(CellEsImpl.class);
        doReturn(mockBox).when(mockCell).getBoxForSchema(schema);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);
        doReturn(schema).when(mockAccessContext).getSchema();
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
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateSentBoxBoundSchema",
                MessageResource.class, boolean.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(oDataSentMessageResource, messageResource, boxboundFlag);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            PersoniumCoreException expected = PersoniumCoreException.SentMessage
                    .BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * ToもToRelationも存在しない場合にPersoniumCoreExceptionが発生すること.
     * @throws Exception Unexpected error.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToもToRelationも存在しない場合にPersoniumCoreExceptionが発生すること() throws Exception {
        String to = (String) OProperties.null_(SentMessage.P_TO.getName(),
                EdmSimpleType.STRING).getValue();
        String toRelation = (String) OProperties.null_(SentMessage.P_TO_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateToAndToRelation",
                String.class, String.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, to, toRelation);
    }

    /**
     * ToがあってToRelationがない場合にPersoniumCoreExceptionが発生しないこと.
     * @throws Exception Unexpected error.
     */
    @Test
    public final void ToがあってToRelationがない場合にPersoniumCoreExceptionが発生しないこと() throws Exception {
        String to = "http://example.com/toAddress/";
        String toRelation = (String) OProperties.null_(SentMessage.P_TO_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateToAndToRelation",
                String.class, String.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, to, toRelation);
    }

    /**
     * ToがなくてToRelationがある場合にPersoniumCoreExceptionが発生しないこと.
     * @throws Exception Unexpected error.
     */
    @Test
    public final void ToがなくてToRelationがある場合にPersoniumCoreExceptionが発生しないこと() throws Exception {
        String to = (String) OProperties.null_(SentMessage.P_TO.getName(),
                EdmSimpleType.STRING).getValue();
        String toRelation = "http://example.com/toRelation";
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateToAndToRelation",
                String.class, String.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, to, toRelation);
    }

    /**
     * ToとToRelationが両方ある場合にPersoniumCoreExceptionが発生しないこと.
     * @throws Exception Unexpected error.
     */
    @Test
    public final void ToとToRelationが両方ある場合にPersoniumCoreExceptionが発生しないこと() throws Exception {
        String to = "http://example.com/toAddress/";
        String toRelation = "http://example.com/toRelation";
        Method method = ODataSentMessageResource.class.getDeclaredMethod("validateToAndToRelation",
                String.class, String.class);
        method.setAccessible(true);
        method.invoke(oDataSentMessageResource, to, toRelation);
    }
}
