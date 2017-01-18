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
package com.fujitsu.dc.test.jersey.cell.ctl;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.utils.Http;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * RoleResourceのテスト.
 * setupパッケージでのRoleの準備があることが前提
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class RoleResourceTest extends JerseyTest {

    private static String cellName = "testcell1";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleResourceTest() {
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
     * RoleリソースへのGET.
     */
    @Test
    public final void getBoxRole() {
        Http.request("cell/rrs-get-box-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", cellName)
        .with("box", "box1")
        .with("role", "role1")
        .returns()
        .statusCode(HttpStatus.SC_OK);

    }

    /**
     * RoleリソースルートへのPROPFIND.
     */
    @Test
    public final void getRoot() {

        // アカウント・ロールの紐付けC
        Http.request("cell/rrs-get-root.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .returns()
                .statusCode(HttpStatus.SC_OK);

    }
}
