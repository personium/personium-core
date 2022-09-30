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
package io.personium.core.model.impl.es.odata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.lock.Lock;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * MessageODataProducer unit tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MessageODataProducer.class, Box.class, UriUtils.class })
@Category({ Unit.class })
public class MessageODataProducerTest {

    /** Target class of unit test. */
    private MessageODataProducer messageODataProducer;

    /**
     * Before.
     */
    @Before
    public void befor() {
        messageODataProducer = spy(new MessageODataProducer(new CellEsImpl(), null));
    }

    /**
     * Test isValidMessageStatus().
     * status is none.
     */
    @Test
    public void isValidMessageStatus_Normal_status_is_none() {
        assertFalse(messageODataProducer.isValidMessageStatus(
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * Test isValidMessageStatus().
     * status is read.
     */
    @Test
    public void isValidMessageStatus_Normal_status_is_read() {
        assertTrue(messageODataProducer.isValidMessageStatus(
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * Test isValidMessageStatus().
     * status is unread.
     */
    @Test
    public void isValidMessageStatus_Normal_status_is_unread() {
        assertTrue(messageODataProducer.isValidMessageStatus(
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * Test isValidMessageStatus().
     * status is rejected.
     */
    @Test
    public void isValidMessageStatus_Normal_status_is_rejected() {
        assertFalse(messageODataProducer.isValidMessageStatus(
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * Test isValidMessageStatus().
     * status is approved.
     */
    @Test
    public void isValidMessageStatus_Normal_status_is_approved() {
        assertFalse(messageODataProducer.isValidMessageStatus(
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * Test isValidCurrentStatus().
     * status is none
     */
    @Test
    public void isValidCurrentStatus_Normal_status_is_none() {
        assertTrue(messageODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * Test isValidCurrentStatus().
     * status is read.
     */
    @Test
    public void isValidCurrentStatus_Normal_status_is_read() {
        assertFalse(messageODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * Test isValidCurrentStatus().
     * status is unread.
     */
    @Test
    public void isValidCurrentStatus_Normal_status_is_unread() {
        assertFalse(messageODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * Test isValidCurrentStatus().
     * status is approved.
     */
    @Test
    public void isValidCurrentStatus_Normal_status_is_approved() {
        assertFalse(messageODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * Test isValidCurrentStatus().
     * status is rejected.
     */
    @Test
    public void isValidCurrentStatus_Normal_status_is_rejected() {
        assertFalse(messageODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * Test beforeCreate().
     * EntitySet is ReceivedMessage.
     * staticFields BoxName is not null.
     */
    @Test
    public void beforeCreate_Normal_receivedmessage_boxname_not_null() {
        String pBoxName = Common.P_BOX_NAME.getName();
        // --------------------
        // Test method args
        // --------------------
        String entitySetName = ReceivedMessage.EDM_TYPE_NAME;
        OEntity oEntity = null;
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        Box mockBox = PowerMockito.mock(Box.class);
        doReturn(mockBox).when(mockCell).getBoxForName("box2");
        doReturn("id1").when(mockBox).getId();

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStatic = new HashMap<String, Object>();
        expectedStatic.put("dummy", "dummy2");
        Map<String, Object> expectedLink = new HashMap<String, Object>();
        expectedLink.put("dummy", "dummy3");
        expectedLink.put("Box", "id1");

        // --------------------
        // Run method
        // --------------------
        messageODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

        // --------------------
        // Confirm result
        // --------------------
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
        // Confirm function call
        verify(mockCell, times(1)).getBoxForName("box2");
        verify(mockBox, times(1)).getId();
    }

    /**
     * Test beforeCreate().
     * EntitySet is ReceivedMessage.
     * staticFields BoxName is null.
     */
    @Test
    public void beforeCreate_Normal_receivedmessage_boxname_is_null() {
        String pBoxName = Common.P_BOX_NAME.getName();
        // --------------------
        // Test method args
        // --------------------
        String entitySetName = ReceivedMessage.EDM_TYPE_NAME;
        OEntity oEntity = null;
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        Box mockBox = PowerMockito.mock(Box.class);
        doReturn(mockBox).when(mockCell).getBoxForName("box2");
        doReturn("id1").when(mockBox).getId();

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStatic = new HashMap<String, Object>();
        expectedStatic.put("dummy", "dummy2");
        Map<String, Object> expectedLink = new HashMap<String, Object>();
        expectedLink.put("dummy", "dummy3");

        // --------------------
        // Run method
        // --------------------
        messageODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

        // --------------------
        // Confirm result
        // --------------------
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
        // Confirm function call
        verify(mockCell, never()).getBoxForName(anyString());
        verify(mockBox, never()).getId();
    }

    /**
     * Test beforeCreate().
     * EntitySet is SentMessage.
     * staticFields BoxName is not null.
     */
    @Test
    public void beforeCreate_Normal_sentmessage_boxname_not_null() {
        String pBoxName = Common.P_BOX_NAME.getName();
        // --------------------
        // Test method args
        // --------------------
        String entitySetName = SentMessage.EDM_TYPE_NAME;
        OEntity oEntity = null;
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        Box mockBox = PowerMockito.mock(Box.class);
        doReturn(mockBox).when(mockCell).getBoxForName("box2");
        doReturn("id1").when(mockBox).getId();

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStatic = new HashMap<String, Object>();
        expectedStatic.put("dummy", "dummy2");
        Map<String, Object> expectedLink = new HashMap<String, Object>();
        expectedLink.put("dummy", "dummy3");
        expectedLink.put("Box", "id1");

        // --------------------
        // Run method
        // --------------------
        messageODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

        // --------------------
        // Confirm result
        // --------------------
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
        // Confirm function call
        verify(mockCell, times(1)).getBoxForName("box2");
        verify(mockBox, times(1)).getId();
    }

    /**
     * Test beforeCreate().
     * EntitySet is SentMessage.
     * staticFields BoxName is null.
     */
    @Test
    public void beforeCreate_Normal_sentmessage_boxname_is_null() {
        String pBoxName = Common.P_BOX_NAME.getName();
        // --------------------
        // Test method args
        // --------------------
        String entitySetName = SentMessage.EDM_TYPE_NAME;
        OEntity oEntity = null;
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        Box mockBox = PowerMockito.mock(Box.class);
        doReturn(mockBox).when(mockCell).getBoxForName("box2");
        doReturn("id1").when(mockBox).getId();

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStatic = new HashMap<String, Object>();
        expectedStatic.put("dummy", "dummy2");
        Map<String, Object> expectedLink = new HashMap<String, Object>();
        expectedLink.put("dummy", "dummy3");

        // --------------------
        // Run method
        // --------------------
        messageODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

        // --------------------
        // Confirm result
        // --------------------
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
        // Confirm function call
        verify(mockCell, never()).getBoxForName(anyString());
        verify(mockBox, never()).getId();
    }

    /**
     * Test beforeCreate().
     * EntitySet is not ReceivedMessage and SentMessage.
     */
    @Test
    public void beforeCreate_Normal_message_is_not_receive_and_sent() {
        String pBoxName = Common.P_BOX_NAME.getName();
        // --------------------
        // Test method args
        // --------------------
        String entitySetName = Relation.EDM_TYPE_NAME;
        OEntity oEntity = null;
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStatic = new HashMap<String, Object>();
        expectedStatic.put(pBoxName, "box2");
        expectedStatic.put("dummy", "dummy2");
        Map<String, Object> expectedLink = new HashMap<String, Object>();
        expectedLink.put("dummy", "dummy3");

        // --------------------
        // Run method
        // --------------------
        messageODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

        // --------------------
        // Confirm result
        // --------------------
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
    }

    /**
     * Test changeStatusAndUpdateRelation().
     * Normal test.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("unchecked")
    @Test
    public void changeStatusAndUpdateRelation_Normal() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EdmEntitySet entitySet = EdmEntitySet.newBuilder().setName("dummyName").build();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("dummy", "dummy");
        OEntityKey originalKey = OEntityKey.create(values);
        String status = "status";

        // --------------------
        // Mock settings
        // --------------------
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).release();
        doReturn(mockLock).when(messageODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(messageODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        List<Map<String, String>> mockRequestObjects = new ArrayList<Map<String, String>>();
        Map<String, String> mockRequestObject = new HashMap<String, String>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");
        mockConvertedStaticFields.put(ReceivedMessage.P_ID.getName(), "dummyId");
        mockConvertedStaticFields.put(ReceivedMessage.P_REQUEST_OBJECTS.getName(), mockRequestObjects);
        mockRequestObjects.add(mockRequestObject);
        mockRequestObject.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RELATION_ADD);

        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(messageODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(messageODataProducer).isValidMessageStatus(status);
        doReturn(true).when(messageODataProducer).isValidCurrentStatus("dummyStatus");

        PowerMockito.doNothing().when(messageODataProducer,
                "updateRelation", "dummyId", "dummyBoxName", mockRequestObject);
        PowerMockito.doNothing().when(messageODataProducer, "updateStatusOfEntitySetDocHandler",
                mockDocHandler, status);

        EntitySetAccessor mockAccessor = mock(EntitySetAccessor.class);
        doReturn(mockAccessor).when(messageODataProducer).getAccessorForEntitySet("dummyName");
        doReturn(1L).when(mockDocHandler).getVersion();
        doReturn("dummyId").when(mockDocHandler).getId();
        PersoniumIndexResponse mockIndexResponse = mock(PersoniumIndexResponse.class);
        doReturn(2L).when(mockIndexResponse).version();
        doReturn(mockIndexResponse).when(mockAccessor).update("dummyId", mockDocHandler, 1L);
        doNothing().when(mockDocHandler).setVersion(2L);
        doReturn("returnEtag").when(mockDocHandler).createEtag();

        // --------------------
        // Expected result
        // --------------------
        String expectedEtag = "returnEtag";

        // --------------------
        // Run method
        // --------------------
        String actualEtag = messageODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualEtag, is(expectedEtag));
        assertNull(mockConvertedStaticFields.get(Common.P_BOX_NAME.getName()));
    }

    /**
     * Test changeStatusAndUpdateRelation().
     * Error test.
     * EntitySetDocHandler is null.
     */
    @Test
    public void changeStatusAndUpdateRelation_Error_EntitySetDocHandler_is_null() {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EdmEntitySet entitySet = EdmEntitySet.newBuilder().setName("dummyName").build();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("dummy", "dummy");
        OEntityKey originalKey = OEntityKey.create(values);
        String status = "status";

        // --------------------
        // Mock settings
        // --------------------
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).release();
        doReturn(mockLock).when(messageODataProducer).lock();

        doReturn(null).when(messageODataProducer).retrieveWithKey(entitySet, originalKey);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            messageODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e, is(PersoniumCoreException.OData.NO_SUCH_ENTITY));
        }
    }

    /**
     * Test changeStatusAndUpdateRelation().
     * Error test.
     * ValidMessageStatus is false.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void changeStatusAndUpdateRelation_Error_ValidMessageStatus_is_false() {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EdmEntitySet entitySet = EdmEntitySet.newBuilder().setName("dummyName").build();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("dummy", "dummy");
        OEntityKey originalKey = OEntityKey.create(values);
        String status = "status";

        // --------------------
        // Mock settings
        // --------------------
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).release();
        doReturn(mockLock).when(messageODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(messageODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(messageODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(false).when(messageODataProducer).isValidMessageStatus(status);
        doReturn(true).when(messageODataProducer).isValidCurrentStatus("dummyStatus");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            messageODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.MESSAGE_COMMAND);
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test changeStatusAndUpdateRelation().
     * Error test.
     * ValidCurrentStatus is false.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void changeStatusAndUpdateRelation_Error_ValidCurrentStatus_is_false() {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EdmEntitySet entitySet = EdmEntitySet.newBuilder().setName("dummyName").build();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("dummy", "dummy");
        OEntityKey originalKey = OEntityKey.create(values);
        String status = "status";

        // --------------------
        // Mock settings
        // --------------------
        Lock mockLock = mock(Lock.class);
        doNothing().when(mockLock).release();
        doReturn(mockLock).when(messageODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(messageODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        List<Map<String, String>> mockRequestObjects = new ArrayList<Map<String, String>>();
        Map<String, String> mockRequestObject = new HashMap<String, String>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQUEST);
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");
        mockConvertedStaticFields.put(ReceivedMessage.P_REQUEST_OBJECTS.getName(), mockRequestObjects);
        mockRequestObjects.add(mockRequestObject);
        mockRequestObject.put(RequestObject.P_REQUEST_TYPE.getName(), RequestObject.REQUEST_TYPE_RELATION_ADD);

        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(messageODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(messageODataProducer).isValidMessageStatus(status);
        doReturn(false).when(messageODataProducer).isValidCurrentStatus("dummyStatus");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            messageODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.MESSAGE_COMMAND);
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test convertNtkpValueToFields().
     * Normal test.
     */
    @Test
    public void convertNtkpValueToFields_Normal() {
        // --------------------
        // Test method args
        // --------------------
        EdmEntitySet entitySet = EdmEntitySet.newBuilder().build();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        Map<String, Object> links = new HashMap<String, Object>();
        links.put("Box", "nGtWo7dYSymzkTjWGMHm1g");

        // --------------------
        // Mock settings
        // --------------------
        messageODataProducer = spy(new MessageODataProducer(new CellEsImpl(), null) {
            @Override
            protected void getNtkpValueMap(EdmEntitySet eSet,
                    Map<String, String> ntkpProperties,
                    Map<String, String> ntkpValueMap) {
                ntkpProperties.put("_Box.Name", "Box");
                ntkpProperties.put("_Relation.Name", "Relation");
                ntkpValueMap.put("_Box.NamenGtWo7dYSymzkTjWGMHm1g", "box1");
            }
        });

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedStaticFields = new HashMap<String, Object>();
        expectedStaticFields.put("_Box.Name", "box1");
        expectedStaticFields.put("_Relation.Name", null);

        // --------------------
        // Run method
        // --------------------
        staticFields = messageODataProducer.convertNtkpValueToFields(entitySet, staticFields, links);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(staticFields.get("_Box.Name"), is(expectedStaticFields.get("_Box.Name")));
        assertThat(staticFields.get("_Relation.Name"), is(expectedStaticFields.get("_Relation.Name")));
        assertThat(staticFields.containsKey("_Relation.Name"), is(true));
        // Confirm function call
        ArgumentCaptor<EdmEntitySet> captor = ArgumentCaptor.forClass(EdmEntitySet.class);
        verify(messageODataProducer, times(1)).getNtkpValueMap(captor.capture(), any(), any());
        assertThat(captor.getValue(), is(entitySet));
    }

    /**
     * Test getNameFromRequestRelation().
     * Normal test.
     * RequestRelation is ClassURL.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getNameFromRequestRelation_Normal_requestRelation_is_classURL() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String requestRelation = "personium-localunit:/dummyAppCell/__relation/__/dummyRelation";
        String regex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", requestRelation);

        // --------------------
        // Expected result
        // --------------------
        String expectedRelationName = "dummyRelation";

        // --------------------
        // Run method
        // --------------------
        String actualRelationName = messageODataProducer.getNameFromClassUrl(requestRelation, regex);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRelationName, is(expectedRelationName));
    }

    /**
     * Test getNameFromRequestRelation().
     * Normal test.
     * RequestRelation is Name.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getNameFromRequestRelation_Normal_requestRelation_is_name() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String requestRelation = "dummyRelation";
        String regex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn(requestRelation).when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", requestRelation);

        // --------------------
        // Expected result
        // --------------------
        String expectedRelationName = "dummyRelation";

        // --------------------
        // Run method
        // --------------------
        String actualRelationName = messageODataProducer.getNameFromClassUrl(requestRelation, regex);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRelationName, is(expectedRelationName));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * RequestRelation is RelationClassURL.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getBoxNameFromRequestRelation_Normal_requestRelation_is_classURL() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String requestRelation = "personium-localunit:/dummyAppCell/__relation/__/dummyRelation";
        String regex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
            UriUtils.class, "convertSchemeFromLocalUnitToHttp", requestRelation);

        Box mockBox = PowerMockito.mock(Box.class);
        doReturn("dummyBoxName").when(mockBox).getName();
        doReturn(mockBox).when(mockCell).getBoxForSchema("http://personium/dummyAppCell/");

        // --------------------
        // Expected result
        // --------------------
        String expectedBoxName = "dummyBoxName";

        // --------------------
        // Run method
        // --------------------
        String actualBoxName = messageODataProducer.getBoxNameFromClassUrl(requestRelation, regex);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualBoxName, is(expectedBoxName));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * RequestRelation is RelationName.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getBoxNameFromRequestRelation_Normal_requestRelation_is_name() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String requestRelation = "dummyRelation";
        String regex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn(requestRelation).when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", requestRelation);

        // --------------------
        // Expected result
        // --------------------
        String expectedBoxName = null;

        // --------------------
        // Run method
        // --------------------
        String actualBoxName = messageODataProducer.getBoxNameFromClassUrl(requestRelation, regex);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualBoxName, is(expectedBoxName));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Error test.
     * Box associated with class URL does not exist.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getBoxNameFromRequestRelation_Error_box_associated_with_classURL_does_not_exist() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String requestRelation = "personium-localunit:/dummyAppCell/__relation/__/dummyRelation";
        String regex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", requestRelation);

        doReturn(null).when(mockCell).getBoxForSchema("http://personium/dummyAppCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            messageODataProducer.getBoxNameFromClassUrl(requestRelation, regex);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage
                    .BOX_THAT_MATCHES_RELATION_CLASS_URL_NOT_EXISTS.params(
                    "http://personium/dummyAppCell/__relation/__/dummyRelation");
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

}
