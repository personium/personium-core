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
package io.personium.core.model.impl.es.odata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
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
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.EsNavigationTargetKeyProperty.NTKPNotFoundException;
import io.personium.core.model.lock.Lock;
import io.personium.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CellCtlODataProducer.class, Box.class})
@Category({ Unit.class })
public class CellCtlODataProducerTest {

    /** Target class of unit test. */
    private CellCtlODataProducer cellCtlODataProducer;

    /**
     * Before.
     */
    @Before
    public void befor() {
        cellCtlODataProducer = spy(new CellCtlODataProducer(new CellEsImpl()));
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_READの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_READの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_UNREADの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_UNREADの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * メッセージのバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidMessageStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_NONEの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_NONEの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_READの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_READの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_UNREADの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_UNREADの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_NONEの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_NONEの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_READの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_READの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidRelationStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 現状ののバリデートにて_TYPE_REQ_RELATION_BUILDの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと() {
        assertTrue(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * REQ_RELATION_BUILDの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * REQ_RELATION_BUILDの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(cellCtlODataProducer.isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * extCellの取得で存在する場合にEntitySetDocHandlerが返却されること.
     */
    @Test
    public void extCellの取得で存在する場合にEntitySetDocHandlerが返却されること() {
        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).retrieveWithKey(anyObject(), anyObject());
        assertTrue(cellCtlODataProducer.getExtCell("https://example.com/test0110/") != null);
    }

    /**
     * リレーションクラスURLのフォーマットが不正な場合はnullが取得できること.
     */
    @Test
    public void extCellの存在確認でOEntityKeyのパースに失敗した場合はREQUEST_RELATION_TARGET_PARSE_ERRORが発生すること() {
        try {
            doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).retrieveWithKey(anyObject(), anyObject());
            cellCtlODataProducer.getExtCell("https://example.com/'/");
            fail("PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_TARGET_PARSE_ERROR does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_TARGET_PARSE_ERROR;
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * Test beforeCreate().
     * EntitySet is ReceivedMessage.
     * staticFields BoxName is not null.
     */
    @Test
    public void beforeCreate_Normal_receivedmessage_boxname_not_null() {
        String pBoxName = ReceivedMessage.P_BOX_NAME.getName();
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
        cellCtlODataProducer.cell = mockCell;
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
        cellCtlODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

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
        String pBoxName = ReceivedMessage.P_BOX_NAME.getName();
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
        cellCtlODataProducer.cell = mockCell;
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
        cellCtlODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

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
        String pBoxName = SentMessage.P_BOX_NAME.getName();
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
        cellCtlODataProducer.cell = mockCell;
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
        cellCtlODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

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
        String pBoxName = SentMessage.P_BOX_NAME.getName();
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
        cellCtlODataProducer.cell = mockCell;
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
        cellCtlODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

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
        String pBoxName = ReceivedMessage.P_BOX_NAME.getName();
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
        cellCtlODataProducer.beforeCreate(entitySetName, oEntity, docHandler);

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
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockLock).when(cellCtlODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(cellCtlODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), "dummyType");
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(cellCtlODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(cellCtlODataProducer).isValidMessageStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidRelationStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidCurrentStatus("dummyType", "dummyStatus");

        PowerMockito.doNothing().when(cellCtlODataProducer, "updateRelation", mockDocHandler, status);
        PowerMockito.doNothing().when(cellCtlODataProducer, "updateStatusOfEntitySetDocHandler",
                mockDocHandler, status);

        EntitySetAccessor mockAccessor = mock(EntitySetAccessor.class);
        doReturn(mockAccessor).when(cellCtlODataProducer).getAccessorForEntitySet("dummyName");
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
        String actualEtag = cellCtlODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actualEtag, is(expectedEtag));
        assertNull(mockConvertedStaticFields.get(ReceivedMessage.P_BOX_NAME.getName()));
    }

    /**
     * Test changeStatusAndUpdateRelation().
     * Error test.
     * EntitySetDocHandler is null.
     */
    @Test
    public void changeStatusAndUpdateRelation_Error_EntitySetDocHandler_is_null() {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockLock).when(cellCtlODataProducer).lock();

        doReturn(null).when(cellCtlODataProducer).retrieveWithKey(entitySet, originalKey);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
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
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockLock).when(cellCtlODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(cellCtlODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), "dummyType");
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(cellCtlODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(false).when(cellCtlODataProducer).isValidMessageStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidRelationStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidCurrentStatus("dummyType", "dummyStatus");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
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
     * ValidRelationStatus is false.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void changeStatusAndUpdateRelation_Error_ValidRelationStatus_is_false() {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockLock).when(cellCtlODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(cellCtlODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), "dummyType");
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(cellCtlODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(cellCtlODataProducer).isValidMessageStatus("dummyType", status);
        doReturn(false).when(cellCtlODataProducer).isValidRelationStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidCurrentStatus("dummyType", "dummyStatus");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
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
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockLock).when(cellCtlODataProducer).lock();

        EntitySetDocHandler mockDocHandler = mock(EntitySetDocHandler.class);
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        Map<String, Object> mockManyToOnelinkId = new HashMap<String, Object>();
        doReturn(mockStaticFields).when(mockDocHandler).getStaticFields();
        doReturn(mockManyToOnelinkId).when(mockDocHandler).getManyToOnelinkId();
        doReturn(mockDocHandler).when(cellCtlODataProducer).retrieveWithKey(entitySet, originalKey);

        Map<String, Object> mockConvertedStaticFields = new HashMap<String, Object>();
        mockConvertedStaticFields.put(ReceivedMessage.P_TYPE.getName(), "dummyType");
        mockConvertedStaticFields.put(ReceivedMessage.P_STATUS.getName(), "dummyStatus");
        mockConvertedStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        // Change the return value according to the number of calls to getStaticFields
        when(mockDocHandler.getStaticFields()).thenReturn(
                mockStaticFields, mockConvertedStaticFields, mockConvertedStaticFields);
        doReturn(mockConvertedStaticFields).when(cellCtlODataProducer).convertNtkpValueToFields(entitySet,
                mockStaticFields, mockManyToOnelinkId);
        doNothing().when(mockDocHandler).setStaticFields(mockConvertedStaticFields);

        doReturn(true).when(cellCtlODataProducer).isValidMessageStatus("dummyType", status);
        doReturn(true).when(cellCtlODataProducer).isValidRelationStatus("dummyType", status);
        doReturn(false).when(cellCtlODataProducer).isValidCurrentStatus("dummyType", "dummyStatus");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.changeStatusAndUpdateRelation(entitySet, originalKey, status);
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
        cellCtlODataProducer = spy(new CellCtlODataProducer(new CellEsImpl()) {
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
        staticFields = cellCtlODataProducer.convertNtkpValueToFields(entitySet, staticFields, links);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(staticFields.get("_Box.Name"), is(expectedStaticFields.get("_Box.Name")));
        assertThat(staticFields.get("_Relation.Name"), is(expectedStaticFields.get("_Relation.Name")));
        assertThat(staticFields.containsKey("_Relation.Name"), is(true));
        // Confirm function call
        ArgumentCaptor<EdmEntitySet> captor = ArgumentCaptor.forClass(EdmEntitySet.class);
        verify(cellCtlODataProducer, times(1)).getNtkpValueMap(captor.capture(), anyObject(), anyObject());
        assertThat(captor.getValue(), is(entitySet));
    }

    /**
     * Test buildRelation().
     * Relation and extcell is not exists.
     * @throws Exception Unexpected error
     */
    @Test
    public void buildRelation_Normal_not_exists_relation_and_extcell() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation("dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation("dummyRelation");

        doReturn(null).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationEntity", "dummyRelation", "dummyBoxName");

        doReturn(null).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createExtCellEntity", "http://personium/dummyExtCell/");

        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationExtCellLinks", "dummyRelation",
                "dummyBoxName", "http://personium/dummyExtCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("buildRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createRelationEntity", "dummyRelation", "dummyBoxName");
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createExtCellEntity", "http://personium/dummyExtCell/");
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createRelationExtCellLinks", "dummyRelation", "dummyBoxName", "http://personium/dummyExtCell/");
    }

    /**
     * Test buildRelation().
     * Relation and extcell is exists.
     * @throws Exception Unexpected error
     */
    @Test
    public void buildRelation_Normal_exists_relation_and_extcell() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation("dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation("dummyRelation");

        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationEntity", "dummyRelation", "dummyBoxName");

        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createExtCellEntity", "http://personium/dummyExtCell/");

        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationExtCellLinks", "dummyRelation",
                "dummyBoxName", "http://personium/dummyExtCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("buildRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        PowerMockito.verifyPrivate(cellCtlODataProducer, never()).invoke(
                "createRelationEntity", "dummyRelation", "dummyBoxName");
        PowerMockito.verifyPrivate(cellCtlODataProducer, never()).invoke(
                "createExtCellEntity", "http://personium/dummyExtCell/");
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createRelationExtCellLinks", "dummyRelation", "dummyBoxName", "http://personium/dummyExtCell/");
    }

    /**
     * Test buildRelation().
     * requestExtCell does not end with a slash.
     * @throws Exception Unexpected error
     */
    @Test
    public void buildRelation_Normal_requestExtCell_does_not_endwith_slash() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(), "dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation("dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation("dummyRelation");

        doReturn(null).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationEntity", "dummyRelation", "dummyBoxName");

        doReturn(null).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");
        PowerMockito.doNothing().when(cellCtlODataProducer, "createExtCellEntity", "http://personium/dummyExtCell/");

        PowerMockito.doNothing().when(cellCtlODataProducer, "createRelationExtCellLinks", "dummyRelation",
                "dummyBoxName", "http://personium/dummyExtCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("buildRelation", EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createRelationEntity", "dummyRelation", "dummyBoxName");
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createExtCellEntity", "http://personium/dummyExtCell/");
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke(
                "createRelationExtCellLinks", "dummyRelation", "dummyBoxName", "http://personium/dummyExtCell/");
    }

    /**
     * Test createRelationOEntityKey().
     * BoxName is not null.
     * @throws Exception Unexpected error
     */
    @Test
    public void createRelationOEntityKey_Normal_boxName_is_not_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String relationName = "dummyRelationName";
        String boxName = "dummyBoxName";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        OEntityKey expectedOEntityKey = OEntityKey.create(
                Relation.P_NAME.getName(), relationName,
                Common.P_BOX_NAME.getName(), boxName);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createRelationOEntityKey",
                String.class, String.class);
        method.setAccessible(true);
        // Run method
        OEntityKey resultOEntityKey = (OEntityKey) method.invoke(cellCtlODataProducer, relationName, boxName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(resultOEntityKey.toString(), is(expectedOEntityKey.toString()));
    }

    /**
     * Test createRelationOEntityKey().
     * BoxName is null.
     * @throws Exception Unexpected error
     */
    @Test
    public void createRelationOEntityKey_Normal_boxName_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String relationName = "dummyRelationName";
        String boxName = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        OEntityKey expectedOEntityKey = OEntityKey.create(
                Relation.P_NAME.getName(), relationName);

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createRelationOEntityKey",
                String.class, String.class);
        method.setAccessible(true);
        // Run method
        OEntityKey resultOEntityKey = (OEntityKey) method.invoke(cellCtlODataProducer, relationName, boxName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(resultOEntityKey.toString(), is(expectedOEntityKey.toString()));
    }

    /**
     * Test createRelationOEntityKey().
     * Error test.
     * EntityKey parse failed.
     * @throws Exception Unexpected error
     */
    @Test
    public void createRelationOEntityKey_Error_entityKey_parse_failed() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String relationName = "'dummy'";
        String boxName = null;

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
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createRelationOEntityKey",
                String.class, String.class);
        method.setAccessible(true);
        // Run method
        try {
            method.invoke(cellCtlODataProducer, relationName, boxName);
            fail("Not exception.");
        } catch (InvocationTargetException e) {
            // --------------------
            // Confirm result
            // --------------------
            IllegalArgumentException cause = new IllegalArgumentException(
                    "bad valueString [''dummy''] as part of keyString [''dummy'']");
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_PARSE_ERROR.reason(cause);
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getStatus(), is(expected.getStatus()));
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
            assertThat(exception.getCause().getMessage(), is(expected.getCause().getMessage()));
        }
    }

    /**
     * Test createRelationEntity().
     * BoxName is not null.
     * @throws Exception Unexpected error
     */
    @Test
    public void createRelationEntity_Normal_boxName_is_not_null() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
        // --------------------
        // Test method args
        // --------------------
        String relationName = "dummyRelationName";
        String boxName = "dummyBoxName";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.doNothing().when(cellCtlODataProducer, "createEntity", anyString(), anyMap());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createRelationEntity",
                String.class, String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, relationName, boxName);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_NAME.getName(), relationName);
        staticFields.put(Common.P_BOX_NAME.getName(), boxName);
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke("createEntity",
                Relation.EDM_TYPE_NAME, staticFields);
    }

    /**
     * Test createRelationEntity().
     * BoxName is null.
     * @throws Exception Unexpected error
     */
    @Test
    public void createRelationEntity_Normal_boxName_is_null() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
        // --------------------
        // Test method args
        // --------------------
        String relationName = "dummyRelationName";
        String boxName = null;

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.doNothing().when(cellCtlODataProducer, "createEntity", anyString(), anyMap());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createRelationEntity",
                String.class, String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, relationName, boxName);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_NAME.getName(), relationName);
        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke("createEntity",
                Relation.EDM_TYPE_NAME, staticFields);
    }

    /**
     * Test createEntity().
     * EntityKey type is complex.
     * @throws Exception Unexpected error
     */
    @Test
    public void createEntity_Normal_entitykey_type_is_complex() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockEsType).when(cellCtlODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doNothing().when(cellCtlODataProducer, "setLinksFromOEntityKey",
                anyObject(), anyString(), anyObject());

        doNothing().when(cellCtlODataProducer).beforeCreate(anyString(), anyObject(), anyObject());
        doReturn(null).when(mockEsType).create(anyString(), anyObject());
        doNothing().when(cellCtlODataProducer).afterCreate(anyString(), anyObject(), anyObject());

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
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createEntity",
                String.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, typeName, staticFields);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<OEntityKey> entityKeyCaptor = ArgumentCaptor.forClass(OEntityKey.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntitySetDocHandler> docHandlerCaptor = ArgumentCaptor.forClass(EntitySetDocHandler.class);
        ArgumentCaptor<OEntity> entityCaptor = ArgumentCaptor.forClass(OEntity.class);

        PowerMockito.verifyPrivate(cellCtlODataProducer, times(1)).invoke("setLinksFromOEntityKey",
                entityKeyCaptor.capture(), stringCaptor.capture(), docHandlerCaptor.capture());
        assertThat(entityKeyCaptor.getValue(), is(OEntityKey.create(staticFields)));
        assertThat(stringCaptor.getValue(), is(typeName));
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(cellCtlODataProducer, times(1)).beforeCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(mockEsType, times(1)).create(stringCaptor.capture(), docHandlerCaptor.capture());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(cellCtlODataProducer, times(1)).afterCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());
    }

    /**
     * Test createEntity().
     * EntityKey type is not complex.
     * @throws Exception Unexpected error
     */
    @Test
    public void createEntity_Normal_entitykey_type_is_not_complex() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockEsType).when(cellCtlODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doNothing().when(cellCtlODataProducer, "setLinksFromOEntityKey",
                anyObject(), anyString(), anyObject());

        doNothing().when(cellCtlODataProducer).beforeCreate(anyString(), anyObject(), anyObject());
        doReturn(null).when(mockEsType).create(anyString(), anyObject());
        doNothing().when(cellCtlODataProducer).afterCreate(anyString(), anyObject(), anyObject());

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
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createEntity",
                String.class, Map.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, typeName, staticFields);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        ArgumentCaptor<OEntityKey> entityKeyCaptor = ArgumentCaptor.forClass(OEntityKey.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntitySetDocHandler> docHandlerCaptor = ArgumentCaptor.forClass(EntitySetDocHandler.class);
        ArgumentCaptor<OEntity> entityCaptor = ArgumentCaptor.forClass(OEntity.class);

        PowerMockito.verifyPrivate(cellCtlODataProducer, times(0)).invoke("setLinksFromOEntityKey",
                entityKeyCaptor.capture(), stringCaptor.capture(), docHandlerCaptor.capture());

        verify(cellCtlODataProducer, times(1)).beforeCreate(stringCaptor.capture(), entityCaptor.capture(),
                docHandlerCaptor.capture());
        assertThat(stringCaptor.getValue(), is(typeName));
        assertNull(entityCaptor.getValue());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(mockEsType, times(1)).create(stringCaptor.capture(), docHandlerCaptor.capture());
        confirmEntitySetDocHandler(expectedOedh, docHandlerCaptor.getValue());

        verify(cellCtlODataProducer, times(1)).afterCreate(stringCaptor.capture(), entityCaptor.capture(),
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
     * Test createEntity().
     * Error test.
     * setLinksFromOEntityKey fail.
     * @throws Exception Unexpected error
     */
    @Test
    public void createEntity_Error_setLinksFromOEntityKey_fail() throws Exception {
        cellCtlODataProducer = PowerMockito.spy(new CellCtlODataProducer(new CellEsImpl()));
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
        doReturn(mockEsType).when(cellCtlODataProducer).getAccessorForEntitySet(typeName);

        PowerMockito.doThrow(new NTKPNotFoundException("dummyMsg")).when(cellCtlODataProducer,
                "setLinksFromOEntityKey", anyObject(), anyString(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("createEntity",
                String.class, Map.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellCtlODataProducer, typeName, staticFields);
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
        doNothing().when(cellCtlODataProducer).setLinksForOedh(anyObject(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("setLinksFromOEntityKey",
                OEntityKey.class, String.class, EntitySetDocHandler.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellCtlODataProducer, key, typeName, oedh);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(cellCtlODataProducer, times(1)).setLinksForOedh(anyObject(), anyObject(), anyObject());
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
        doThrow(new NTKPNotFoundException("dummyMsg")).when(cellCtlODataProducer).setLinksForOedh(
                anyObject(), anyObject(), anyObject());

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = CellCtlODataProducer.class.getDeclaredMethod("setLinksFromOEntityKey",
                OEntityKey.class, String.class, EntitySetDocHandler.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellCtlODataProducer, key, typeName, oedh);
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
     * Test breakRelation().
     * Normal test.
     */
    @Test
    public void breakRelation_Normal() {
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(),
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");

        EntitySetDocHandler relation = new OEntityDocHandler();
        EntitySetDocHandler extCell = new OEntityDocHandler();
        doReturn(relation).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        doReturn(extCell).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");

        doReturn(true).when(cellCtlODataProducer).deleteLinkEntity(relation, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        cellCtlODataProducer.breakRelation(entitySetDocHandler);

        // --------------------
        // Confirm result
        // --------------------
        // Confirm function call
        verify(cellCtlODataProducer, times(1)).deleteLinkEntity(relation, extCell);
    }

    /**
     * Test breakRelation().
     * Error test.
     * relation is null.
     */
    @Test
    public void breakRelation_Error_relation_is_null() {
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(),
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");

        doReturn(null).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.breakRelation(entitySetDocHandler);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params("dummyRelation");
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test breakRelation().
     * Error test.
     * extCell is null.
     */
    @Test
    public void breakRelation_Error_extCell_is_null() {
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(),
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");

        EntitySetDocHandler relation = new OEntityDocHandler();
        doReturn(relation).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        doReturn(null).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.breakRelation(entitySetDocHandler);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage
                    .REQUEST_RELATION_TARGET_DOES_NOT_EXISTS.params("http://personium/dummyExtCell/");
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test breakRelation().
     * Error test.
     * deleteLinkEntity is false.
     */
    @Test
    public void breakRelation_Error_deleteLinkEntity_is_false() {
        // --------------------
        // Test method args
        // --------------------
        EntitySetDocHandler entitySetDocHandler = mock(EntitySetDocHandler.class);

        // --------------------
        // Mock settings
        // --------------------
        Map<String, Object> mockStaticFields = new HashMap<String, Object>();
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION.getName(),
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        mockStaticFields.put(ReceivedMessage.P_BOX_NAME.getName(), "dummyBoxName");
        mockStaticFields.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://personium/dummyExtCell/");
        doReturn(mockStaticFields).when(entitySetDocHandler).getStaticFields();

        doReturn("dummyRelation").when(cellCtlODataProducer).getRelationNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");
        doReturn("dummyBoxName").when(cellCtlODataProducer).getBoxNameFromRequestRelation(
                "http://personium/dummyAppCell/__relation/__/dummyRelation");

        EntitySetDocHandler relation = new OEntityDocHandler();
        EntitySetDocHandler extCell = new OEntityDocHandler();
        doReturn(relation).when(cellCtlODataProducer).getRelation("dummyRelation", "dummyBoxName");
        doReturn(extCell).when(cellCtlODataProducer).getExtCell("http://personium/dummyExtCell/");

        doReturn(false).when(cellCtlODataProducer).deleteLinkEntity(relation, extCell);

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            cellCtlODataProducer.breakRelation(entitySetDocHandler);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.ReceivedMessage.LINK_DOES_NOT_EXISTS.params(
                    "dummyRelation", "http://personium/dummyExtCell/");
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }
}
