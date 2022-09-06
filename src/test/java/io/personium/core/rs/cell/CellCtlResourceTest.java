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
package io.personium.core.rs.cell;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.Privilege;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.test.categories.Unit;

/**
 * Unit Test class of CellCtlResource.
 */
@Category({ Unit.class })
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
     * Test validateRule().
     * Normal test.
     * action is log.
     */
    @Test
    public void validateRule_action_log() {
        String action = "log";

        // object is null.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                null,
                                                false),
                   is((String) null));

        // object is localcell.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                false),
                   is((String) null));

        // object is localbox.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is localunit.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localunit:/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is http.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "http://personium/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "/personium/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // boxname exists, object is localbox.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                null,
                                                true),
                   is((String) null));

        // boxname exists, object is localcell.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                true),
                   is(Rule.P_OBJECT.getName()));

        // boxname exists, object is localunit.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localunit:/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                true),
                   is(Rule.P_OBJECT.getName()));

        // boxname exists, object is http.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "http:/personium/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                true),
                   is(Rule.P_OBJECT.getName()));

        // boxname exists, object is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "/personium/cell/box/col/entity",
                                                null,
                                                action,
                                                null,
                                                true),
                   is(Rule.P_OBJECT.getName()));

    }

    /**
     * Test validateRule().
     * Normal test.
     * action is relay.
     */
    @Test
    public void validateRule_Normal_action_relay() {
        String action = "relay";

        // service is null.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                null,
                                                true),
                   is(Rule.P_TARGETURL.getName()));
        // object is localcell and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is((String) null));
        // object is null and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "http://personium/cell/box/dir/dir/col/service#box/dir/dir/col/service",
                                                false),
                   is((String) null));
        // object is localbox and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_OBJECT.getName()));
        // object is localunit and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localunit:/cell/box/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_OBJECT.getName()));
        // object is http and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "http://personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_OBJECT.getName()));
        // object is https and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "https://personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_OBJECT.getName()));
        // object is invalid and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "/personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_OBJECT.getName()));
        // object is localcell and service is https.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "https://personium/path/cell/box/dir/col/service#box/dir/col/service",
                                                false),
                   is((String) null));
        // object is localcell and service is localunit.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/dir/dir/dir/col/service",
                                                false),
                   is((String) null));
        // object is localcell and service is localcell.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localcell:/box/dir/col/service",
                                                false),
                   is((String) null));
        // object is localcell and service is localbox.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localbox:/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));
        // object is localcell and service is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "/personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));
        // boxname exists, object is localbox and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is((String) null));
        // boxname exists, object is null and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is((String) null));
        // boxname exists, object is localcell and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_OBJECT.getName()));
        // boxname exists, object is localunit and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localunit:/cell/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_OBJECT.getName()));
        // boxname exists, object is http and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "http://personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_OBJECT.getName()));
        // boxname exists, object is https and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "https://personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_OBJECT.getName()));
        // boxname exists, object is invalid and service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "/personium/cell/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_OBJECT.getName()));
        // boxname exists, object is localbox and service is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "/personium/cell/box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));
    }

    /**
     * Test validateRule().
     * Normal test.
     * action is exec.
     */
    @Test
    public void validateRule_Normal_action_exec() {
        String action = "exec";

        // service is null.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // boxname is null, object is valid and service is localcell.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localcell:/box/dir/col/service",
                                                false),
                   is((String) null));

        // boxname is null, object is valid and service is localbox.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localbox:/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));


        // boxname is null, object is valid and service is localunit.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // boxname is null, object is valid and service is http.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "http:/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // boxname is null, object is valid and service is https.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "https:/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // boxname is null, object is valid and service is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/box/col/entity",
                                                null,
                                                action,
                                                "/personium/cell/box/col/service#box/col/service",
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // boxname exists, object is valid and service is localbox.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "personium-localbox:/dir/col/service",
                                                true),
                   is((String) null));

        // boxname exists, object is valid and service is localcell.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "personium-localcell:/box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // boxname exists, object is valid and service is localunit.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "personium-localunit:/cell/box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // boxname exists, object is valid and service is http.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "http://personium/cell/box/col/service#box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // boxname exists, object is valid and service is https.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "https://personium/cell/box/col/service#box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // boxname exists, object is valid and service is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localbox:/col/entity",
                                                null,
                                                action,
                                                "/personium/cell/box/col/service#box/col/service",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

    }

    /**
     * Test validateRule().
     * Normal test.
     * action is relay.event.
     */
    @Test
    public void validateRule_Normal_action_relayevent() {
        String action = "relay.event";

        // service is null.
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // service is localcell
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localcell:/",
                                                false),
                   is((String) null));

        // service is invalid localcell
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localcell:/box",
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // service is localbox
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localbox:/",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // service is localunit
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localunit:/cell",
                                                true),
                   is(Rule.P_TARGETURL.getName()));

        // service is localunit
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                null,
                                                null,
                                                action,
                                                "personium-localunit:/cell/",
                                                true),
                   is((String) null));

        // service is path base
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/__ctl/Rule",
                                                null,
                                                action,
                                                "https://host.domain/unitpath/cell/",
                                                false),
                   is((String) null));

        // service is fqdn base
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/__ctl/Rule",
                                                null,
                                                action,
                                                "https://cell.host.domain/unitpath/",
                                                false),
                   is((String) null));

        // object is localcell
        assertThat(CellCtlResource.validateRule(false,
                                                null,
                                                "personium-localcell:/__",
                                                null,
                                                action,
                                                "https://cell.host.domain/",
                                                true),
                   is((String) null));

    }

    /**
     * Test validateRule().
     * Normal test.
     * action is relay.data.
     */
    @Test
    public void validateRule_Normal_action_relaydata() {
        String action = "relay.data";

        // service is null.
        assertThat(CellCtlResource.validateRule(false,
                                                "odata.create",
                                                null,
                                                null,
                                                action,
                                                null,
                                                false),
                   is(Rule.P_TARGETURL.getName()));

        // service is valid.
        assertThat(CellCtlResource.validateRule(false,
                                                "odata.create",
                                                null,
                                                null,
                                                action,
                                                "https://cell.host.domain/unit/box/col/ent#box/col/ent",
                                                false),
                   is((String) null));

        // type is odata.update.
        assertThat(CellCtlResource.validateRule(false,
                                                "odata.update",
                                                null,
                                                null,
                                                action,
                                                "https://cell.host.domain/unit/box/col/ent#box/col/ent",
                                                false),
                   is((String) null));

        // type is odata.patch.
        assertThat(CellCtlResource.validateRule(false,
                                                "odata.patch",
                                                null,
                                                null,
                                                action,
                                                "https://cell.host.domain/unit/box/col/ent#box/col/ent",
                                                false),
                   is((String) null));

        // type is invalid.
        assertThat(CellCtlResource.validateRule(false,
                                                "odata.get",
                                                null,
                                                null,
                                                action,
                                                "https://cell.host.domain/unit/box/col/ent#box/col/ent",
                                                false),
                   is(Rule.P_TYPE.getName()));

    }

    /**
     * Test validateRule().
     * Normal test.
     * type is timer.oneshot.
     */
    @Test
    public void validateRule_Normal_type_timeroneshot() {
        String type = "timer.oneshot";

        // external is false
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "1234567890",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/col/srv",
                                                false),
                   is((String) null));

        // external is true
        assertThat(CellCtlResource.validateRule(true,
                                                type,
                                                "1234567890",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/col/srv",
                                                false),
                   is(Rule.P_EXTERNAL.getName()));

        // object is 1
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "1",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is((String) null));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "0",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "-1",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "253402300800000",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

    }

    /**
     * Test validateRule().
     * Normal test.
     * type is timer.periodic.
     */
    @Test
    public void validateRule_Normal_type_timerperiodic() {
        String type = "timer.periodic";

        // external is false
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "10",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is((String) null));

        // external is true
        assertThat(CellCtlResource.validateRule(true,
                                                type,
                                                "10",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_EXTERNAL.getName()));

        // object is 1
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "1",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is((String) null));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "0",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "-10",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

        // object is invalid
        assertThat(CellCtlResource.validateRule(false,
                                                type,
                                                "253402300800000",
                                                null,
                                                "exec",
                                                "personium-localcell:/box/dir/col/srv",
                                                false),
                   is(Rule.P_OBJECT.getName()));

    }

}
