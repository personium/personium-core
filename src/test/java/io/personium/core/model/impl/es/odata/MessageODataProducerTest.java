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
package io.personium.core.model.impl.es.odata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
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
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.EsNavigationTargetKeyProperty.NTKPNotFoundException;
import io.personium.core.model.lock.Lock;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * MessageODataProducerユニットテストクラス.
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
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(messageODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(messageODataProducer).isValidMessageStatus(status);
        doReturn(true).when(messageODataProducer).isValidCurrentStatus("dummyStatus");

        PowerMockito.doNothing().when(messageODataProducer, "updateRelation", mockDocHandler);
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
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");
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
        verify(messageODataProducer, times(1)).getNtkpValueMap(captor.capture(), anyObject(), anyObject());
        assertThat(captor.getValue(), is(entitySet));
    }


    /**
     * Test updateRelation().
     * boxName is null.
     * @throws Exception Unexpected error
     */
    @Test
    public void updateRelation_Normal_boxName_is_null() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_MESSAGE);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(Common.P_BOX_NAME.getName(), "dummyBox");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_MESSAGE);

        doReturn(null).when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_MESSAGE);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_MESSAGE);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_MESSAGE);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_MESSAGE);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_MESSAGE);

        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke(
                "registerRelation", anyString(), anyObject(), anyObject());
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke(
                "deleteRelation", anyString(), anyObject(), anyObject());
    }

    /**
     * Test updateRelation().
     * extCellUrl is not trailing slash.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void updateRelation_Normal_extCellUrl_is_not_trailing_slash() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        doReturn("dummyBox").when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "dummyBox");
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        PowerMockito.doNothing().when(messageODataProducer, "registerRelation", anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("registerRelation",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke("deleteRelation",
                anyString(), anyObject(), anyObject());

        assertThat(edmTypeCaptor.getValue(), is(Relation.EDM_TYPE_NAME));
        assertThat(entityKeyMapCaptor.getValue().get(Common.P_BOX_NAME.getName()), is("dummyBox"));
        assertThat(extCellKeyMapCaptor.getValue().get(ExtCell.P_URL.getName()), is("http://personium/dummyExtCell/"));
    }

    /**
     * Test updateRelation().
     * Type is relationBuild.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void updateRelation_Normal_type_is_relation_build() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        doReturn("dummyBox").when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "dummyBox");
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        PowerMockito.doNothing().when(messageODataProducer, "registerRelation", anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BUILD);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BUILD);

        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("registerRelation",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke("deleteRelation",
                anyString(), anyObject(), anyObject());

        assertThat(edmTypeCaptor.getValue(), is(Relation.EDM_TYPE_NAME));
        assertThat(entityKeyMapCaptor.getValue().get(Common.P_BOX_NAME.getName()), is("dummyBox"));
        assertThat(extCellKeyMapCaptor.getValue().get(ExtCell.P_URL.getName()), is("http://personium/dummyExtCell/"));
    }

    /**
     * Test updateRelation().
     * Type is relationBreak.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void updateRelation_Normal_type_is_relation_break() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_RELATION_BREAK);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BREAK);

        doReturn("dummyBox").when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BREAK);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "dummyBox");
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BREAK);

        PowerMockito.doNothing().when(messageODataProducer, "deleteRelation", anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BREAK);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_RELATION_BREAK);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_RELATION_BREAK);

        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke("registerRelation",
                anyString(), anyObject(), anyObject());
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("deleteRelation",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());

        assertThat(edmTypeCaptor.getValue(), is(Relation.EDM_TYPE_NAME));
        assertThat(entityKeyMapCaptor.getValue().get(Common.P_BOX_NAME.getName()), is("dummyBox"));
        assertThat(extCellKeyMapCaptor.getValue().get(ExtCell.P_URL.getName()), is("http://personium/dummyExtCell/"));
    }

    /**
     * Test updateRelation().
     * Type is roleGrant.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void updateRelation_Normal_type_is_role_grant() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_ROLE_GRANT);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_GRANT);

        doReturn("dummyBox").when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_GRANT);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "dummyBox");
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_ROLE_GRANT);

        PowerMockito.doNothing().when(messageODataProducer, "registerRelation", anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_GRANT);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_GRANT);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_ROLE_GRANT);

        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("registerRelation",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke("deleteRelation",
                anyString(), anyObject(), anyObject());

        assertThat(edmTypeCaptor.getValue(), is(Role.EDM_TYPE_NAME));
        assertThat(entityKeyMapCaptor.getValue().get(Common.P_BOX_NAME.getName()), is("dummyBox"));
        assertThat(extCellKeyMapCaptor.getValue().get(ExtCell.P_URL.getName()), is("http://personium/dummyExtCell/"));
    }

    /**
     * Test updateRelation().
     * Type is roleRevoke.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void updateRelation_Normal_type_is_role_revoke() throws Exception  {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_TYPE.getName(), ReceivedMessage.TYPE_REQ_ROLE_REVOKE);
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(messageODataProducer).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);

        doReturn("dummyBox").when(messageODataProducer).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);

        Map<String, Object> entityKeyMap = new HashMap<String, Object>();
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "dummyBox");
        PowerMockito.doReturn(entityKeyMap).when(messageODataProducer, "getEntityKeyMapFromType",
                "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);

        PowerMockito.doNothing().when(messageODataProducer, "deleteRelation", anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("updateRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).getNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);
        verify(messageODataProducer, times(1)).getBoxNameFromRequestRelation(
                "dummyRelation", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke(
                "getEntityKeyMapFromType", "dummyRelation", "dummyBox", ReceivedMessage.TYPE_REQ_ROLE_REVOKE);

        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke("registerRelation",
                anyString(), anyObject(), anyObject());
        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("deleteRelation",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());

        assertThat(edmTypeCaptor.getValue(), is(Role.EDM_TYPE_NAME));
        assertThat(entityKeyMapCaptor.getValue().get(Common.P_BOX_NAME.getName()), is("dummyBox"));
        assertThat(extCellKeyMapCaptor.getValue().get(ExtCell.P_URL.getName()), is("http://personium/dummyExtCell/"));
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
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", "http://personium", requestRelation);

        PowerMockito.doReturn(Common.PATTERN_RELATION_CLASS_URL).when(messageODataProducer, "getRegexFromType", type);

        // --------------------
        // Expected result
        // --------------------
        String expectedRelationName = "dummyRelation";

        // --------------------
        // Run method
        // --------------------
        String actualRelationName = messageODataProducer.getNameFromRequestRelation(requestRelation, type);

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
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn(requestRelation).when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", "http://personium", requestRelation);

        PowerMockito.doReturn(Common.PATTERN_RELATION_CLASS_URL).when(messageODataProducer, "getRegexFromType", type);

        // --------------------
        // Expected result
        // --------------------
        String expectedRelationName = "dummyRelation";

        // --------------------
        // Run method
        // --------------------
        String actualRelationName = messageODataProducer.getNameFromRequestRelation(requestRelation, type);

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
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
            UriUtils.class, "convertSchemeFromLocalUnitToHttp", "http://personium", requestRelation);

        PowerMockito.doReturn(Common.PATTERN_RELATION_CLASS_URL).when(messageODataProducer, "getRegexFromType", type);

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
        String actualBoxName = messageODataProducer.getBoxNameFromRequestRelation(requestRelation, type);

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
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn(requestRelation).when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", "http://personium", requestRelation);

        PowerMockito.doReturn(Common.PATTERN_RELATION_CLASS_URL).when(messageODataProducer, "getRegexFromType", type);

        // --------------------
        // Expected result
        // --------------------
        String expectedBoxName = null;

        // --------------------
        // Run method
        // --------------------
        String actualBoxName = messageODataProducer.getBoxNameFromRequestRelation(requestRelation, type);

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
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        Cell mockCell = mock(Cell.class);
        messageODataProducer.cell = mockCell;
        doReturn("http://personium").when(mockCell).getUnitUrl();

        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personium/dummyAppCell/__relation/__/dummyRelation").when(
                UriUtils.class, "convertSchemeFromLocalUnitToHttp", "http://personium", requestRelation);

        PowerMockito.doReturn(Common.PATTERN_RELATION_CLASS_URL).when(messageODataProducer, "getRegexFromType", type);

        doReturn(null).when(mockCell).getBoxForSchema("http://personium/dummyAppCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            messageODataProducer.getBoxNameFromRequestRelation(requestRelation, type);
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

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * Type is relationBuild.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getRegexFromType_Normal_type_is_relation_build() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        String expectedRegex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("getRegexFromType", String.class);
        method.setAccessible(true);
        // Run method
        String actualRegex = (String) method.invoke(messageODataProducer, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRegex, is(expectedRegex));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * Type is relationBreak.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getRegexFromType_Normal_type_is_relation_break() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        String expectedRegex = Common.PATTERN_RELATION_CLASS_URL;

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("getRegexFromType", String.class);
        method.setAccessible(true);
        // Run method
        String actualRegex = (String) method.invoke(messageODataProducer, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRegex, is(expectedRegex));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * Type is roleGrant.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getRegexFromType_Normal_type_is_role_grant() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        String expectedRegex = Common.PATTERN_ROLE_CLASS_URL;

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("getRegexFromType", String.class);
        method.setAccessible(true);
        // Run method
        String actualRegex = (String) method.invoke(messageODataProducer, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRegex, is(expectedRegex));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * Type is roleRevoke.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getRegexFromType_Normal_type_is_role_revoke() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_REQ_ROLE_REVOKE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        String expectedRegex = Common.PATTERN_ROLE_CLASS_URL;

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("getRegexFromType", String.class);
        method.setAccessible(true);
        // Run method
        String actualRegex = (String) method.invoke(messageODataProducer, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRegex, is(expectedRegex));
    }

    /**
     * Test getBoxNameFromRequestRelation().
     * Normal test.
     * Type is other.
     * @throws Exception Unexpected error.
     */
    @Test
    public void getRegexFromType_Normal_type_is_other() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String type = ReceivedMessage.TYPE_MESSAGE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        String expectedRegex = "";

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("getRegexFromType", String.class);
        method.setAccessible(true);
        // Run method
        String actualRegex = (String) method.invoke(messageODataProducer, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualRegex, is(expectedRegex));
    }

    /**
     * Test getEntityKeyMapFromType().
     * Normal test.
     * boxName is not null.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getEntityKeyMapFromType_Normal_boxName_is_not_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String name = "dummyName";
        String boxName = "dummyBox";
        String type = ReceivedMessage.TYPE_MESSAGE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedEntityKeyMap = new HashMap<>();
        expectedEntityKeyMap.put(Common.P_BOX_NAME.getName(), boxName);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "getEntityKeyMapFromType", String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Map<String, Object> actualEntityKeyMap = (Map<String, Object>) method.invoke(
                messageODataProducer, name, boxName, type);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualEntityKeyMap.get(Common.P_BOX_NAME.getName()),
                is(expectedEntityKeyMap.get(Common.P_BOX_NAME.getName())));
        assertNull(actualEntityKeyMap.get(Relation.P_NAME.getName()));
        assertNull(actualEntityKeyMap.get(Common.P_NAME.getName()));
    }

    /**
     * Test getEntityKeyMapFromType().
     * Normal test.
     * type is relationBuild.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getEntityKeyMapFromType_Normal_type_is_relation_build() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String name = "dummyName";
        String boxName = null;
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedEntityKeyMap = new HashMap<>();
        expectedEntityKeyMap.put(Relation.P_NAME.getName(), name);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "getEntityKeyMapFromType", String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Map<String, Object> actualEntityKeyMap = (Map<String, Object>) method.invoke(
                messageODataProducer, name, boxName, type);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(actualEntityKeyMap.get(Common.P_BOX_NAME.getName()));
        assertThat(actualEntityKeyMap.get(Relation.P_NAME.getName()),
                is(expectedEntityKeyMap.get(Relation.P_NAME.getName())));
    }

    /**
     * Test getEntityKeyMapFromType().
     * Normal test.
     * type is relationBreak.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getEntityKeyMapFromType_Normal_type_is_relation_break() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String name = "dummyName";
        String boxName = null;
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedEntityKeyMap = new HashMap<>();
        expectedEntityKeyMap.put(Relation.P_NAME.getName(), name);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "getEntityKeyMapFromType", String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Map<String, Object> actualEntityKeyMap = (Map<String, Object>) method.invoke(
                messageODataProducer, name, boxName, type);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(actualEntityKeyMap.get(Common.P_BOX_NAME.getName()));
        assertThat(actualEntityKeyMap.get(Relation.P_NAME.getName()),
                is(expectedEntityKeyMap.get(Relation.P_NAME.getName())));
    }

    /**
     * Test getEntityKeyMapFromType().
     * Normal test.
     * type is roleGrant.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getEntityKeyMapFromType_Normal_type_is_role_grant() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String name = "dummyName";
        String boxName = null;
        String type = ReceivedMessage.TYPE_REQ_ROLE_GRANT;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedEntityKeyMap = new HashMap<>();
        expectedEntityKeyMap.put(Common.P_NAME.getName(), name);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "getEntityKeyMapFromType", String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Map<String, Object> actualEntityKeyMap = (Map<String, Object>) method.invoke(
                messageODataProducer, name, boxName, type);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(actualEntityKeyMap.get(Common.P_BOX_NAME.getName()));
        assertThat(actualEntityKeyMap.get(Common.P_NAME.getName()),
                is(expectedEntityKeyMap.get(Common.P_NAME.getName())));
    }

    /**
     * Test getEntityKeyMapFromType().
     * Normal test.
     * type is roleRevoke.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getEntityKeyMapFromType_Normal_type_is_role_revoke() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String name = "dummyName";
        String boxName = null;
        String type = ReceivedMessage.TYPE_REQ_ROLE_REVOKE;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedEntityKeyMap = new HashMap<>();
        expectedEntityKeyMap.put(Common.P_NAME.getName(), name);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "getEntityKeyMapFromType", String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Map<String, Object> actualEntityKeyMap = (Map<String, Object>) method.invoke(
                messageODataProducer, name, boxName, type);

        // --------------------
        // Confirm result
        // --------------------
        assertNull(actualEntityKeyMap.get(Common.P_BOX_NAME.getName()));
        assertThat(actualEntityKeyMap.get(Common.P_NAME.getName()),
                is(expectedEntityKeyMap.get(Common.P_NAME.getName())));
    }

    /**
     * Test registerRelation().
     * Entity and extcell is not exists.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void registerRelation_Normal_not_exists_entity_and_extcell() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        doReturn(null).when(messageODataProducer).getEntitySetDocHandler(anyString(), anyMap());
        PowerMockito.doNothing().when(messageODataProducer,
                "createOEntity", anyString(), anyMapOf(String.class, Object.class));
        PowerMockito.doNothing().when(messageODataProducer, "createExtCellLinks", anyString(), anyMap(), anyMap());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "registerRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);


        verify(messageODataProducer, times(2)).getEntitySetDocHandler(
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture());
        List<String> edmTypeCaptorValueList = edmTypeCaptor.getAllValues();
        List<Map> entityKeyMapCaptorValueList = entityKeyMapCaptor.getAllValues();
        assertThat(edmType, is(edmTypeCaptorValueList.get(0)));
        assertThat(entityKeyMap, is(entityKeyMapCaptorValueList.get(0)));
        assertThat(ExtCell.EDM_TYPE_NAME, is(edmTypeCaptorValueList.get(1)));
        assertThat(extCellKeyMap, is(entityKeyMapCaptorValueList.get(1)));

        PowerMockito.verifyPrivate(messageODataProducer, times(2)).invoke(
                "createOEntity", edmTypeCaptor.capture(), entityKeyMapCaptor.capture());
        edmTypeCaptorValueList = edmTypeCaptor.getAllValues();
        entityKeyMapCaptorValueList = entityKeyMapCaptor.getAllValues();
        assertThat(edmType, is(edmTypeCaptorValueList.get(0)));
        assertThat(entityKeyMap, is(entityKeyMapCaptorValueList.get(0)));
        assertThat(ExtCell.EDM_TYPE_NAME, is(edmTypeCaptorValueList.get(1)));
        assertThat(extCellKeyMap, is(entityKeyMapCaptorValueList.get(1)));

        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("createExtCellLinks",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());
        assertThat(edmType, is(edmTypeCaptor.getValue()));
        assertThat(entityKeyMap, is(entityKeyMapCaptor.getValue()));
        assertThat(extCellKeyMap, is(extCellKeyMapCaptor.getValue()));
    }

    /**
     * Test registerRelation().
     * Entity and extcell is exists.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void registerRelation_Normal_exists_entity_and_extcell() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        doReturn(mockDocHandler).when(messageODataProducer).getEntitySetDocHandler(anyString(), anyMap());
        PowerMockito.doNothing().when(messageODataProducer, "createExtCellLinks", anyString(), anyMap(), anyMap());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "registerRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> extCellKeyMapCaptor = ArgumentCaptor.forClass(Map.class);


        verify(messageODataProducer, times(2)).getEntitySetDocHandler(
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture());
        List<String> edmTypeCaptorValueList = edmTypeCaptor.getAllValues();
        List<Map> entityKeyMapCaptorValueList = entityKeyMapCaptor.getAllValues();
        assertThat(edmType, is(edmTypeCaptorValueList.get(0)));
        assertThat(entityKeyMap, is(entityKeyMapCaptorValueList.get(0)));
        assertThat(ExtCell.EDM_TYPE_NAME, is(edmTypeCaptorValueList.get(1)));
        assertThat(extCellKeyMap, is(entityKeyMapCaptorValueList.get(1)));

        PowerMockito.verifyPrivate(messageODataProducer, never()).invoke(
                "createOEntity", edmTypeCaptor.capture(), entityKeyMapCaptor.capture());

        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("createExtCellLinks",
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture(), extCellKeyMapCaptor.capture());
        assertThat(edmType, is(edmTypeCaptor.getValue()));
        assertThat(entityKeyMap, is(entityKeyMapCaptor.getValue()));
        assertThat(extCellKeyMap, is(extCellKeyMapCaptor.getValue()));
    }

    /**
     * Test deleteRelation().
     * Normal test.
     * @throws Exception Unexpected error
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void deleteRelation_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler entity = new OEntityDocHandler();
        EntitySetDocHandler extCell = new OEntityDocHandler();
        doReturn(entity).when(messageODataProducer).getEntitySetDocHandler(edmType, entityKeyMap);
        doReturn(extCell).when(messageODataProducer).getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap);

        doReturn(true).when(messageODataProducer).deleteLinkEntity(entity, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "deleteRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<String> edmTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> entityKeyMapCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messageODataProducer, times(2)).getEntitySetDocHandler(
                edmTypeCaptor.capture(), entityKeyMapCaptor.capture());
        List<String> edmTypeCaptorValueList = edmTypeCaptor.getAllValues();
        List<Map> entityKeyMapCaptorValueList = entityKeyMapCaptor.getAllValues();
        assertThat(edmType, is(edmTypeCaptorValueList.get(0)));
        assertThat(entityKeyMap, is(entityKeyMapCaptorValueList.get(0)));
        assertThat(ExtCell.EDM_TYPE_NAME, is(edmTypeCaptorValueList.get(1)));
        assertThat(extCellKeyMap, is(entityKeyMapCaptorValueList.get(1)));

        verify(messageODataProducer, times(1)).deleteLinkEntity(entity, extCell);
    }

    /**
     * Test deleteRelation().
     * Error test.
     * entity is null.
     * @throws Exception Unexpected error
     */
    @Test
    public void deleteRelation_Error_entity_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler entity = null;
        EntitySetDocHandler extCell = new OEntityDocHandler();
        doReturn(entity).when(messageODataProducer).getEntitySetDocHandler(edmType, entityKeyMap);
        doReturn(extCell).when(messageODataProducer).getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap);

        doReturn(true).when(messageODataProducer).deleteLinkEntity(entity, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "deleteRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                            edmType, entityKeyMap.toString());
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test deleteRelation().
     * Error test.
     * extCell is null.
     * @throws Exception Unexpected error
     */
    @Test
    public void deleteRelation_Error_extCell_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler entity = new OEntityDocHandler();
        EntitySetDocHandler extCell = null;
        doReturn(entity).when(messageODataProducer).getEntitySetDocHandler(edmType, entityKeyMap);
        doReturn(extCell).when(messageODataProducer).getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap);

        doReturn(true).when(messageODataProducer).deleteLinkEntity(entity, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "deleteRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS.params(
                            ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test deleteRelation().
     * Error test.
     * deleteLinkEntity is false.
     * @throws Exception Unexpected error
     */
    @Test
    public void deleteRelation_Error_deleteLinkEntity_is_false() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put("dummyKey1", "dummyValue1");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put("dummyKey2", "dummyValue2");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler entity = new OEntityDocHandler();
        EntitySetDocHandler extCell = new OEntityDocHandler();
        doReturn(entity).when(messageODataProducer).getEntitySetDocHandler(edmType, entityKeyMap);
        doReturn(extCell).when(messageODataProducer).getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap);

        doReturn(false).when(messageODataProducer).deleteLinkEntity(entity, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod(
                "deleteRelation", String.class, Map.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.LINK_DOES_NOT_EXISTS.params(
                            edmType, entityKeyMap.toString(), ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test getEntitySetDocHandler().
     * Normal test.
     */
    @Test
    public void getEntitySetDocHandler_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Relation.P_NAME.getName(), "relationName");
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "boxName");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetDocHandler entitySetDocHandler = new OEntityDocHandler();
        entitySetDocHandler.setStaticFields(entityKeyMap);
        doReturn(entitySetDocHandler).when(messageODataProducer).retrieveWithKey(anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        EntitySetDocHandler actual = messageODataProducer.getEntitySetDocHandler(edmType, entityKeyMap);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actual, is(entitySetDocHandler));
    }

    /**
     * Test createOEntity().
     * EntityKey type is complex.
     * @throws Exception Unexpected error
     */
    @Test
    public void createOEntity_Normal_entitykey_type_is_complex() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String typeName = "dummyTypeName";
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_NAME.getName(), "dummyRelationName");
        staticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetAccessor mockEsType = mock(EntitySetAccessor.class);
        doReturn(mockEsType).when(messageODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doNothing().when(messageODataProducer, "setLinksFromOEntityKey",
                anyObject(), anyString(), anyObject());

        doNothing().when(messageODataProducer).beforeCreate(anyString(), anyObject(), anyObject());
        doReturn(null).when(mockEsType).create(anyString(), anyObject());
        doNothing().when(messageODataProducer).afterCreate(anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        EntitySetDocHandler expectedOedh = new OEntityDocHandler();
        expectedOedh.setType(typeName);
        expectedOedh.setStaticFields(staticFields);
        expectedOedh.setBoxId(null);
        expectedOedh.setNodeId(null);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("createOEntity",
                String.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, typeName, staticFields);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<OEntityKey> entityKeyCaptor = ArgumentCaptor.forClass(OEntityKey.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntitySetDocHandler> docHandlerCaptor = ArgumentCaptor.forClass(EntitySetDocHandler.class);
        ArgumentCaptor<OEntity> entityCaptor = ArgumentCaptor.forClass(OEntity.class);

        PowerMockito.verifyPrivate(messageODataProducer, times(1)).invoke("setLinksFromOEntityKey",
                entityKeyCaptor.capture(), stringCaptor.capture(), docHandlerCaptor.capture());
        assertThat(entityKeyCaptor.getValue(), is(OEntityKey.create(staticFields)));
        assertThat(stringCaptor.getValue(), is(typeName));
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(messageODataProducer, times(1)).beforeCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(mockEsType, times(1)).create(stringCaptor.capture(), docHandlerCaptor.capture());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(messageODataProducer, times(1)).afterCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());
    }

    /**
     * Test createOEntity().
     * EntityKey type is not complex.
     * @throws Exception Unexpected error
     */
    @Test
    public void createOEntity_Normal_entitykey_type_is_not_complex() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String typeName = "dummyTypeName";
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_NAME.getName(), "dummyRelationName");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetAccessor mockEsType = mock(EntitySetAccessor.class);
        doReturn(mockEsType).when(messageODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doNothing().when(messageODataProducer, "setLinksFromOEntityKey",
                anyObject(), anyString(), anyObject());

        doNothing().when(messageODataProducer).beforeCreate(anyString(), anyObject(), anyObject());
        doReturn(null).when(mockEsType).create(anyString(), anyObject());
        doNothing().when(messageODataProducer).afterCreate(anyString(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        EntitySetDocHandler expectedOedh = new OEntityDocHandler();
        expectedOedh.setType(typeName);
        expectedOedh.setStaticFields(staticFields);
        expectedOedh.setBoxId(null);
        expectedOedh.setNodeId(null);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("createOEntity",
                String.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, typeName, staticFields);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<OEntityKey> entityKeyCaptor = ArgumentCaptor.forClass(OEntityKey.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntitySetDocHandler> docHandlerCaptor = ArgumentCaptor.forClass(EntitySetDocHandler.class);
        ArgumentCaptor<OEntity> entityCaptor = ArgumentCaptor.forClass(OEntity.class);

        PowerMockito.verifyPrivate(messageODataProducer, times(0)).invoke("setLinksFromOEntityKey",
                entityKeyCaptor.capture(), stringCaptor.capture(), docHandlerCaptor.capture());

        verify(messageODataProducer, times(1)).beforeCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(mockEsType, times(1)).create(stringCaptor.capture(), docHandlerCaptor.capture());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(messageODataProducer, times(1)).afterCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());
    }

    /**
     * Confirm EntitySetDocHandler.
     * Indefinite values such as UUID and SystemDate are not checked.
     * @param expected expected
     * @param actual actual
     */
    private void confirmEntitySetDocHandler(EntitySetDocHandler expected, EntitySetDocHandler actual) {
        assertThat(expected.getType(), is(actual.getType()));
        assertThat(expected.getStaticFields(), is(actual.getStaticFields()));
        assertThat(expected.getBoxId(), is(actual.getBoxId()));
        assertThat(expected.getNodeId(), is(actual.getNodeId()));
    }

    /**
     * Test createOEntity().
     * Error test.
     * setLinksFromOEntityKey fail.
     * @throws Exception Unexpected error
     */
    @Test
    public void createOEntity_Error_setLinksFromOEntityKey_fail() throws Exception {
        messageODataProducer = PowerMockito.spy(new MessageODataProducer(new CellEsImpl(), null));
        // --------------------
        // Test method args
        // --------------------
        String typeName = "dummyTypeName";
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_NAME.getName(), "dummyRelationName");
        staticFields.put(Common.P_BOX_NAME.getName(), "dummyBoxName");

        // --------------------
        // Mock settings
        // --------------------
        EntitySetAccessor mockEsType = mock(EntitySetAccessor.class);
        doReturn(mockEsType).when(messageODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doThrow(new NTKPNotFoundException("dummyMsg")).when(messageODataProducer,
                "setLinksFromOEntityKey", anyObject(), anyString(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("createOEntity",
                String.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, typeName, staticFields);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected =
                    PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params("dummyMsg");
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test setLinksFromOEntityKey().
     * Normal test.
     * @throws Exception Unexpected error
     */
    @Test
    public void setLinksFromOEntityKey_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        OEntityKey key = OEntityKey.create(
                Relation.P_NAME.getName(), "dummyRelationName",
                Common.P_BOX_NAME.getName(), "dummyBoxName");
        String typeName = Relation.EDM_TYPE_NAME;
        EntitySetDocHandler oedh = new OEntityDocHandler();

        // --------------------
        // Mock settings
        // --------------------
        doNothing().when(messageODataProducer).setLinksForOedh(anyObject(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("setLinksFromOEntityKey",
                OEntityKey.class, String.class, EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, key, typeName, oedh);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(messageODataProducer, times(1)).setLinksForOedh(anyObject(), anyObject(), anyObject());
    }

    /**
     * Test setLinksFromOEntityKey().
     * Error test.
     * setLinksForOedh fail.
     * @throws Exception Unexpected error
     */
    @Test
    public void setLinksFromOEntityKey_Error_setLinksForOedh_fail() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        OEntityKey key = OEntityKey.create(
                Relation.P_NAME.getName(), "dummyRelationName",
                Common.P_BOX_NAME.getName(), "dummyBoxName");
        String typeName = Relation.EDM_TYPE_NAME;
        EntitySetDocHandler oedh = new OEntityDocHandler();

        // --------------------
        // Mock settings
        // --------------------
        doThrow(new NTKPNotFoundException("dummyMsg")).when(messageODataProducer).setLinksForOedh(
                anyObject(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("setLinksFromOEntityKey",
                OEntityKey.class, String.class, EntitySetDocHandler.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, key, typeName, oedh);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            NTKPNotFoundException expected = new NTKPNotFoundException("dummyMsg");
            NTKPNotFoundException exception = (NTKPNotFoundException) e.getCause();
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test createExtCellLinks().
     * Normal test.
     * @throws Exception Unexpected error
     */
    @Test
    public void createExtCellLinks_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Relation.P_NAME.getName(), "relationName");
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "boxName");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put(ExtCell.P_URL.getName(), "http://personium/ExtCell/");

        // --------------------
        // Mock settings
        // --------------------
        doNothing().when(messageODataProducer).createLinks(anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("createExtCellLinks",
                String.class, Map.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);

        // --------------------
        // Confirm result
        // --------------------
        ArgumentCaptor<OEntityId> entityIdCaptor = ArgumentCaptor.forClass(OEntityId.class);
        ArgumentCaptor<OEntityId> extCellEntityIdCaptor = ArgumentCaptor.forClass(OEntityId.class);

        verify(messageODataProducer, times(1)).createLinks(entityIdCaptor.capture(), extCellEntityIdCaptor.capture());
        OEntityId actualEntityId = entityIdCaptor.getValue();
        OEntityKey actualOEntityKey = actualEntityId.getEntityKey();
        assertThat(actualEntityId.getEntitySetName(), is(Relation.EDM_TYPE_NAME));
        assertThat(actualOEntityKey.toKeyString(),
                is("(" + Relation.P_NAME.getName() + "='relationName'," + Common.P_BOX_NAME.getName() + "='boxName')"));

        OEntityId actualExtCellEntityId = extCellEntityIdCaptor.getValue();
        OEntityKey actualExtCellOEntityKey = actualExtCellEntityId.getEntityKey();
        assertThat(actualExtCellEntityId.getEntitySetName(), is(ExtCell.EDM_TYPE_NAME));
        assertThat(actualExtCellOEntityKey.toKeyString(), is("('http://personium/ExtCell/')"));
    }

    /**
     * Test createExtCellLinks().
     * Error test.
     * createLinks fail.
     * @throws Exception Unexpected error
     */
    @Test
    public void createExtCellLinks_Error_createLinks_fail() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String edmType = Relation.EDM_TYPE_NAME;
        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Relation.P_NAME.getName(), "relationName");
        entityKeyMap.put(Common.P_BOX_NAME.getName(), "boxName");
        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put(ExtCell.P_URL.getName(), "http://personium/ExtCell/");

        // --------------------
        // Mock settings
        // --------------------
        doThrow(PersoniumCoreException.OData.CONFLICT_LINKS).when(messageODataProducer).createLinks(
                anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = MessageODataProducer.class.getDeclaredMethod("createExtCellLinks",
                String.class, Map.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(messageODataProducer, edmType, entityKeyMap, extCellKeyMap);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }
}
