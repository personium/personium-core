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
package com.fujitsu.dc.test.unit.core.model.impl.es.odata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.ctl.ReceivedMessagePort;
import com.fujitsu.dc.core.model.impl.es.CellEsImpl;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.CellCtlODataProducer;
import com.fujitsu.dc.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@Category({ Unit.class })
public class CellCtlODataProducerTest extends CellCtlODataProducer {
    static Cell cell = new CellEsImpl();

    /**
     * コンストラクタ.
     */
    public CellCtlODataProducerTest() {
        super(cell);
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_READの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_READの場合にバリデートエラーにならないこと() {
        assertTrue(isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_UNREADの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_UNREADの場合にバリデートエラーにならないこと() {
        assertTrue(isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void メッセージのバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(isValidMessageStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * メッセージのバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーにならないこと.
     */
    @Test
    public void メッセージのバリデートにて_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーにならないこと() {
        assertTrue(isValidMessageStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_NONEの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_NONEの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_READの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_READの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_UNREADの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BREAKかつSTATUS_UNREADの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_NONEの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_NONEの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_APPROVEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_READの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_READの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_READ));
    }

    /**
     * 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーになること.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_REQ_RELATION_BUILDかつSTATUS_UNREADの場合にバリデートエラーになること() {
        assertFalse(isValidRelationStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_UNREAD));
    }

    /**
     * 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと.
     */
    @Test
    public void 関係承認のバリデートにて_TYPE_MESSAGEかつSTATUS_REJECTEDの場合にバリデートエラーにならないこと() {
        assertTrue(isValidRelationStatus(
                ReceivedMessagePort.TYPE_MESSAGE,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * 現状ののバリデートにて_TYPE_REQ_RELATION_BUILDの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと() {
        assertTrue(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_NONEの場合にバリデートエラーにならないこと() {
        assertTrue(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_NONE));
    }

    /**
     * REQ_RELATION_BUILDの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること() {
        assertFalse(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * REQ_RELATION_BUILDの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BUILDの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BUILD,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_APPROVEDの場合にバリデートエラーになること() {
        assertFalse(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_APPROVED));
    }

    /**
     * REQ_RELATION_BREAKの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること.
     */
    @Test
    public void REQ_RELATION_BREAKの時に現ステータスがSTATUS_REJECTEDの場合にバリデートエラーになること() {
        assertFalse(isValidCurrentStatus(
                ReceivedMessagePort.TYPE_REQ_RELATION_BREAK,
                ReceivedMessagePort.STATUS_REJECTED));
    }

    /**
     * リレーションクラスURLからリレーション名を取得できること.
     */
    @Test
    public void リレーションクラスURLからリレーション名を取得できること() {
        String relationName = getRelationFromRelationClassUrl(
                "https://example.com/test0110/__relation/box/+:me");
        assertEquals("+:me", relationName);
    }

    /**
     * リレーションクラスURLのフォーマットが不正な場合はnullが取得できること.
     */
    @Test
    public void リレーションクラスURLのフォーマットが不正な場合はnullが取得できること() {
        String relationName = getRelationFromRelationClassUrl(
                "https://example.com/test0110/__relation/box/");
        assertEquals(null, relationName);
    }

    /**
     * elasticsearchに接続しないためのCellCtlODataProducerMockクラス.
     */
    public static class CellCtlODataProducerMock extends CellCtlODataProducer {
        static Cell cell = new CellEsImpl();
        private boolean getRelationNullFlag = false;
        private boolean getExtCellNullFlag = false;

        /**
         * コンストラクタ.
         */
        public CellCtlODataProducerMock() {
            super(cell);
        }

        /**
         * getRelationNullFlagのセッター.
         * @param getRelationNullFlag Nullで返却するか否か
         */
        public void setGetRelationNullFlag(boolean getRelationNullFlag) {
            this.getRelationNullFlag = getRelationNullFlag;
        }

        /**
         * getExtCellNullFlagのセッター.
         * @param getExtCellNullFlag Nullで返却するか否か
         */
        public void setGetExtCellNullFlag(boolean getExtCellNullFlag) {
            this.getExtCellNullFlag = getExtCellNullFlag;
        }

        @Override
        protected EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey) {
            return null;
        }

        @Override
        protected EntitySetDocHandler getRelation(String key) {
            if (getRelationNullFlag) {
                return null;
            } else {
                return new OEntityDocHandler();
            }
        }

        @Override
        protected EntitySetDocHandler getExtCell(String key) {
            if (getExtCellNullFlag) {
                return null;
            } else {
                return new OEntityDocHandler();
            }
        }

        /**
         * retrieveWithKeyの結果Nullを返すMock.
         * @param key 存在確認対象のExtCell
         * @return nullを返却
         */
        public EntitySetDocHandler getExtCellOfNull(String key) {
            return getExtCell(key);
        }

        /**
         * deleteLinkEntityの結果falseを返すMock.
         * @param source リンク元エンティティ
         * @param target リンク先エンティティ
         * @return nullを返却
         */
        @Override
        protected boolean deleteLinkEntity(EntitySetDocHandler source, EntitySetDocHandler target) {
            return false;
        }

        /**
         * retrieveWithKeyの結果Nullを返すMock.
         * @param entitySetDocHandler メッセージのDocHandler
         */
        public void breakRelationMock(EntitySetDocHandler entitySetDocHandler) {
            breakRelation(entitySetDocHandler);
        }
    }

    @Override
    protected EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey) {
        return new OEntityDocHandler();
    }

    /**
     * extCellの取得で存在する場合にEntitySetDocHandlerが返却されること.
     */
    @Test
    public void extCellの取得で存在する場合にEntitySetDocHandlerが返却されること() {
        assertTrue(getExtCell("https://example.com/test0110/") != null);
    }

    /**
     * リレーションクラスURLのフォーマットが不正な場合はnullが取得できること.
     */
    @Test
    public void extCellの存在確認でOEntityKeyのパースに失敗した場合はREQUEST_RELATION_TARGET_PARSE_ERRORが発生すること() {
        try {
            getExtCell("https://example.com/'/");
            fail("DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR does not occurred.");
        } catch (DcCoreException e) {
            DcCoreException expected = DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR;
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
            breakRelation(docHandler);
            fail("DcCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR does not occurred.");
        } catch (DcCoreException e) {
            DcCoreException expected = DcCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR;
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
        try {
            CellCtlODataProducerMock mock = new CellCtlODataProducerMock();
            mock.setGetRelationNullFlag(true);
            mock.breakRelationMock(docHandler);
            fail("DcCoreException.ReceiveMessage.REQUEST_RELATION_DOES_NOT_EXISTS does not occurred.");
        } catch (DcCoreException e) {
            DcCoreException expected = DcCoreException.ReceiveMessage.REQUEST_RELATION_DOES_NOT_EXISTS
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
        try {
            CellCtlODataProducerMock mock = new CellCtlODataProducerMock();
            mock.setGetExtCellNullFlag(true);
            mock.breakRelationMock(docHandler);
            fail("DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS does not occurred.");
        } catch (DcCoreException e) {
            DcCoreException expected = DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS
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
        try {
            CellCtlODataProducerMock mock = new CellCtlODataProducerMock();
            mock.breakRelationMock(docHandler);
            fail("DcCoreException.ReceiveMessage.LINK_DOES_NOT_EXISTS does not occurred.");
        } catch (DcCoreException e) {
            DcCoreException expected = DcCoreException.ReceiveMessage.LINK_DOES_NOT_EXISTS
                    .params("+:me", "https://example.com/test0110/");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }
}
