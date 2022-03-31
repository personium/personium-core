/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.es.EsIndex;
import io.personium.common.es.impl.InternalEsClient;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.test.categories.Unit;
import io.personium.test.utils.UrlUtils;
/**
 * Unit test for ODataEntityAccessor.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(InternalEsClient.class)
@Category({Unit.class })
public class ODataEntityAccessorTest {

    private static final String INDEX_NAME = "index_for_test";
    private static final String TYPE_NAME = "userdata";
    private static final String ROUTING_ID = "RoutingIdTest";

    private static EsIndex index;

    @BeforeClass
    public static void beforeClass() {
        PowerMockito.spy(InternalEsClient.class);
        PowerMockito.mockStatic(InternalEsClient.class);
        InternalEsClient mockClient = PowerMockito.mock(InternalEsClient.class);
        PowerMockito.when(InternalEsClient.getInstance(anyString(), anyString())).thenReturn(mockClient);
        PowerMockito.when(mockClient.createIndex(anyString(), any())).thenReturn(new PlainActionFuture<CreateIndexResponse>() {
            @Override
            public CreateIndexResponse get() {
                CreateIndexResponse ret = PowerMockito.mock(CreateIndexResponse.class);
                return ret;
            }
        });
        PowerMockito.when(mockClient.asyncIndex(anyString(), anyString(), anyString(), anyString(), any(), any(), anyLong()))
            .thenReturn(new PlainActionFuture<IndexResponse>() {
            @Override
            public IndexResponse get() {
                IndexResponse ret = PowerMockito.mock(IndexResponse.class);
                PowerMockito.when(ret.getId()).thenReturn("12");
                return ret;
            }
        });
        PowerMockito.when(mockClient.asyncDelete(anyString(), anyString(), anyString(), anyString(), anyLong()))
        .thenReturn(new PlainActionFuture<DeleteResponse>() {
            @Override
            public DeleteResponse get() {
                DeleteResponse ret = PowerMockito.mock(DeleteResponse.class);
                //PowerMockito.when(ret.getId()).thenReturn("12");
                return ret;
            }
        });

        index = EsModel.idxUserWithUnitPrefix("test");
    }

    /**
     * create処理が正常に終了する.
     */
    @Test
    public void create処理が正常に終了する()  {
        // Preparation
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();

        // Create a data record
        PersoniumIndexResponse response = entityAccessor.create(docHandler);

        // check the response
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // delete the data
        entityAccessor.delete(docHandler);
    }

    /**
     * update処理が正常に終了する.
     */
    @Test
    public void update処理が正常に終了する() {
        // Preparation
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();
        PersoniumIndexResponse createResponse = entityAccessor.create(docHandler);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));

        // Data Update
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put("test", "testdata");
        docHandler.setStaticFields(staticFields);

        PersoniumIndexResponse updateResponse = entityAccessor.update(createResponse.getId(), docHandler);

        // Check the response
        assertNotNull(updateResponse);
        assertEquals(createResponse.getId(), updateResponse.getId());

        // Delete the data
        entityAccessor.delete(docHandler);
    }

    /**
     * delete処理が正常に終了する.
     */
    @Test
    public void delete処理が正常に終了する() {
        // Preparation
        ODataEntityAccessor entityAccessor = new ODataEntityAccessor(index, TYPE_NAME, ROUTING_ID);
        OEntityDocHandler docHandler = createTestOEntityDocHandler();
        PersoniumIndexResponse response = entityAccessor.create(docHandler);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // Delete the data
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
