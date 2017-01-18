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
package com.fujitsu.dc.core.model.impl.es;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.core.model.impl.es.accessor.DavNodeAccessor;
import com.fujitsu.dc.core.model.impl.es.accessor.EntitySetAccessor;
import com.fujitsu.dc.core.model.impl.es.accessor.ODataLinkAccessor;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;

/**
 * EsModelの単体テストケース.
 */
@Ignore
@RunWith(DcRunner.class)
@Category({ Unit.class })
public class EsModelTest {

    /**
     * テストケース共通の初期化処理. テスト用のElasticsearchのNodeを初期化する
     * @throws Exception 異常が発生した場合の例外
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * テストケース共通のクリーンアップ処理. テスト用のElasticsearchのNodeをクローズする
     * @throws Exception 異常が発生した場合の例外
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * 各テスト実行前の初期化処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * 各テスト実行後のクリーンアップ処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * idxAdminメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void idxAdminメソッドでオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxAdmin();
        assertNotNull(index);
        assertEquals(EsIndex.CATEGORY_AD, index.getCategory());
    }

    /**
     * idxUserメソッドでnullを指定した場合にオブジェクトが正常に取得できる.
     */
    @Test
    public void idxUserメソッドでnullを指定した場合にオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxUser(null);
        assertNotNull(index);
        assertTrue(index.getName().endsWith("anon"));
        assertEquals(EsIndex.CATEGORY_USR, index.getCategory());
    }

    /**
     * idxUserメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void idxUserメソッドでオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxUser("UriStringForTest");
        assertNotNull(index);
        assertTrue(index.getName().endsWith("UriStringForTest"));
        assertEquals(EsIndex.CATEGORY_USR, index.getCategory());
    }

    /**
     * cellメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void cellメソッドでオブジェクトが正常に取得できる() {
        EntitySetAccessor cell = EsModel.cell();
        assertNotNull(cell);
        assertEquals("Cell", cell.getType());
    }

    /**
     * boxメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void boxメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        EntitySetAccessor box = EsModel.box(null);
        assertNotNull(box);
    }

    /**
     * unitCtlメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void unitCtlメソッドでオブジェクトが正常に取得できる() {
        EntitySetAccessor unitCtl = EsModel.unitCtl("TypeStringForTest", "TestCellID");
        assertNotNull(unitCtl);
        assertEquals("TypeStringForTest", unitCtl.getType());
    }

    /**
     * cellCtlメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void cellCtlメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        EntitySetAccessor cellCtl = EsModel.cellCtl(null, "TypeStringForTest");
        assertNotNull(cellCtl);
    }

    /**
     * unitCtlLinkメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void unitCtlLinkメソッドでオブジェクトが正常に取得できる() {
        ODataLinkAccessor unitCtlLink = EsModel.unitCtlLink("TestCellID");
        assertNotNull(unitCtlLink);
        assertEquals("link", unitCtlLink.getType());
    }

    /**
     * cellCtlLinkメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void cellCtlLinkメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        ODataLinkAccessor cellCtlLink = EsModel.cellCtlLink(null);
        assertNotNull(cellCtlLink);
    }

    /**
     * colメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void colメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        DavNodeAccessor col = EsModel.col(null);
        assertNotNull(col);
    }
}
