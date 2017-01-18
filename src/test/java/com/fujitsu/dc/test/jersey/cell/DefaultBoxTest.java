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
package com.fujitsu.dc.test.jersey.cell;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * UnitUserでCellをCRUDするテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class DefaultBoxTest extends JerseyTest {

    private static final String CELL_NAME = "createCell1";
    private static final String COL_NAME = "createCol";
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public DefaultBoxTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {

    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
    }

    /**
     * セル作成時にデフォルトボックスが生成されることの確認.
     */
    @Test
    public final void セル作成時にデフォルトボックスが生成されることの確認() {
        try {
            // セル作成
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);

            // デフォルトボックスに対してMKCOLを実行して、ボックスの存在及び子要素が作成できることを確認
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", CELL_NAME, Box.DEFAULT_BOX_NAME + "/" + COL_NAME,
                    TOKEN,
                    HttpStatus.SC_CREATED);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(CELL_NAME, Box.DEFAULT_BOX_NAME, COL_NAME, TOKEN, -1);

            // セル削除
            CellUtils.delete(TOKEN, CELL_NAME, -1);
        }
    }

    /**
     * デフォルトボックス配下にデータが存在するとセルが削除できないことの確認.
     */
    @Test
    public final void デフォルトボックス配下にデータが存在するとセルが削除できないことの確認() {
        try {
            // セル作成
            CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);

            // デフォルトボックスにコレクションを作成
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", CELL_NAME, Box.DEFAULT_BOX_NAME + "/" + COL_NAME,
                    TOKEN,
                    HttpStatus.SC_CREATED);

            // デフォルトボックスにコレクションがあるためセル削除が失敗すること
            CellUtils.delete(TOKEN, CELL_NAME, HttpStatus.SC_CONFLICT);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(CELL_NAME, Box.DEFAULT_BOX_NAME, COL_NAME, TOKEN, -1);

            // セル削除
            CellUtils.delete(TOKEN, CELL_NAME, -1);
        }
    }
}
