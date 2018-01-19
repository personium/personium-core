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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.Privilege;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.test.categories.Unit;

/**
 * Unit Test class of CellCtlResource.
 */
@Category({Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessContext.class, ModelFactory.ODataCtl.class })
public class CellCtlResourceTest {

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is Account.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_Account() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Account";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is Role.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_Role() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Role";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is ExtRole.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_ExtRole() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ExtRole";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is Relation.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_Relation() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Relation";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.SOCIAL_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is ExtCell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_ExtCell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ExtCell";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.SOCIAL_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is Box.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_Box() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Box";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.BOX_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is ReceivedMessage.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_ReceivedMessage() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ReceivedMessage";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.MESSAGE_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is SentMessage.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_SentMessage() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "SentMessage";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.MESSAGE_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is Rule.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_Rule() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.RULE_READ;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = null;

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = null;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryReadPrivilege().
     * Normal test.
     * EntitySet is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryReadPrivilege_Normal_entityset_is_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "invalid";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = null;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryReadPrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is Account.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_Account() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Account";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is Role.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_Role() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Role";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is ExtRole.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_ExtRole() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ExtRole";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.AUTH;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is Relation.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_Relation() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Relation";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.SOCIAL;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is ExtCell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_ExtCell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ExtCell";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.SOCIAL;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is Box.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_Box() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Box";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.BOX;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is ReceivedMessage.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_ReceivedMessage() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "ReceivedMessage";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.MESSAGE;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is SentMessage.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_SentMessage() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "SentMessage";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.MESSAGE;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is Rule.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_Rule() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = CellPrivilege.RULE;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_null() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = null;

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = null;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test getNecessaryWritePrivilege().
     * Normal test.
     * EntitySet is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getNecessaryWritePrivilege_Normal_entityset_is_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "invalid";

        // --------------------
        // Expected result
        // --------------------
        Privilege expected = null;

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        Privilege priv = resource.getNecessaryWritePrivilege(entitySetName);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(priv, is(expected));
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is not Rule.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_entityset_is_not_Rule() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Role";

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, null);
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is log.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_log() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), null));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is log, object is localcell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_log_object_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is log, object is localbox.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_log_object_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is log, object is localunit.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_log_object_localunit() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localunit:/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is log, object is http.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_log_object_http() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "http://personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is log, object is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_log_object_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "/personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, boxname exists, action is log, object is localbox.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_boxname_action_log_object_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is log, object is localcell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_log_object_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is log, object is localunit.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_log_object_localunit() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localunit:/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is log, object is http.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_log_object_http() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "http:/personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is log, object is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_log_object_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "log"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "/personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay and service is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), null));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is relay, object is localcell and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_relay_object_localcell_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is relay, object is null and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_relay_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), null));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is localbox and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_localbox_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is localunit and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_localunit_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localunit:/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is http and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_http_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "http://personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is https and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_https_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "https://personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is invalid and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_invalid_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "/personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is relay, object is localcell and service is https.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_relay_object_localcell_service_https() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "https://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, action is relay, object is localcell and service is localunit.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_relay_object_localcell_service_localunit() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is localcell and service is localcell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_localcell_service_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localcell:/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is localcell and service is localbox.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_localcell_service_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localbox:/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is relay, object is localcell and service is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_relay_object_localcell_service_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "/personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, boxname exists, action is relay, object is localbox and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_boxname_action_relay_object_localbox_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, boxname exists, action is relay, object is null and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_boxname_action_relay_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), null));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is localcell and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_localcell_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is localunit and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_localunit_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localunit:/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is http and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_http_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "http://personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is https and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_https_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "https://personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is invalid and service is valid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_invalid_service_valid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "/personium/cell/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_OBJECT.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is relay, object is localbox and service is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_relay_object_localbox_service_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "relay"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "/personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, action is exec and service is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), null));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), null));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is localcell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_action_exec_object_valid_service_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localcell:/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is localbox.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec_object_valid_service_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localbox:/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is localunit.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec_object_valid_service_localunit() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is http.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec_object_valid_service_http() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is https.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec_object_valid_service_https() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "https:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname is null, action is exec, object is valid and service is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_action_exec_object_valid_service_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localcell:/box/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "/personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), null));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Normal test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is localbox.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Normal_boxname_action_exec_object_valid_service_localbox() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localbox:/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        resource.validate(entitySetName, props);
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is localcell.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_exec_object_valid_service_localcell() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localcell:/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is localunit.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_exec_object_valid_service_localunit() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "personium-localunit:/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is http.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_exec_object_valid_service_http() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "http://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is https.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_exec_object_valid_service_https() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "https://personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validate().
     * Error test.
     * EntitySet is Rule, boxname exists, action is exec, object is valid and service is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validate_Error_boxname_action_exec_object_valid_service_invalid() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        String rootUrl = "https://personium/cell/";
        String entitySetName = "Rule";
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        props.add(OProperties.string(Rule.P_ACTION.getName(), "exec"));
        props.add(OProperties.string(Rule.P_OBJECT.getName(), "personium-localbox:/col/entity"));
        props.add(OProperties.string(Rule.P_SERVICE.getName(), "/personium/cell/box/col/service"));
        props.add(OProperties.string(Common.P_BOX_NAME.getName(), "box"));

        // --------------------
        // Expected result
        // --------------------
        PersoniumCoreException expected =
                PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Rule.P_SERVICE.getName());

        // --------------------
        // Mock settings
        // --------------------
        Cell cell = mock(Cell.class);
        doReturn(rootUrl).when(cell).getUrl();
        AccessContext accessContext = mock(AccessContext.class);
        doReturn(cell).when(accessContext).getCell();
        CellCtlODataProducer producer = mock(CellCtlODataProducer.class);
        doReturn(null).when(producer).getMetadata();
        spy(ModelFactory.ODataCtl.class);
        doReturn(producer).when(ModelFactory.ODataCtl.class, "cellCtl", cell);

        // --------------------
        // Run method
        // --------------------
        CellCtlResource resource = new CellCtlResource(accessContext, null, null);
        try {
            resource.validate(entitySetName, props);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }
}
