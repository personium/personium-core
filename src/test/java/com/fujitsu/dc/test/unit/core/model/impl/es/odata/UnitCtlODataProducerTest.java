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
package com.fujitsu.dc.test.unit.core.model.impl.es.odata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer;
import com.fujitsu.dc.test.categories.Unit;

/**
 * UnitCtlODataProducerユニットテストクラス.
 */
@Category({ Unit.class })
public class UnitCtlODataProducerTest {

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer#UnitCtlODataProducer()}.
     */
    @Test
    @Ignore
    public void testUnitCtlODataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForEntitySet(java.lang.String)}.
     */
    @Test
    public void testGetAccessorForEntitySetString() {
//        EsClient.setConnectionConfiguration(DcCoreConfig.getEsClusterName(), DcCoreConfig.getEsHosts());
        UnitCtlODataProducer producer = new UnitCtlODataProducer(null);
        assertNotNull(producer.getAccessorForEntitySet("Cell"));
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForLink()}.
     */
    @Test
    @Ignore
    public void testGetAccessorForLink() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer#getAccessorForLog()}.
     */
    @Test
    @Ignore
    public void testGetAccessorForLog() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.UnitCtlODataProducer#getMetadata()}.
     */
    @Test
    public void testGetMetadata() {
        UnitCtlODataProducer producer = new UnitCtlODataProducer(null);
        assertNotNull(producer.getMetadata());
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getAccessorForLink()}.
     */
    @Test
    @Ignore
    public void testGetEsTypeForLink1() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getAccessorForLog()}.
     */
    @Test
    @Ignore
    public void testGetEsTypeForLog1() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#EsODataProducer()}.
     */
    @Test
    @Ignore
    public void testEsODataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#callFunction
     * (org.odata4j.edm.EdmFunctionImport, java.util.Map, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testCallFunction() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#close()}.
     */
    @Test
    @Ignore
    public void testClose() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getEntity
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getEntities
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#deleteEntity
     * (java.lang.String, org.odata4j.core.OEntityKey)}
     * .
     */
    @Test
    @Ignore
    public void testDeleteEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#createEntity
     * (java.lang.String, * org.odata4j.core.OEntity)} .
     */
    @Test
    @Ignore
    public void testCreateEntityStringOEntity() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#createEntity
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#createLink
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#deleteLink
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getLinks
     * (org.odata4j.core.OEntityId, java.lang.String)}
     * .
     */
    @Test
    @Ignore
    public void testGetLinks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getMetadataProducer()}.
     */
    @Test
    @Ignore
    public void testGetMetadataProducer() {
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getNavProperty
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#mergeEntity
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#updateEntity
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#updateLink
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getEntitiesCount
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
     * {@link com.fujitsu.dc.core.model.impl.es.odata.EsODataProducer#getNavPropertyCount
     * (java.lang.String, org.odata4j.core.OEntityKey, java.lang.String, org.odata4j.producer.QueryInfo)}
     * .
     */
    @Test
    @Ignore
    public void testGetNavPropertyCount() {
        fail("Not yet implemented");
    }
}
