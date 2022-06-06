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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.es.EsIndex;
import io.personium.common.es.impl.EsIndexImpl;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.response.impl.PersoniumSearchResponseImpl;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for CellDataAccessor.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ CellDataAccessor.class })
@Category({Unit.class })
public class CellDataAccessorTest {

    /** Test class. */
    private CellDataAccessor cellDataAccessor;

    /**
     * Test bulkDeleteBox().
     * normal.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void bulkDeleteBox_Normal() {
        // Test method args
        String boxId = "boxId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = spy(new CellDataAccessor(index, cellId));

        doNothing().when(cellDataAccessor).deleteByQuery(anyString(), any());

        // Expected result
        String expectedQuery = "{query="
                + "{filtered="
                + "{filter="
                + "{and="
                + "{filters="
                + "["
                + "{term={c=cellId}}, {term={b=boxId}}"
                + "]"
                + "}}, "
                + "query={match_all={}}"
                + "}}}";

        // Run method
        cellDataAccessor.bulkDeleteBox(boxId);

        // Confirm result
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cellDataAccessor, times(1)).deleteByQuery(cellIdCaptor.capture(), queryCaptor.capture());
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(queryCaptor.getValue().toString(), is(expectedQuery));
    }

    /**
     * Test deleteBoxLinkData().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void deleteBoxLinkData_Normal() throws Exception {
        // Test method args
        String boxId = "boxId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = PowerMockito.spy(new CellDataAccessor(index, cellId));

        PowerMockito.doNothing().when(cellDataAccessor, "deleteExtRoleLinkedToBox", boxId);

        doNothing().when(cellDataAccessor).deleteByQuery(anyString(), any());

        // Expected result
        String expectedQuery = "{query="
                + "{filtered="
                + "{filter="
                + "{and="
                + "{filters="
                + "["
                + "{term={c=cellId}}, {term={l.Box=boxId}}"
                + "]"
                + "}}, "
                + "query={match_all={}}"
                + "}}}";

        // Run method
        cellDataAccessor.deleteBoxLinkData(boxId);

        // Confirm result
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cellDataAccessor, times(1)).deleteByQuery(cellIdCaptor.capture(), queryCaptor.capture());
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(queryCaptor.getValue().toString(), is(expectedQuery));
    }

    /**
     * Test bulkDeleteODataCollection().
     * normal.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void bulkDeleteODataCollection_Normal() {
        // Test method args
        String boxId = "boxId";
        String nodeId = "nodeId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = spy(new CellDataAccessor(index, cellId));

        doNothing().when(cellDataAccessor).deleteByQuery(anyString(), any());

        // Expected result
        String expectedQuery = "{query="
                + "{filtered="
                + "{filter="
                + "{and="
                + "{filters="
                + "["
                + "{term={c=cellId}}, {term={b=boxId}}, {term={n=nodeId}}"
                + "]"
                + "}}, "
                + "query={match_all={}}"
                + "}}}";

        // Run method
        cellDataAccessor.bulkDeleteODataCollection(boxId, nodeId);

        // Confirm result
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cellDataAccessor, times(1)).deleteByQuery(cellIdCaptor.capture(), queryCaptor.capture());
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(queryCaptor.getValue().toString(), is(expectedQuery));
    }

    /**
     * Test deleteExtRoleLinkedToBox().
     * normal.
     * RelationLinkedToBox is not found.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void deleteExtRoleLinkedToBox_Normal_not_found_RelationLinkedToBox() throws Exception {
        // Test method args
        String boxId = "boxId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = PowerMockito.spy(new CellDataAccessor(index, cellId));

        PersoniumSearchResponse response = mock(PersoniumSearchResponse.class);
        PersoniumSearchHits searchHits = mock(PersoniumSearchHits.class);
        PowerMockito.doReturn(response).when(cellDataAccessor, "searchRelationLinkedToBox", boxId);
        doReturn(searchHits).when(response).getHits();
        doReturn(0L).when(searchHits).getCount();

        // Load methods for private
        Method method = CellDataAccessor.class.getDeclaredMethod("deleteExtRoleLinkedToBox", String.class);
        method.setAccessible(true);

        // Run method
        method.invoke(cellDataAccessor, boxId);

        // Confirm result
        verify(cellDataAccessor, times(0)).deleteByQuery(anyString(), anyMap());
    }

    /**
     * Test deleteExtRoleLinkedToBox().
     * normal.
     * RelationLinkedToBox is found.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void deleteExtRoleLinkedToBox_Normal_found_RelationLinkedToBox() throws Exception {
        // Test method args
        String boxId = "boxId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = PowerMockito.spy(new CellDataAccessor(index, cellId));

        PersoniumSearchResponse response = mock(PersoniumSearchResponse.class);
        PersoniumSearchHits searchHits = mock(PersoniumSearchHits.class);
        PowerMockito.doReturn(response).when(cellDataAccessor, "searchRelationLinkedToBox", boxId);
        doReturn(searchHits).when(response).getHits();
        doReturn(2L).when(searchHits).getCount();

        PersoniumSearchHit hit1 = mock(PersoniumSearchHit.class);
        PersoniumSearchHit hit2 = mock(PersoniumSearchHit.class);
        PersoniumSearchHit[] hits = {hit1, hit2};
        doReturn(hits).when(searchHits).getHits();
        doReturn("relationId001").when(hit1).getId();
        doReturn("relationId002").when(hit2).getId();

        doNothing().when(cellDataAccessor).deleteByQuery(anyString(), any());

        // Expected result
        String expectedQuery = "{query="
                + "{filtered="
                + "{filter="
                + "{and="
                + "{filters="
                + "["
                + "{term={_type=ExtRole}}, {term={c=cellId}}, "
                + "{bool={should=["
                + "{term={l.Relation=relationId001}}, {term={l.Relation=relationId002}}"
                + "]}}"
                + "]"
                + "}}, "
                + "query={match_all={}}"
                + "}}}";

        // Load methods for private
        Method method = CellDataAccessor.class.getDeclaredMethod("deleteExtRoleLinkedToBox", String.class);
        method.setAccessible(true);

        // Run method
        method.invoke(cellDataAccessor, boxId);

        // Confirm result
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cellDataAccessor, times(1)).deleteByQuery(cellIdCaptor.capture(), queryCaptor.capture());
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(queryCaptor.getValue().toString(), is(expectedQuery));
    }

    /**
     * Test searchRelationLinkedToBox().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void searchRelationLinkedToBox_Normal() throws Exception {
        // Test method args
        String boxId = "boxId";

        // Mock settings
        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        String cellId = "cellId";
        cellDataAccessor = spy(new CellDataAccessor(index, cellId));

        PersoniumSearchResponse response = PowerMockito.mock(PersoniumSearchResponseImpl.class);
        doReturn(response).when(cellDataAccessor).searchForIndex(anyString(), any());

        // Expected result
        String expectedQuery = "{query="
                + "{filtered="
                + "{filter="
                + "{and="
                + "{filters="
                + "["
                + "{term={_type=Relation}}, {term={c=cellId}}, {term={l.Box=boxId}}"
                + "]"
                + "}}, "
                + "query={match_all={}}"
                + "}}}";

        // Load methods for private
        Method method = CellDataAccessor.class.getDeclaredMethod("searchRelationLinkedToBox", String.class);
        method.setAccessible(true);

        // Run method
        PersoniumSearchResponse actual = (PersoniumSearchResponse) method.invoke(cellDataAccessor, boxId);

        // Confirm result
        assertThat(actual, is(response));
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cellDataAccessor, times(1)).searchForIndex(cellIdCaptor.capture(), queryCaptor.capture());
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(queryCaptor.getValue().toString(), is(expectedQuery));
    }
}
