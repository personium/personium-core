/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.es.odata.MessageODataProducer;
import io.personium.core.model.impl.es.odata.UnitCtlODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for ModelFactory.
 */
@Category({ Unit.class })
public class ModelFactoryTest {

    /**
     * Test ODataCtl.unitCtl().
     * Normal test.
     */
    @Test
    public void ODataCtl_unitCtl_Normal() {
        assertThat(ModelFactory.ODataCtl.unitCtl(null), instanceOf(UnitCtlODataProducer.class));
    }

    /**
     * Test ODataCtl.cellCtl().
     * Normal test.
     */
    @Test
    public void ODataCtl_cellCtl_Normal() {
        assertThat(ModelFactory.ODataCtl.cellCtl(null), instanceOf(CellCtlODataProducer.class));
    }

    /**
     * Test ODataCtl.message().
     * Normal test.
     */
    @Test
    public void ODataCtl_message_Normal() {
        assertThat(ModelFactory.ODataCtl.message(null, null), instanceOf(MessageODataProducer.class));
    }

    /**
     * Test ODataCtl.userSchema().
     * Normal test.
     */
    @Test
    public void ODataCtl_userSchema_Normal() {
        assertThat(ModelFactory.ODataCtl.userSchema(null, null), instanceOf(UserSchemaODataProducer.class));
    }

    /**
     * Test ODataCtl.userData().
     * Normal test.
     */
    @Test
    public void ODataCtl_userData_Normal() {
        assertThat(ModelFactory.ODataCtl.userData(null, null), instanceOf(UserDataODataProducer.class));
    }

}
