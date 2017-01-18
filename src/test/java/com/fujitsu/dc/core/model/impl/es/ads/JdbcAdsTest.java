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
package com.fujitsu.dc.core.model.impl.es.ads;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.impl.es.DavNode;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.test.categories.Unit;

/**
 * JdbcAdsユニットテストクラス.
 */
@Category({ Unit.class })
public class JdbcAdsTest {
    static Logger log = LoggerFactory.getLogger(JdbcAdsTest.class);

    // 本テストで作成・削除するRDBのSchema(Database)名。
    // 本番データを消すような事件が起こらぬよう、万が一に備えて衝突しづらい名前にしておく.
    static final String TEST_INDEX = "test_index_jipwoearewprie";

    /**
     * テストの前処理.
     * Indexに対応する空間、すなわちJDBCではSchema(Database)の新規作成.
     * @throws Exception Exception
     */
    @Before
    public void before() throws Exception {
        JdbcAds ads = new JdbcAds();
        ads.createIndex(TEST_INDEX);
    }

    /**
     * テストの後処理
     * Indexに対応する空間、すなわちJDBCではSchema(Database)の削除.
     * @throws Exception Exception
     */
    @After
    public void after() throws Exception {
        JdbcAds ads = new JdbcAds();
        ads.deleteIndex(TEST_INDEX);
    }

    /**
     * CELLに対応するADSのCRUD.
     * @throws Exception Exception
     */
    @Test
    public void CELLに対応するADSのCRUD() throws Exception {
        JdbcAds ads = new JdbcAds();
        OEntityDocHandler oedh = new OEntityDocHandler();
        String testId = DcUUID.randomUUID();
        oedh.setId(testId);
        oedh.setType(Cell.EDM_TYPE_NAME);
        oedh.setPublished(new Date().getTime());
        oedh.setUpdated(new Date().getTime());

        // 作成
        ads.createCell(TEST_INDEX, oedh);
        assertEquals(1, ads.countCell(TEST_INDEX));

        List<JSONObject> list = ads.getCellList(TEST_INDEX, 0, 10);
        assertEquals(1, list.size());

        // 更新
        oedh.setUpdated(new Date().getTime());
        ads.updateCell(TEST_INDEX, oedh);
        assertEquals(1, ads.countCell(TEST_INDEX));

        // ID検索
        List<String> idList = new ArrayList<String>();
        idList.add(oedh.getId());
        List<JSONObject> res = ads.searchCellList(TEST_INDEX, idList);
        assertEquals(1, res.size());

        // 削除
        ads.deleteCell(TEST_INDEX, oedh.getId());
        assertEquals(0, ads.countCell(TEST_INDEX));
    }

    /**
     * OEntityDocに対応するADSのCRUD.
     * @throws Exception Exception
     */
    @Test
    public void OEntityDocに対応するADSのCRUD() throws Exception {
        JdbcAds ads = new JdbcAds();
        OEntityDocHandler oedh = this.createTestOEntityDocHandler();
        // 作成
        ads.createEntity(TEST_INDEX, oedh);
        assertEquals(1, ads.countEntity(TEST_INDEX));

        List<JSONObject> list = ads.getEntityList(TEST_INDEX, 0, 10);
        assertEquals(1, list.size());

        // 更新
        oedh.setUpdated(new Date().getTime());
        ads.updateEntity(TEST_INDEX, oedh);
        assertEquals(1, ads.countEntity(TEST_INDEX));

        // ID検索
        List<String> idList = new ArrayList<String>();
        idList.add(oedh.getId());
        List<JSONObject> res = ads.searchEntityList(TEST_INDEX, idList);
        assertEquals(1, res.size());
        String id = (String) res.get(0).get("id");
        assertEquals(oedh.getId(), id);

        // 削除
        ads.deleteEntity(TEST_INDEX, oedh.getId());
        assertEquals(0, ads.countEntity(TEST_INDEX));
    }

