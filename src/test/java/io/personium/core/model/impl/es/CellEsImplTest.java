/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model.impl.es;

import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for CellEsImpl.
 */
@Category({ Unit.class })
public class CellEsImplTest {

    /**
     * 空白を含むCell名を指定してnullが返却されること.
     * @throws Exception Unexpected error
     */
    @Test
    public void 空白を含むCell名を指定してnullが返却されること() throws Exception {
        // Load methods for private
        Method method = CellEsImpl.class.getDeclaredMethod("findCell", String.class, String.class);
        method.setAccessible(true);
        // Run method
        assertNull(method.invoke(null, "s.Name.untouched", "cell test"));
    }

    /**
     * 改行制御コードを含むCell名を指定してnullが返却されること.
     * @throws Exception Unexpected error
     */
    @Test
    public void 改行制御コードを含むCell名を指定してnullが返却されること() throws Exception {
        // Load methods for private
        Method method = CellEsImpl.class.getDeclaredMethod("findCell", String.class, String.class);
        method.setAccessible(true);
        // Run method
        assertNull(method.invoke(null, "s.Name.untouched", "cell\ntest"));
    }

    /**
     * タブ制御コードを含むCell名を指定してnullが返却されること.
     * @throws Exception Unexpected error
     */
    @Test
    public void タブ制御コードを含むCell名を指定してnullが返却されること() throws Exception {
        // Load methods for private
        Method method = CellEsImpl.class.getDeclaredMethod("findCell", String.class, String.class);
        method.setAccessible(true);
        // Run method
        assertNull(method.invoke(null, "s.Name.untouched", "cell\ttest"));
    }

    /**
     * リターンコードを含むCell名を指定してnullが返却されること.
     * @throws Exception Unexpected error
     */
    @Test
    public void リターンコードを含むCell名を指定してnullが返却されること() throws Exception {
        // Load methods for private
        Method method = CellEsImpl.class.getDeclaredMethod("findCell", String.class, String.class);
        method.setAccessible(true);
        // Run method
        assertNull(method.invoke(null, "s.Name.untouched", "cell\rtest"));
    }

    /**
     * 空白を含むBox名を指定してnullが返却されること.
     */
    @Test
    public void 空白を含むBox名を指定してnullが返却されること() {
        CellEsImpl cellEsImpl = new CellEsImpl();
        assertNull(cellEsImpl.getBoxForName("box test"));
    }

    /**
     * 改行制御コードを含むBox名を指定してnullが返却されること.
     */
    @Test
    public void 改行制御コードを含むBox名を指定してnullが返却されること() {
        CellEsImpl cellEsImpl = new CellEsImpl();
        assertNull(cellEsImpl.getBoxForName("box\ntest"));
    }

    /**
     * タブ制御コードを含むBox名を指定してnullが返却されること.
     */
    @Test
    public void タブ制御コードを含むBox名を指定してnullが返却されること() {
        CellEsImpl cellEsImpl = new CellEsImpl();
        assertNull(cellEsImpl.getBoxForName("box\ttest"));
    }

    /**
     * リターンコードを含むBox名を指定してnullが返却されること.
     */
    @Test
    public void リターンコードを含むBox名を指定してnullが返却されること() {
        CellEsImpl cellEsImpl = new CellEsImpl();
        assertNull(cellEsImpl.getBoxForName("box\rtest"));
    }
}
