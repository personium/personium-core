/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.test.categories.Unit;

/**
 * MessageODataResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageODataResource.class, MessageResource.class, AccessContext.class, CellEsImpl.class})
@Category({ Unit.class })
public class MessageODataResourceTest {

    /** Target class of unit test. */
    private MessageODataResource messageODataResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        messageODataResource = spy(new MessageODataResource(null, null, null));
    }

    /**
     * Test validate().
     * EntitySetName is ReceivedMessage.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validate_Normal_entitySetName_is_ReceivedMessage() throws Exception {
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        messageODataResource = spy(new MessageODataResource(messageResource, null, ReceivedMessage.EDM_TYPE_NAME));
        // --------------------
        // Test method args
        // --------------------
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(ReceivedMessagePort.P_SCHEMA.getName(), "http://personium/schema001"));
        props.add(OProperties.string(ReceivedMessage.P_MULTICAST_TO.getName(), null));
        props.add(OProperties.string(ReceivedMessage.P_BODY.getName(), "body"));
        props.add(OProperties.string(ReceivedMessage.P_TYPE.getName(), "message"));
        props.add(OProperties.string(ReceivedMessage.P_STATUS.getName(), "unread"));
        props.add(OProperties.string(ReceivedMessage.P_REQUEST_RELATION.getName(), null));
        props.add(OProperties.string(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), null));
        messageODataResource.collectProperties(props);

        // --------------------
        // Mock settings
        // --------------------
        doNothing().when(messageODataResource).validateReceivedBoxBoundSchema(
                messageResource, "http://personium/schema001");
        PowerMockito.mockStatic(MessageODataResource.class);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateUriCsv",
                ReceivedMessage.P_MULTICAST_TO.getName(), null);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateBody", "body",
                Common.MAX_MESSAGE_BODY_LENGTH);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateStatus", "message", "unread");
        PowerMockito.doNothing().when(MessageODataResource.class, "validateReqRelation", "message", null, null);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        messageODataResource.validate(props);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataResource, times(1)).validateReceivedBoxBoundSchema(messageResource,
                "http://personium/schema001");
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), null);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateBody("body", Common.MAX_MESSAGE_BODY_LENGTH);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateStatus("message", "unread");
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateReqRelation("message", null, null);
    }

    /**
     * Test validate().
     * EntitySetName is SentMessage.
     * @throws Exception Unexpected error.
     */
    @Test
    public void validate_Normal_entitySetName_is_SentMessage() throws Exception {
        String baseUri = "http://personium/";
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        doReturn(baseUri).when(accessContext).getBaseUri();
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        doReturn(accessContext).when(messageResource).getAccessContext();

        messageODataResource = spy(new MessageODataResource(messageResource, null, SentMessage.EDM_TYPE_NAME));
        // --------------------
        // Test method args
        // --------------------
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(SentMessagePort.P_BOX_BOUND.getName(), "false"));
        props.add(OProperties.string(SentMessage.P_TO.getName(), "http://personium/user001"));
        props.add(OProperties.string(SentMessage.P_BODY.getName(), "body"));
        props.add(OProperties.string(SentMessage.P_TO_RELATION.getName(), null));
        props.add(OProperties.string(SentMessage.P_TYPE.getName(), "message"));
        props.add(OProperties.string(SentMessage.P_REQUEST_RELATION.getName(), null));
        props.add(OProperties.string(SentMessage.P_REQUEST_RELATION_TARGET.getName(), null));
        messageODataResource.collectProperties(props);

        // --------------------
        // Mock settings
        // --------------------
        doNothing().when(messageODataResource).validateSentBoxBoundSchema(messageResource, false);
        PowerMockito.mockStatic(MessageODataResource.class);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateUriCsv",
                SentMessage.P_TO.getName(), "http://personium/user001");
        PowerMockito.doNothing().when(MessageODataResource.class, "validateBody", "body",
                Common.MAX_MESSAGE_BODY_LENGTH);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateToAndToRelation",
                "http://personium/user001", null);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateToValue",
                "http://personium/user001", baseUri);
        PowerMockito.doNothing().when(MessageODataResource.class, "validateReqRelation", "message", null, null);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        messageODataResource.validate(props);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataResource, times(1)).validateSentBoxBoundSchema(messageResource, false);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(), "http://personium/user001");
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateBody("body", Common.MAX_MESSAGE_BODY_LENGTH);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateToAndToRelation("http://personium/user001", null);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateToValue("http://personium/user001", baseUri);
        PowerMockito.verifyStatic(times(1));
        MessageODataResource.validateReqRelation("message", null, null);
    }

    /**
     * Test validateReceivedBoxBoundSchema().
     * Schema is not empty.
     */
    @Test
    public void validateReceivedBoxBoundSchema_Normal_schema_is_not_empty() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        String schema = "http://personium/schema001";

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = new Box(null, null, null, null, 0L);
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);
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
        messageODataResource.validateReceivedBoxBoundSchema(messageResource, schema);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(mockCell, times(1)).getBoxForSchema(schema);
    }

    /**
     * Test validateReceivedBoxBoundSchema().
     * Schema is empty.
     */
    @Test
    public void validateReceivedBoxBoundSchema_Normal_schema_is_empty() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        String schema = "";

        // --------------------
        // Mock settings
        // --------------------
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        messageODataResource.validateReceivedBoxBoundSchema(messageResource, schema);

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
     */
    @Test
    public void validateReceivedBoxBoundSchema_Error_box_is_null() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        String schema = "http://personium/schema001";

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = null;
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);
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
        try {
            messageODataResource.validateReceivedBoxBoundSchema(messageResource, schema);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage
                    .BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateSentBoxBoundSchema().
     * boxboundFlag is true.
     */
    @Test
    public void validateSentBoxBoundSchema_Normal_boxboundFlag_is_true() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        boolean boxboundFlag = true;

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = new Box(null, null, null, null, 0L);
        String schema = "http://personium/schema001";
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);
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
        messageODataResource.validateSentBoxBoundSchema(messageResource, boxboundFlag);

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
     */
    @Test
    public void validateSentBoxBoundSchema_Normal_boxboundFlag_is_false() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        boolean boxboundFlag = false;

        // --------------------
        // Mock settings
        // --------------------
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);
        AccessContext mockAccessContext = PowerMockito.mock(AccessContext.class);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        messageODataResource.validateSentBoxBoundSchema(messageResource, boxboundFlag);

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
     */
    @Test
    public void validateSentBoxBoundSchema_Error_box_is_null() {
        // --------------------
        // Test method args
        // --------------------
        MessageResource messageResource = PowerMockito.mock(MessageResource.class);
        boolean boxboundFlag = true;

        // --------------------
        // Mock settings
        // --------------------
        Box mockBox = null;
        String schema = "http://personium/schema001";
        CellEsImpl mockCell = PowerMockito.mock(CellEsImpl.class);
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
        try {
            messageODataResource.validateSentBoxBoundSchema(messageResource, boxboundFlag);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.SentMessage
                    .BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateStatus().
     * Normal test.
     * Type is message.
     */
    @Test
    public void validateStatus_Normal_type_is_message() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_MESSAGE;
        String status = ReceivedMessage.STATUS_UNREAD;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateStatus().
     * Normal test.
     * Type is relationBuild.
     */
    @Test
    public void validateStatus_Normal_type_is_relation_build() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String status = ReceivedMessage.STATUS_NONE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateStatus().
     * Normal test.
     * Type is roleGrant.
     */
    @Test
    public void validateStatus_Normal_type_is_role_grant() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;
        String status = ReceivedMessage.STATUS_NONE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateStatus().
     * Error test.
     * Type is message.
     */
    @Test
    public void validateStatus_Error_type_is_message() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_MESSAGE;
        String status = ReceivedMessage.STATUS_READ;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateStatus().
     * Error test.
     * Type is relationBuild.
     */
    @Test
    public void validateStatus_Error_type_is_relation_build() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String status = ReceivedMessage.STATUS_APPROVED;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateStatus().
     * Error test.
     * Type is relationBreak.
     */
    @Test
    public void validateStatus_Error_type_is_relation_break() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String status = ReceivedMessage.STATUS_REJECTED;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateStatus().
     * Error test.
     * Type is roleGrant.
     */
    @Test
    public void validateStatus_Error_type_is_role_grant() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;
        String status = ReceivedMessage.STATUS_REJECTED;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateStatus().
     * Error test.
     * Type is roleRevoke.
     */
    @Test
    public void validateStatus_Error_type_is_role_revoke() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_REVOKE;
        String status = ReceivedMessage.STATUS_APPROVED;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateStatus(type, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_STATUS.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Normal test.
     * Type is relationBuild.
     */
    @Test
    public void validateReqRelation_Normal_type_is_relation_build() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = "http://personium/AppCell/__relation/__/-relationName_+:/";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateReqRelation().
     * Normal test.
     * Type is roleGrant.
     */
    @Test
    public void validateReqRelation_Normal_type_is_role_grant() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;
        String requestRelation = "http://personium/AppCell/__role/__/roleName-_/";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateReqRelation().
     * Normal test.
     * Type is message.
     */
    @Test
    public void validateReqRelation_Normal_type_is_message() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_MESSAGE;
        String requestRelation = null;
        String requestRelationTarget = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is relationBuild and RequestRelation is null.
     */
    @Test
    public void validateReqRelation_Error_type_is_relation_build_and_requestRelation_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = null;
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName()
                    + "," + ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is relationBreak and RequestRelationTarget is null.
     */
    @Test
    public void validateReqRelation_Error_type_is_relation_break_and_requestRelationTarget_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = "rerationName";
        String requestRelationTarget = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName()
                    + "," + ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is roleGrant and RequestRelation is null.
     */
    @Test
    public void validateReqRelation_Error_type_is_role_grant_and_requestRelation_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;
        String requestRelation = null;
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName()
                    + "," + ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is roleRevoke and RequestRelationTarget is null.
     */
    @Test
    public void validateReqRelation_Error_type_is_role_revoke_and_requestRelationTarget_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_REVOKE;
        String requestRelation = "roleName";
        String requestRelationTarget = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName()
                    + "," + ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is relationBuild and RequestRelation is invalid format.
     */
    @Test
    public void validateReqRelation_Error_type_is_relation_build_and_requestRelation_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = "http://personium/Cell/relationName";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is relationBreak and RequestRelation is invalid format.
     */
    @Test
    public void validateReqRelation_Error_type_is_relation_break_and_requestRelation_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = "_rerationName-+:";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is roleGrant and RequestRelation is invalid format.
     */
    @Test
    public void validateReqRelation_Error_type_is_role_grant_and_requestRelation_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;
        String requestRelation = "http://personium/Cell/roleName";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateReqRelation().
     * Error test.
     * Type is roleRevoke and RequestRelation is invalid format.
     */
    @Test
    public void validateReqRelation_Error_type_is_role_revoke_and_requestRelation_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_REVOKE;
        String requestRelation = "-roleName_";
        String requestRelationTarget = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }
}
