/**
 * Personium
 * Copyright 2017-2019 Personium Project
 *  - FUJITSU LIMITED
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.test.categories.Unit;

/**
 * ODataSvcCollectionResource unit test classs.
 */
@Category({ Unit.class })
public class NullResourceTest {

    /** Target class of unit test. */
    private NullResource nullResource;

    @Before
    public void before() {
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
        Cell cell = mock(Cell.class);
        doReturn(null).when(davRsCmp).getAccessContext();
        doReturn(url).when(davRsCmp).getUrl();
        doReturn(cell).when(davRsCmp).getCell();
        doReturn(cellUrl).when(cell).getUrl();
        doReturn(name).when(davCmp).getName();
        nullResource = new NullResource(davRsCmp, davCmp, false);

    }

    /**
     *
     */
    @Test
    public void put_With_IfMatchWildCard_ShouldReturn_412() {
        try {
            nullResource.put("text/html", "*", null);
            fail();
        }catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Dav.NO_ENTITY_MATCH.getCode(), e.getCode());
        }
    }

    /**
     * delete_ShouldReturn_404
     */
    @Test
    public void delete_ShouldReturn_404() {
        try {
            nullResource.delete(null);
            fail();
        }catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.getCode(), e.getCode());
        }
    }
    /**
     * delete_With_IfMatchWildCard_ShouldReturn_412
     */
    @Test
    public void delete_With_IfMatchWildCard_ShouldReturn_412() {
        try {
            nullResource.delete("*");
            fail();
        }catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Dav.NO_ENTITY_MATCH.getCode(), e.getCode());
        }
    }
}
