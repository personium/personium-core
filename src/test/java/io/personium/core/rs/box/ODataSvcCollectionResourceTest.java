/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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
import org.powermock.api.mockito.PowerMockito;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.test.categories.Unit;

/**
 * ODataSvcCollectionResource unit test classs.
 */
@Category({ Unit.class })
public class ODataSvcCollectionResourceTest {

    /** Target class of unit test. */
    private ODataSvcCollectionResource odataSvcCollectionResource;

    /**
     * Test constructor.
     */
    @Test
    public void constructor_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box";
        String name = "col";
        String cellUrl = "https://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);
        Cell cell = mock(Cell.class);
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(name).when(davCmp).getName();
        doReturn(producer).when(davCmp).getODataProducer();
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = "personium-localcell:/box/" + name + "/";

        // --------------------
        // Run method
        // --------------------
        odataSvcCollectionResource = new ODataSvcCollectionResource(davRsCmp, davCmp);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(odataSvcCollectionResource.getRootUrl(), is(expected));
    }

    /**
     * Test deleteEntitySetNameFromOp().
     * Normal test.
     * op is links.Entity.create
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void deleteEntitySetNameFromOp_Normal_op_is_links() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box";
        String name = "col";
        String cellUrl = "https://personium/cell/";
        String op = "links.Entity.create";

        // --------------------
        // Mock settings
        // --------------------
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);
        Cell cell = mock(Cell.class);
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(name).when(davCmp).getName();
        doReturn(producer).when(davCmp).getODataProducer();
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = "links.create";

        // --------------------
        // Run method
        // --------------------
        ODataSvcCollectionResource resource = new ODataSvcCollectionResource(davRsCmp, davCmp);
        Method method = ODataSvcCollectionResource.class.getDeclaredMethod("deleteEntitySetNameFromOp", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(resource, op);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test deleteEntitySetNameFromOp().
     * Normal test.
     * op is navprop.Entity.list
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void deleteEntitySetNameFromOp_Normal_op_is_navprop() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box";
        String name = "col";
        String cellUrl = "https://personium/cell/";
        String op = "navprop.Entity.list";

        // --------------------
        // Mock settings
        // --------------------
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);
        Cell cell = mock(Cell.class);
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(name).when(davCmp).getName();
        doReturn(producer).when(davCmp).getODataProducer();
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = "navprop.list";

        // --------------------
        // Run method
        // --------------------
        ODataSvcCollectionResource resource = new ODataSvcCollectionResource(davRsCmp, davCmp);
        Method method = ODataSvcCollectionResource.class.getDeclaredMethod("deleteEntitySetNameFromOp", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(resource, op);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test deleteEntitySetNameFromOp().
     * Normal test.
     * op is update
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void deleteEntitySetNameFromOp_Normal_op_is_update() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box";
        String name = "col";
        String cellUrl = "https://personium/cell/";
        String op = "update";

        // --------------------
        // Mock settings
        // --------------------
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);
        Cell cell = mock(Cell.class);
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(name).when(davCmp).getName();
        doReturn(producer).when(davCmp).getODataProducer();
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = "update";

        // --------------------
        // Run method
        // --------------------
        ODataSvcCollectionResource resource = new ODataSvcCollectionResource(davRsCmp, davCmp);
        Method method = ODataSvcCollectionResource.class.getDeclaredMethod("deleteEntitySetNameFromOp", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(resource, op);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

}
