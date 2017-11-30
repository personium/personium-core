/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        DavCmp davCmp = mock(DavCmp.class);
        PersoniumODataProducer producer = mock(PersoniumODataProducer.class);

        // --------------------
        // Test method args
        // --------------------
        String url = "https://personium/cell/box";
        String name = "col";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(name).when(davCmp).getName();
        doReturn(producer).when(davCmp).getODataProducer();
        doReturn(null).when(producer).getMetadata();

        // --------------------
        // Expected result
        // --------------------
        String expected = url + "/" + name + "/";

        // --------------------
        // Run method
        // --------------------
        odataSvcCollectionResource = new ODataSvcCollectionResource(davRsCmp, davCmp);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(odataSvcCollectionResource.getRootUrl(), is(expected));
    }

}
