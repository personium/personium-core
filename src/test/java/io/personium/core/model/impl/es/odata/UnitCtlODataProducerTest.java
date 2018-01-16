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
package io.personium.core.model.impl.es.odata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.auth.AccessContext;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessContext.class, UriUtils.class, QueryMapFactory.class })
@Category({ Unit.class })
public class UnitCtlODataProducerTest {

    /** Test class. */
    private UnitCtlODataProducer unitCtlODataProducer;

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.UnitCtlODataProducer#UnitCtlODataProducer()}.
     */
    @Test
    @Ignore
    public void testUnitCtlODataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test getImplicitFilters().
     * normal.
     * Type is UnitUser.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void getImplicitFilters_Normal_type_unituser() throws Exception {
        // Test method args
        String entitySetName = "entitySetName";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        unitCtlODataProducer = spy(new UnitCtlODataProducer(accessContext));

        doReturn(AccessContext.TYPE_UNIT_USER).when(accessContext).getType();
        doReturn("http://personiumunit/").when(accessContext).getBaseUri();
        doReturn("http://personiumunit/admincell/#admin").when(accessContext).getSubject();
        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("personium-localunit:/admincell/#admin").when(UriUtils.class,
                "convertSchemeFromHttpToLocalUnit", "http://personiumunit/", "http://personiumunit/admincell/#admin");

        Map<String, Object> term1 = new HashMap<>();
        Map<String, Object> term2 = new HashMap<>();
        term1.put("key1", "value1");
        term2.put("key2", "value2");
        PowerMockito.mockStatic(QueryMapFactory.class);
        PowerMockito.doReturn(term1).when(QueryMapFactory.class, "termQuery",
                OEntityDocHandler.KEY_OWNER, "http://personiumunit/admincell/#admin");
        PowerMockito.doReturn(term2).when(QueryMapFactory.class, "termQuery",
                OEntityDocHandler.KEY_OWNER, "personium-localunit:/admincell/#admin");

        Map<String, Object> should = new HashMap<>();
        should.put("should", "value");

        ArgumentCaptor<List> orQueriesCaptor = ArgumentCaptor.forClass(List.class);
        PowerMockito.doReturn(should).when(QueryMapFactory.class, "shouldQuery", orQueriesCaptor.capture());

        // Expected result
        List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>();
        expected.add(should);

        // Run method
        List<Map<String, Object>> actual = unitCtlODataProducer.getImplicitFilters(entitySetName);

        // Confirm result
        List<Map<String, Object>> orQueries = orQueriesCaptor.getValue();
        assertThat(orQueries.get(0).get("key1"), is("value1"));
        assertThat(orQueries.get(1).get("key2"), is("value2"));

        assertThat(actual.get(0).get("should"), is(expected.get(0).get("should")));
    }

    /**
     * Test getImplicitFilters().
     * normal.
     * Type is UnitMaster.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void getImplicitFilters_Normal_type_unitmaster() throws Exception {
        // Test method args
        String entitySetName = "entitySetName";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        unitCtlODataProducer = spy(new UnitCtlODataProducer(accessContext));

        doReturn(AccessContext.TYPE_UNIT_MASTER).when(accessContext).getType();

        // Expected result
        // None.

        // Run method
        List<Map<String, Object>> actual = unitCtlODataProducer.getImplicitFilters(entitySetName);

        // Confirm result
        assertThat(actual.size(), is(0));
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForEntitySet(java.lang.String)}.
     */
    @Test
    public void testGetAccessorForEntitySetString() {
//        EsClient.setConnectionConfiguration(PersoniumUnitConfig.getEsClusterName(), PersoniumUnitConfig.getEsHosts());
        UnitCtlODataProducer producer = new UnitCtlODataProducer(null);
        assertNotNull(producer.getAccessorForEntitySet("Cell"));
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForLink()}.
     */
    @Test
    @Ignore
    public void testGetAccessorForLink() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForLog()}.
     */
    @Test
    @Ignore
    public void testGetAccessorForLog() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.UnitCtlODataProducer#getMetadata()}.
     */
    @Test
    public void testGetMetadata() {
        UnitCtlODataProducer producer = new UnitCtlODataProducer(null);
        assertNotNull(producer.getMetadata());
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#getAccessorForLink()}.
     */
    @Test
    @Ignore
    public void testGetEsTypeForLink1() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#getAccessorForLog()}.
     */
    @Test
    @Ignore
    public void testGetEsTypeForLog1() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#EsODataProducer()}.
     */
    @Test
    @Ignore
    public void testEsODataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#callFunction
     * (org.odata4j.edm.EdmFunctionImport, java.util.Map, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testCallFunction() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#close()}.
     */
    @Test
    @Ignore
    public void testClose() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getEntity
     * (java.lang.String, org.odata4j.core.OEntityKey, org.odata4j.producer.EntityQueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getEntities
     * (java.lang.String, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetEntities() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#deleteEntity
     * (java.lang.String, org.odata4j.core.OEntityKey)}
     * .
     */
    @Test
    @Ignore
    public void testDeleteEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#createEntity
     * (java.lang.String, * org.odata4j.core.OEntity)} .
     */
    @Test
    @Ignore
    public void testCreateEntityStringOEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#createEntity
     * (java.lang.String, org.odata4j.core.OEntityKey, java.lang.String, org.odata4j.core.OEntity)}
     * .
     */
    @Test
    @Ignore
    public void testCreateEntityStringOEntityKeyStringOEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#createLink
     * (org.odata4j.core.OEntityId, java.lang.String, org.odata4j.core.OEntityId)}
     * .
     */
    @Test
    @Ignore
    public void testCreateLink() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#deleteLink
     * (org.odata4j.core.OEntityId, java.lang.String, org.odata4j.core.OEntityKey)}
     * .
     */
    @Test
    @Ignore
    public void testDeleteLink() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getLinks
     * (org.odata4j.core.OEntityId, java.lang.String)}
     * .
     */
    @Test
    @Ignore
    public void testGetLinks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link io.personium.core.model.impl.es.odata.EsODataProducer#getMetadataProducer()}.
     */
    @Test
    @Ignore
    public void testGetMetadataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getNavProperty
     * (java.lang.String, org.odata4j.core.OEntityKey, java.lang.String, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetNavProperty() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#mergeEntity
     * (java.lang.String, org.odata4j.core.OEntity)}
     * .
     */
    @Test
    @Ignore
    public void testMergeEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#updateEntity
     * (java.lang.String, org.odata4j.core.OEntity)}
     * .
     */
    @Test
    @Ignore
    public void testUpdateEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#updateLink
     * (org.odata4j.core.OEntityId, java.lang.String, org.odata4j.core.OEntityKey, org.odata4j.core.OEntityId)}
     * .
     */
    @Test
    @Ignore
    public void testUpdateLink() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getEntitiesCount
     * (java.lang.String, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetEntitiesCount() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link io.personium.core.model.impl.es.odata.EsODataProducer#getNavPropertyCount
     * (java.lang.String, org.odata4j.core.OEntityKey, java.lang.String, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetNavPropertyCount() {
        fail("Not yet implemented");
    }
}