    OEntityDocHandler createTestOEntityDocHandler() {
        OEntityDocHandler oedh = new OEntityDocHandler();
        String testId = DcUUID.randomUUID();
        oedh.setId(testId);
        oedh.setType(DcUUID.randomUUID());
        oedh.setCellId("cellid");
        oedh.setBoxId("boxid");
        oedh.setPublished(new Date().getTime());
        oedh.setUpdated(new Date().getTime());
        return oedh;
    }

    /**
     * LinkDocに対応するADSのCRUD.
     * @throws Exception Exception
     */
    @Test
    public void LinkDocに対応するADSのCRUD() throws Exception {
        JdbcAds ads = new JdbcAds();
        OEntityDocHandler oedh1 = this.createTestOEntityDocHandler();
        OEntityDocHandler oedh2 = this.createTestOEntityDocHandler();

        LinkDocHandler ldh = new LinkDocHandler(oedh1, oedh2);
        ldh.setPublished(new Date().getTime());
        ldh.setUpdated(new Date().getTime());
        // 作成
        ads.createLink(TEST_INDEX, ldh);
        assertEquals(1, ads.countLink(TEST_INDEX));
        List<JSONObject> list = ads.getLinkList(TEST_INDEX, 0, 10);
        assertEquals(1, list.size());
        log.debug("    Link Body:" + list.get(0).toJSONString());

        // 更新
        ldh.setUpdated(new Date().getTime());
        ads.updateLink(TEST_INDEX, ldh);
        assertEquals(1, ads.countLink(TEST_INDEX));

        // ID検索
        List<String> idList = new ArrayList<String>();
        idList.add(ldh.getId());
        List<JSONObject> res = ads.searchLinkList(TEST_INDEX, idList);
        assertEquals(1, res.size());
        String id = (String) res.get(0).get("id");
        assertEquals(ldh.getId(), id);

        // 削除
        ads.deleteLink(TEST_INDEX, ldh.getId());
        assertEquals(0, ads.countLink(TEST_INDEX));
    }

    /**
     * DavNodeに対応するADSのCRUD.
     * @throws Exception Exception
     */
    @Test
    public void DavNodeに対応するADSのCRUD() throws Exception {
        JdbcAds ads = new JdbcAds();
        // Mockの作成
        Cell cell = Mockito.mock(Cell.class);
        Mockito.when(cell.getName()).thenReturn("dummyCell");
        Mockito.when(cell.getId()).thenReturn("cellid");
        Mockito.when(cell.getUrl()).thenReturn("http://example.com/dummyCell");
        Mockito.when(cell.getOwner()).thenReturn("dummy_cell_owner");

        Box box = new Box(cell, null);
        box.setId("dummyBoxId");
        box.setName("dummyBoxName");

        // 作成
        DavNode davNode = new DavNode();
        davNode.setId(DcUUID.randomUUID());
        ads.createDavNode(TEST_INDEX, davNode);
        assertEquals(1, ads.countDavNode(TEST_INDEX));
        List<JSONObject> list = ads.getDavNodeList(TEST_INDEX, 0, 10);
        assertEquals(1, list.size());
        log.debug("    DavNode Body:" + list.get(0).toJSONString());

        // 更新
        davNode.setUpdated(new Date().getTime());
        ads.updateDavNode(TEST_INDEX, davNode);
        assertEquals(1, ads.countDavNode(TEST_INDEX));

        // ID検索
        List<String> idList = new ArrayList<String>();
        idList.add(davNode.getId());
        List<JSONObject> res = ads.searchDavNodeList(TEST_INDEX, idList);
        assertEquals(1, res.size());
        String id = (String) res.get(0).get("id");
        assertEquals(davNode.getId(), id);

        // 削除
        ads.deleteDavNode(TEST_INDEX, davNode.getId());
        assertEquals(0, ads.countDavNode(TEST_INDEX));
    }

}
