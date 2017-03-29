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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmEntitySet;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CellCtlODataProducer.class, Box.class})
@Category({ Unit.class })
public class CellCtlODataProducerTest {

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
     * リレーションクラスURLからリレーション名を取得できること.
     */
    @Test
    public void リレーションクラスURLからリレーション名を取得できること() {
        String relationName = cellCtlODataProducer.getRelationFromRelationClassUrl(
                "https://example.com/test0110/__relation/box/+:me");
        assertEquals("+:me", relationName);
    }

    /**
     * リレーションクラスURLのフォーマットが不正な場合はnullが取得できること.
     */
    @Test
    public void リレーションクラスURLのフォーマットが不正な場合はnullが取得できること() {
        String relationName = cellCtlODataProducer.getRelationFromRelationClassUrl(
                "https://example.com/test0110/__relation/box/");
        assertEquals(null, relationName);
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
            fail("PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR;
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * RequestRelationが不正な場合はREQUEST_RELATION_PARSE_ERRORが発生すること.
     */
    @Test
    public void RequestRelationが不正な場合はREQUEST_RELATION_PARSE_ERRORが発生すること() {
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION.getName(),
                "https://example.com/test0110/__relation/box/");
        docHandler.setStaticFields(staticFields);
        try {
            cellCtlODataProducer.breakRelation(docHandler);
            fail("PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR;
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * RequestRelationが不正な場合はREQUEST_RELATION_DOES_NOT_EXISTSが発生すること.
     */
    @Test
    public void RequestRelationが存在しない場合はREQUEST_RELATION_DOES_NOT_EXISTSが発生すること() {
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION.getName(),
                "https://example.com/test0110/__relation/box/+:me");
        docHandler.setStaticFields(staticFields);
        doReturn(null).when(cellCtlODataProducer).getRelation(anyString(), anyString());
        try {
            cellCtlODataProducer.breakRelation(docHandler);
            fail("PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_DOES_NOT_EXISTS does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_DOES_NOT_EXISTS
                    .params("+:me");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * RequestRelationTargetが存在しない場合はREQUEST_RELATION_TARGET_DOES_NOT_EXISTSが発生すること.
     */
    @Test
    public void RequestRelationTargetが存在しない場合はREQUEST_RELATION_TARGET_DOES_NOT_EXISTSが発生すること() {
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION.getName(),
                "https://example.com/test0110/__relation/box/+:me");
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION_TARGET.getName(),
                "https://example.com/test0110/");
        docHandler.setStaticFields(staticFields);
        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).getRelation(anyString(), anyString());
        doReturn(null).when(cellCtlODataProducer).getExtCell(anyString());
        try {
            cellCtlODataProducer.breakRelation(docHandler);
            fail("PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected =
                    PersoniumCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS
                    .params("https://example.com/test0110/");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * Link情報が存在しない場合はLINK_DOES_NOT_EXISTSが発生すること.
     */
    @Test
    public void Link情報が存在しない場合はLINK_DOES_NOT_EXISTSが発生すること() {
        OEntityDocHandler docHandler = new OEntityDocHandler();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION.getName(),
                "https://example.com/test0110/__relation/box/+:me");
        staticFields.put(ReceivedMessagePort.P_REQUEST_RELATION_TARGET.getName(),
                "https://example.com/test0110/");
        docHandler.setStaticFields(staticFields);
        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).getRelation(anyString(), anyString());
        doReturn(new OEntityDocHandler()).when(cellCtlODataProducer).getExtCell(anyString());
        doReturn(false).when(cellCtlODataProducer).deleteLinkEntity(anyObject(), anyObject());
        try {
            cellCtlODataProducer.breakRelation(docHandler);
            fail("PersoniumCoreException.ReceiveMessage.LINK_DOES_NOT_EXISTS does not occurred.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.ReceiveMessage.LINK_DOES_NOT_EXISTS
                    .params("+:me", "https://example.com/test0110/");
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
        Map<String, Object> dynamicFields = new HashMap<String, Object>();
        dynamicFields.put(pBoxName, "box1");
        dynamicFields.put("dummy", "dummy1");
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setDynamicFields(dynamicFields);
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
        Map<String, Object> expectedDynamic = new HashMap<String, Object>();
        expectedDynamic.put("dummy", "dummy1");
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
        dynamicFields = docHandler.getDynamicFields();
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(dynamicFields.get(pBoxName), is(expectedDynamic.get(pBoxName)));
        assertThat(dynamicFields.get("dummy"), is(expectedDynamic.get("dummy")));
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
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
        Map<String, Object> dynamicFields = new HashMap<String, Object>();
        dynamicFields.put(pBoxName, "box1");
        dynamicFields.put("dummy", "dummy1");
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setDynamicFields(dynamicFields);
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
        Map<String, Object> expectedDynamic = new HashMap<String, Object>();
        expectedDynamic.put("dummy", "dummy1");
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
        dynamicFields = docHandler.getDynamicFields();
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(dynamicFields.get(pBoxName), is(expectedDynamic.get(pBoxName)));
        assertThat(dynamicFields.get("dummy"), is(expectedDynamic.get("dummy")));
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
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
        Map<String, Object> dynamicFields = new HashMap<String, Object>();
        dynamicFields.put(pBoxName, "box1");
        dynamicFields.put("dummy", "dummy1");
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setDynamicFields(dynamicFields);
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
        Map<String, Object> expectedDynamic = new HashMap<String, Object>();
        expectedDynamic.put(pBoxName, "box1");
        expectedDynamic.put("dummy", "dummy1");
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
        dynamicFields = docHandler.getDynamicFields();
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(dynamicFields.get(pBoxName), is(expectedDynamic.get(pBoxName)));
        assertThat(dynamicFields.get("dummy"), is(expectedDynamic.get("dummy")));
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
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
        Map<String, Object> dynamicFields = new HashMap<String, Object>();
        dynamicFields.put(pBoxName, "box1");
        dynamicFields.put("dummy", "dummy1");
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setDynamicFields(dynamicFields);
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
        Map<String, Object> expectedDynamic = new HashMap<String, Object>();
        expectedDynamic.put(pBoxName, "box1");
        expectedDynamic.put("dummy", "dummy1");
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
        dynamicFields = docHandler.getDynamicFields();
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(dynamicFields.get(pBoxName), is(expectedDynamic.get(pBoxName)));
        assertThat(dynamicFields.get("dummy"), is(expectedDynamic.get("dummy")));
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
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
        Map<String, Object> dynamicFields = new HashMap<String, Object>();
        dynamicFields.put(pBoxName, "box1");
        dynamicFields.put("dummy", "dummy1");
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(pBoxName, "box2");
        staticFields.put("dummy", "dummy2");
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("dummy", "dummy3");
        docHandler.setDynamicFields(dynamicFields);
        docHandler.setStaticFields(staticFields);
        docHandler.setManyToOnelinkId(link);

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        Map<String, Object> expectedDynamic = new HashMap<String, Object>();
        expectedDynamic.put(pBoxName, "box1");
        expectedDynamic.put("dummy", "dummy1");
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
        dynamicFields = docHandler.getDynamicFields();
        staticFields = docHandler.getStaticFields();
        link = docHandler.getManyToOnelinkId();
        assertThat(dynamicFields.get(pBoxName), is(expectedDynamic.get(pBoxName)));
        assertThat(dynamicFields.get("dummy"), is(expectedDynamic.get("dummy")));
        assertThat(staticFields.get(pBoxName), is(expectedStatic.get(pBoxName)));
        assertThat(staticFields.get("dummy"), is(expectedStatic.get("dummy")));
        assertThat(link.get("Box"), is(expectedLink.get("Box")));
        assertThat(link.get("dummy"), is(expectedLink.get("dummy")));
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
        EdmEntitySet entitySet = null;
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
    }
}
