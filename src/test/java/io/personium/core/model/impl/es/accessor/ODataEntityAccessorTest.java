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
package io.personium.core.model.impl.es.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.es.EsClient;
import io.personium.common.es.EsIndex;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.unit.core.UrlUtils;

/**
 * ODataEntityAccessorTestの単体テストケース.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class })
public class ODataEntityAccessorTest {

    private static final String INDEX_NAME = "index_for_test";
    private static final String TYPE_NAME = "TypeForTest";
    private static final String ROUTING_ID = "RoutingIdTest";

    private static EsClient esClient;

    /**
     * 各テスト実行前の初期化処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @Before
    public void setUp() throws Exception {
        esClient = new EsClient(PersoniumUnitConfig.getEsClusterName(), PersoniumUnitConfig.getEsHosts());
    }

    /**
     * 各テスト実行後のクリーンアップ処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @After
    public void tearDown() throws Exception {
        String esUnitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        EsIndex index = esClient.idxUser(esUnitPrefix, INDEX_NAME);
        try {
            index.delete();
        } catch (Exception ex) {
            System.out.println("");
        }
    }

    /**
     * create処理が正常に終了する.
     */
    @Test
    public void create処理が正常に終了する() {
        // 事前準備
        String esUnitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        EsIndex index = esClient.idxUser(esUnitPrefix, INDEX_NAME);
        assertNotNull(index);
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();

        // データ登録実行
        PersoniumIndexResponse response = entityAccessor.create(docHandler);

        // レスポンスのチェック
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // データを削除する
        entityAccessor.delete(docHandler);
    }

    /**
     * update処理が正常に終了する.
     */
    @Test
    public void update処理が正常に終了する() {
        // 事前準備
        String esUnitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        EsIndex index = esClient.idxUser(esUnitPrefix, INDEX_NAME);
        assertNotNull(index);
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();
        PersoniumIndexResponse createResponse = entityAccessor.create(docHandler);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));

        // データ更新実行
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("test", "testdata");
        docHandler.setStaticFields(staticFields);

        PersoniumIndexResponse updateResponse = entityAccessor.update(createResponse.getId(), docHandler);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(createResponse.getId(), updateResponse.getId());

        // データを削除する
        entityAccessor.delete(docHandler);
    }

    /**
     * delete処理が正常に終了する.
     */
    @Test
    public void delete処理が正常に終了する() {
        // 事前準備
        String esUnitPrefix = PersoniumUnitConfig.getEsUnitPrefix();
        EsIndex index = esClient.idxUser(esUnitPrefix, INDEX_NAME);
        assertNotNull(index);
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();
        PersoniumIndexResponse response = entityAccessor.create(docHandler);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // データを削除する
        entityAccessor.delete(docHandler);
    }

    /**
     * OEntityDocHandlerを生成する.
     * @return
     */
    private OEntityDocHandler createTestOEntityDocHandler() {
        long dateTime = new Date().getTime();
        Map<String, Object> dynamicField = new HashMap<String, Object>();
        Map<String, Object> staticFields = new HashMap<String, Object>();
        Map<String, Object> hiddenFields = new HashMap<String, Object>();
        Map<String, Object> link = new HashMap<String, Object>();
        OEntityDocHandler docHandler = new OEntityDocHandler();
        docHandler.setType("testType");
        docHandler.setCellId("testCellId");
        docHandler.setBoxId("testBoxId");
        docHandler.setNodeId("testNodeId");
        docHandler.setPublished(dateTime);
        docHandler.setUpdated(dateTime);
        docHandler.setDynamicFields(dynamicField);
        docHandler.setStaticFields(staticFields);
        docHandler.setHiddenFields(hiddenFields);
        docHandler.setManyToOnelinkId(link);
        String url = UrlUtils.getBaseUrl() + "#" + INDEX_NAME;
        hiddenFields.put("Owner", url);
        docHandler.resolveUnitUserName(hiddenFields);
        return docHandler;
    }
}
