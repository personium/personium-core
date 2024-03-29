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
package io.personium.core.rs.box;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.test.categories.Unit;

/**
 * ODataSvcSchemaResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ODataSvcCollectionResource.class })
@Category({ Unit.class })
public class ODataSvcSchemaResourceTest {

    /** Target class of unit test. */
    private ODataSvcSchemaResource odataSvcSchemaResource;

    /**
     * Test constructor.
     */
    @Test
    public void constructor_Normal() {
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);
        ODataSvcCollectionResource odataSvcCollectionResource = mock(ODataSvcCollectionResource.class);
        Cell cell = mock(Cell.class);

        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box/col";
        String cellUrl = "https://personium/cell/";
        String name = "$metadata";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(davCmp).when(davRsCmp).getDavCmp();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(producer).when(davCmp).getSchemaODataProducer(cell);
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = "personium-localcell:/box/col/" + name + "/";

        // --------------------
        // Run method
        // --------------------
        odataSvcSchemaResource = new ODataSvcSchemaResource(davRsCmp, odataSvcCollectionResource);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(odataSvcSchemaResource.getRootUrl(), is(expected));
    }

}
