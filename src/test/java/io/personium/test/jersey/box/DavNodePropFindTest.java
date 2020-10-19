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
package io.personium.test.jersey.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.TResponse;

/**
 * PROPFINDのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class DavNodePropFindTest extends PersoniumTest {

    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "davtestcell";
    private static final String BOX_NAME = "davTestBox";

    /**
     * コンストラクタ.
     * PersoniumCoreContainerFilterクラスを設定しておく
     */
    public DavNodePropFindTest() {
        super(new PersoniumCoreApplication());
    }
    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト完了時に実行される処理.
     */
    @AfterClass
    public static void afterClass() {
    }

    /**
     * 各テストの開始時に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
        CellUtils.create(CELL_NAME, TOKEN, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL_NAME, BOX_NAME, TOKEN, HttpStatus.SC_CREATED);
    }

    /**
     * 各テストの完了時に実行される処理.
     */
    @After
    public void after() {
        Setup.cellBulkDeletion(CELL_NAME);
    }

    /**
     * 複数階層のDavCollectionに対するPROPFINDが正しく取得できること.
     */
    @Test
    public final void 複数階層のDavCollectionに対するPROPFINDが正しく取得できること() {
        final String davColFile = "box/mkcol.txt";
        final String davFile = "box/dav-put.txt";
        final String propFile = "box/propfind-box-allprop.txt";
        final String davBody = "dav body";
        // Box配下のDavコレクション作成
        // 1階層目のDavコレクション配下にDavコレクション作成
        String path = BOX_NAME + "/" + "dav1";
        DavResourceUtils.createWebDavCollection(davColFile, CELL_NAME, path, TOKEN, HttpStatus.SC_CREATED);
        path = BOX_NAME + "/" + "dav2";
        DavResourceUtils.createWebDavCollection(davColFile, CELL_NAME, path, TOKEN, HttpStatus.SC_CREATED);
        // Box配下のDavファイル作成
        path = "file1.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "file2.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);

        // 1階層目のDavコレクション配下にDavファイル作成
        path = "dav1/dav1-file1.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav1/dav1-file2.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav2/dav2-file1.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav2/dav2-file2.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);

        // 1階層目のDavコレクション配下にDavコレクション作成
        path = BOX_NAME + "/" + "dav2/dav2-1";
        DavResourceUtils.createWebDavCollection(davColFile, CELL_NAME, path, TOKEN, HttpStatus.SC_CREATED);
        path = BOX_NAME + "/" + "dav2/dav2-2";
        DavResourceUtils.createWebDavCollection(davColFile, CELL_NAME, path, TOKEN, HttpStatus.SC_CREATED);

        // 2階層目のDavコレクション配下にDavファイル作成
        path = "dav2/dav2-1/dav2-1-file1.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav2/dav2-1/dav2-1-file2.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav2/dav2-2/dav2-2-file1.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);
        path = "dav2/dav2-2/dav2-2-file2.txt";
        DavResourceUtils.createWebDavFile(CELL_NAME, TOKEN, davFile, davBody, BOX_NAME, path, HttpStatus.SC_CREATED);

        // Check0: PROPFND Cell (Resources directly below Cell are not displayed)
        //   * This process essentially corresponds to "290_Cell_Get_Property".
        //   * However, there is a part that performs the same process as "305_Get_Property" internally,
        //   * and it was necessary to register and test the subordinate WebDav, so a test case was added here.
        List<String> expects = new ArrayList<String>();
        expects.add(CELL_NAME);
        TResponse cellPropRes = CellUtils.propfind(CELL_NAME, "cell/propfind-cell-allprop.txt", TOKEN, "1",
                HttpStatus.SC_MULTI_STATUS);
        NodeList resElems = cellPropRes.bodyAsXml().getDocumentElement().getElementsByTagName("response");
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

        // Check1: Box直下のPROPFND
        path = BOX_NAME;
        expects = new ArrayList<String>();
        expects.add(BOX_NAME);
        expects.add("dav1");
        expects.add("dav2");
        expects.add("file1.txt");
        expects.add("file2.txt");
        resElems = requestPropfindAndGetNodes(propFile, TOKEN, CELL_NAME, path);
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

        // Check2-1: 1階層目のDavコレクションのPROPFND(2File)
        path = BOX_NAME + "/" + "dav1";
        expects = new ArrayList<String>();
        expects.add("dav1");
        expects.add("dav1-file1.txt");
        expects.add("dav1-file2.txt");
        resElems = requestPropfindAndGetNodes(propFile, TOKEN, CELL_NAME, path);
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

        // Check2-1: 1階層目のDavコレクションのPROPFND(2Collection + 2File)
        path = BOX_NAME + "/" + "dav2";
        expects = new ArrayList<String>();
        expects.add("dav2");
        expects.add("dav2-file1.txt");
        expects.add("dav2-file2.txt");
        expects.add("dav2-1");
        expects.add("dav2-2");
        resElems = requestPropfindAndGetNodes(propFile, TOKEN, CELL_NAME, path);
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

        // Check3-1: 2階層目のDavコレクションのPROPFND(2File)
        path = BOX_NAME + "/" + "dav2/dav2-1";
        expects = new ArrayList<String>();
        expects.add("dav2-1");
        expects.add("dav2-1-file1.txt");
        expects.add("dav2-1-file2.txt");
        resElems = requestPropfindAndGetNodes(propFile, TOKEN, CELL_NAME, path);
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

        // Check3-2: 2階層目のDavコレクションのPROPFND(2File)
        path = BOX_NAME + "/" + "dav2/dav2-2";
        expects = new ArrayList<String>();
        expects.add("dav2-2");
        expects.add("dav2-2-file1.txt");
        expects.add("dav2-2-file2.txt");
        resElems = requestPropfindAndGetNodes(propFile, TOKEN, CELL_NAME, path);
        assertEquals(expects.size(),  resElems.getLength());
        checkPropFindChildren(resElems, expects);

    }

    /**
     * 引数で渡されたリソースに対してPROPFIND(depth=1)を実行して、/multistatus/response/hrefのノードリストを取得する.
     * @param propFile リクエストファイル名
     * @param token アクセストークン
     * @param cellName セル名
     * @param path Box配下のリソースパス
     * @return 取得したノードリスト
     */
    private NodeList requestPropfindAndGetNodes(String propFile, String token, String cellName, String path) {
        TResponse res = DavResourceUtils.propfind(propFile, TOKEN, CELL_NAME, path, 1, HttpStatus.SC_MULTI_STATUS);
        Element root = res.bodyAsXml().getDocumentElement();
        NodeList resElems = root.getElementsByTagName("response");
        return resElems;
    }

    private void checkPropFindChildren(NodeList resElems, List<String> expects) {
        for (int i = 0; i < resElems.getLength(); i++) {
            Element elem = (Element) resElems.item(i);
            String href = elem.getElementsByTagName("href").item(0).getTextContent();
            String[] fields = href.split("/");
            assertTrue(expects.contains(fields[fields.length - 1]));
        }
    }
}
