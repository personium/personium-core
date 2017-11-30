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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.es.EsClient;
import io.personium.common.es.EsIndex;
import io.personium.common.es.impl.EsIndexImpl;
import io.personium.core.model.impl.es.EsModel;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for CellAccessor.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({EsClient.class, EsModel.class})
@Category({Unit.class })
public class CellAccessorTest {

    /** Test class. */
    private CellAccessor cellAccessor;

    /**
     * Test bulkDeleteODataCollection().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void bulkDeleteODataCollection_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String cellId = "cellId";
        String boxId = "boxId";
        String nodeId = "nodeId";
        String unitUserName = "unitUserName";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.whenNew(EsClient.class).withAnyArguments().thenReturn(null);
        PowerMockito.mockStatic(EsModel.class);
        PowerMockito.doReturn(null).when(EsModel.class, "type", "", "", "", 0, 0);

        EsIndex index = new EsIndexImpl("", "", 0, 0, null);
        cellAccessor = spy(new CellAccessor(index, "", ""));

        DataSourceAccessor accessor = mock(DataSourceAccessor.class);
        PowerMockito.doReturn(accessor).when(EsModel.class, "dsa", unitUserName);

        doNothing().when(accessor).deleteByQuery(anyString(), any());

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        cellAccessor.bulkDeleteODataCollection(cellId, boxId, nodeId, unitUserName);

        // --------------------
        // Confirm result
        // --------------------
        ArgumentCaptor<String> cellIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> deleteQueryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(accessor, times(1)).deleteByQuery(cellIdCaptor.capture(), deleteQueryCaptor.capture());

        Map<String, Object> deleteQuery = deleteQueryCaptor.getValue();
        assertThat(cellIdCaptor.getValue(), is(cellId));
        assertThat(deleteQuery.get("c"), is(cellId));
        assertThat(deleteQuery.get("b"), is(boxId));
        assertThat(deleteQuery.get("n"), is(nodeId));
    }
}
