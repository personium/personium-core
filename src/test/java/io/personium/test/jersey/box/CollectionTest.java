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
package io.personium.test.jersey.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.jersey.test.framework.JerseyTest;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.box.odatacol.schema.complextype.ComplexTypeUtils;
import io.personium.test.jersey.box.odatacol.schema.complextypeproperty.ComplexTypePropertyUtils;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TestMethodUtils;

/**
 * MKCOLのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class CollectionTest extends JerseyTest {
    /**
     * コンストラクタ.
     */
    public CollectionTest() {
        super("io.personium.core.rs");
    }

    static final String ACL_AUTH_TEST_SETTING_FILE = "box/acl-authtest.txt";
    static final String ACL_AUTH_PROPPATCH_TEST_SETTING_FILE = "box/acl-authtestProppatch.txt";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String ACL_SETTING_TEST = "box/acl-setting.txt";

    /**
     * MKCOL-Normalのテスト.
     */
    @Test
    public final void MKCOL_Normalのテスト() {
        String path = "davcol1";

        try {
            // コレクションの作成
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得（bodyあり）
            TResponse tresponseAll = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponseAll.statusCode(HttpStatus.SC_MULTI_STATUS);

            // コレクションの取得(bodyなし)
            TResponse tresponse = Http.request("box/propfind-col-body-0.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // bodyの全文比較
            assertEquals(tresponse.getBody(), tresponseAll.getBody());

            // BodyXMLからの要素取得
            String status1 = getXmlNodeValue(tresponse.bodyAsXml(), "status");
            assertEquals("HTTP/1.1 200 OK", status1);

            // webDAVの仕様とのマッチ
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "response"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "href"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "propstat"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "prop"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "creationdate"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "getlastmodified"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "resourcetype"));

        } finally {
            // コレクションの削除
            deleteTest(path, -1);

            // コレクションの取得（削除の確認）
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        }
    }

    /**
     * MKCOL_Body異常系のテスト.
     */
    @Test
    public final void MKCOL_Body異常系のテスト() {
        String path = "davcol3";
        StringBuilder sb;
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
        sb.append("<D:sets><D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop></D:sets>");
        sb.append("</D:mkcol>");
        // Http http = Http.request("box/mkcol-custom.txt").with("path", path).with("token", TOKEN);
        try {
            // XML不正
            String code = PersoniumCoreException.Dav.XML_ERROR.getCode();
            String message = PersoniumCoreException.Dav.XML_ERROR.getMessage();
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("<D:set<D:prop><D:resourcetype2><D:collection/></D:resourcetype2></D:prop></D:set>");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .checkErrorResponse(code, message);

            // set 要素無し
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

            // set 要素名ミス
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("<D:sets><D:prop><D:resourcetype><D:collection/></D:resourcetype></D:prop></D:sets>");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

            // props 要素名ミス
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("<D:set><D:props><D:resourcetype><D:collection/></D:resourcetype></D:props></D:set>");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

            // resourcetype 要素名ミス
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("<D:set><D:prop><D:resourcetype2><D:collection/></D:resourcetype2></D:prop></D:set>");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

            // resourcetype が不正
            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<D:mkcol xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">");
            sb.append("<D:set><D:prop><D:resourcetype><D:collection2/></D:resourcetype></D:prop></D:set>");
            sb.append("</D:mkcol>");
            Http.request("box/mkcol-custom.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .with("body", sb.toString())
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // コレクションの削除
            deleteTest(path, -1);
        }
    }

    /**
     * mkcolのテスト.
     */
    @Test
    public final void MKCOL_Body無しのテスト() {

        String path = "davcol2";
        try {
            // コレクションの作成
            Http.request("box/mkcol-0.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得(bodyなし)
            TResponse tresponse = Http.request("box/propfind-col-body-0.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // コレクションの取得（bodyあり）
            TResponse tresponseAll = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponseAll.statusCode(HttpStatus.SC_MULTI_STATUS);

            // コレクションpropfindの比較
            assertEquals(tresponse.getBody(), tresponseAll.getBody());

            // BodyXMLからの要素取得
            String status1 = getXmlNodeValue(tresponse.bodyAsXml(), "status");
            assertEquals("HTTP/1.1 200 OK", status1);

            // webDAVの仕様とのマッチ
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "response"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "href"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "propstat"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "prop"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "creationdate"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "getlastmodified"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "resourcetype"));

        } finally {
            // コレクションの削除
            deleteTest(path, -1);

            // コレクションの取得（削除の確認）
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        }

    }

    /**
     * mkcolのテスト.
     */
    @Test
    public final void MKCOL_Body無しContentLength無しのテスト() {

        String path = "davcol2";
        try {
            // コレクションの作成
            Http.request("box/mkcol-0-non-content-length.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得(bodyなし)
            TResponse tresponse = Http.request("box/propfind-col-body-0.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // コレクションの取得（bodyあり）
            TResponse tresponseAll = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponseAll.statusCode(HttpStatus.SC_MULTI_STATUS);

            // コレクションpropfindの比較
            assertEquals(tresponse.getBody(), tresponseAll.getBody());

            // BodyXMLからの要素取得
            String status1 = getXmlNodeValue(tresponse.bodyAsXml(), "status");
            assertEquals("HTTP/1.1 200 OK", status1);

            // webDAVの仕様とのマッチ
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "response"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "href"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "propstat"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "prop"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "creationdate"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "getlastmodified"));
            assertNotNull(getXmlNodeValue(tresponse.bodyAsXml(), "resourcetype"));

        } finally {
            // コレクションの削除
            deleteTest(path, -1);

            // コレクションの取得（削除の確認）
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        }

    }

    /**
     * ODataのコレクション作成テスト.
     */
    @Test
    public final void ODataのコレクション作成テスト() {

        String path = "odatacol";
        try {
            // コレクションの作成
            Http.request("box/mkcol-odata.txt")
                    .with("cellPath", "testcell1")
                    .with("boxPath", "box1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得（bodyあり）
            TResponse tresponseAll = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponseAll.statusCode(HttpStatus.SC_MULTI_STATUS);

            // p:odata の存在チェック
            serviceColTypeTest(tresponseAll.bodyAsXml(), "p:odata");
        } finally {

            // Boxの削除
            deleteTest(path, -1);
        }
    }

    /**
     * Serviceのコレクション作成テスト.
     */
    @Test
    public final void Serviceのコレクション作成テスト() {

        String path = "servicecol";
        try {
            // コレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得（bodyあり）
            TResponse tresponseAll = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponseAll.statusCode(HttpStatus.SC_MULTI_STATUS);

            // p:service の存在チェック
            serviceColTypeTest(tresponseAll.bodyAsXml(), "p:service");
        } finally {
            // コレクションの削除
            deleteTest(path, -1);
        }
    }

    /**
     * Serviceのコレクション削除時に__srcの削除が行われることの確認.
     */
    @Test
    public final void Serviceのコレクション削除時に__srcの削除が行われることの確認() {

        String path = "servicecol02";
        try {
            // コレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの削除
            deleteTest(path, HttpStatus.SC_NO_CONTENT);

            // servicecol02 の存在チェック
            ResourceUtils.accessResource(path, TOKEN, HttpStatus.SC_NOT_FOUND, Setup.TEST_BOX1, Setup.TEST_CELL1);
            // __src の存在チェック
            ResourceUtils.accessResource(path + "/__src", TOKEN, HttpStatus.SC_NOT_FOUND, Setup.TEST_BOX1,
                    Setup.TEST_CELL1);
        } finally {
            // Boxの削除
            deleteTest(path, -1);
        }
    }

    /**
     * 階層コレクション作成テスト. 階層の内容↓ box1 ┗davcol1 ┗davcol2 ┗davcol5 ┗odatacol1 ┗davcol3 => 登録できない ┗servicecol1 ┗ davcol4 =>
     * 登録できない
     */
    @Test
    public final void 階層コレクション作成テスト() {

        try {
            // 基底Davコレクションの作成
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの作成 Davコレクション配下にDavコレクションを作成出来る事を確認
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1/davcol2")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの作成 Davコレクション配下にDavコレクションを作成出来る事を確認
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1/davcol2/davcol5")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの作成 Davコレクション配下にODataコレクションを作成出来る事を確認
            Http.request("box/mkcol-odata.txt")
                    .with("cellPath", "testcell1")
                    .with("boxPath", "box1")
                    .with("path", "davcol1/odatacol1")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの作成 Davコレクション配下にServiceコレクションを作成出来る事を確認
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1/servicecol1")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの作成 ODataコレクション配下にコレクションを作成出来ない事を確認
            // TODO レスポンスコードが404で正しいかどうかは、将来再検討
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1/odatacol1/davcol3")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            // コレクションの作成 Serviceコレクション配下にコレクションを作成出来ない事を確認
            // TODO レスポンスコードが404で正しいかどうかは、将来再検討
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", "davcol1/servicecol1/davcol4")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

            // コレクションの取得（depth1のテスト）
            TResponse tresponseWebDav = Http.request("box/propfind-col-allprop.txt")
                    .with("path", "davcol1")
                    .with("depth", "1")
                    .with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // davcol1配下のXML構成確認テスト
            String testcell = "testcell1";
            String boxName = "box1";
            String baseCell = "davcol1";
            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            NodeList resourcetypeList = root.getElementsByTagName("response");
            // responseの要素数が4
            assertEquals(4, resourcetypeList.getLength());
            // 各要素のhref・statusの確認
            depthTest(resourcetypeList, UrlUtils.box(testcell, boxName, baseCell),
                    "HTTP/1.1 200 OK");

            depthTest(resourcetypeList, UrlUtils.box(testcell, boxName, baseCell,
                    "servicecol1"), "HTTP/1.1 200 OK");

            depthTest(resourcetypeList, UrlUtils.box(testcell, boxName, baseCell,
                    "odatacol1"), "HTTP/1.1 200 OK");

            depthTest(resourcetypeList, UrlUtils.box(testcell, boxName, baseCell,
                    "davcol2"), "HTTP/1.1 200 OK");

            // davcol5が取得出来ていない事を確認
            assertEquals(-1, tresponseWebDav.getBody().indexOf(UrlUtils.box(testcell, boxName,
                    baseCell, "davcol2", "davcol5")));

        } finally {
            // Boxの削除
            deleteTest("davcol1/servicecol1", -1);
            deleteTest("davcol1/odatacol1", -1);
            deleteTest("davcol1/davcol2/davcol5", -1);
            deleteTest("davcol1/davcol2", -1);
            deleteTest("davcol1", -1);
        }
    }

    /**
     * DavCol削除時の子供データチェックテスト. 階層の内容↓ box1 ┗davcol1 ┗davcol2 ┗davFile.txt
     */
    @Test
    public final void DavCol削除時の子供データチェックテスト() {

        String davCol1 = "davcol1";
        String davCol2 = "davcol2";
        String davFileName = "davFile.txt";
        try {
            // 基底Davコレクションの作成
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, davCol1);

            // コレクションの作成 Davコレクション配下にDavコレクションを作成出来る事を確認
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, davCol1 + "/" + davCol2);

            // Davファイルの作成
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, TOKEN, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    davCol1 + "/" + davCol2 + "/" + davFileName, HttpStatus.SC_CREATED);

            // コレクションの削除→子供（DavFile）があるため削除出来ない
            deleteTest(davCol1 + "/" + davCol2, HttpStatus.SC_CONFLICT);

            // コレクションの削除→子供（davcol2）があるため削除出来ない
            deleteTest(davCol1, HttpStatus.SC_CONFLICT);

            // Davファイルの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    davCol1 + "/" + davCol2 + "/" + davFileName, HttpStatus.SC_NO_CONTENT, Setup.TEST_BOX1);

            // コレクションの削除→子供（DavFile）が無くなったため削除出来る
            deleteTest(davCol1 + "/" + davCol2, HttpStatus.SC_NO_CONTENT);

            // コレクションの削除→子供（davcol2）が無いため削除出来る
            deleteTest(davCol1, HttpStatus.SC_NO_CONTENT);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    davCol1 + "/" + davCol2 + "/" + davFileName, -1, Setup.TEST_BOX1);
            deleteTest(davCol1 + "/" + davCol2, -1);
            deleteTest(davCol1, -1);
        }
    }

    /**
     * Box削除時の子供Collectionチェックテスト. 階層の内容↓ deleteBox ┗davcol1
     */
    @Test
    public final void Box削除時の子供Collectionチェックテスト() {

        String boxName = "deleteBox";
        String davCol1 = "davcol1";
        try {
            // 基底Boxの作成
            BoxUtils.create(Setup.TEST_CELL1, boxName, TOKEN, HttpStatus.SC_CREATED);

            // コレクションの作成
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", Setup.TEST_CELL1, boxName + "/" + davCol1,
                    TOKEN, HttpStatus.SC_CREATED);

            // Boxの削除→子供（davcol1）があるため削除出来ない
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, HttpStatus.SC_CONFLICT);

            // コレクションの削除
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, boxName, davCol1, TOKEN, HttpStatus.SC_NO_CONTENT);

            // コレクションの削除→子供（davcol1）が無いため削除出来る
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, boxName, davCol1, TOKEN, -1);
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, -1);
        }
    }

    /**
     * Box削除時の子供WebDavFileチェックテスト. 階層の内容↓ deleteBox deleteFile
     */
    @Test
    public final void Box削除時の子供WebDavFileチェックテスト() {

        String boxName = "deleteBox";
        String davFileNmae = "deleteFile";
        try {
            // 基底Boxの作成
            BoxUtils.create(Setup.TEST_CELL1, boxName, TOKEN, HttpStatus.SC_CREATED);

            // Box直下にWebDavファイルを作成
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, TOKEN, "box/dav-put.txt", "hoge",
                    boxName, davFileNmae, HttpStatus.SC_CREATED);

            // Boxの削除→子供（deleteFile）があるため削除出来ない
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, HttpStatus.SC_CONFLICT);

            // WebDavファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, TOKEN, boxName, davFileNmae);

            // Boxの削除→子供（deleteFile）が無いため削除出来る
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // WebDavファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, TOKEN, boxName, davFileNmae);

            // Boxの削除
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, boxName, -1);
        }
    }

    /**
     * Odata削除時の子供データチェックテスト. 階層の内容↓ deleteOdata ┗deleteEntType
     */
    @Test
    public final void Odata削除時の子供データチェックテスト() {

        String odataName = "deleteOdata";
        String entityType = "deleteEntType";
        String accept = "application/xml";
        try {
            // Odataコレクションの作成
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    odataName);

            // EntityTypeの作成
            Http.request("box/entitySet-post.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("odataSvcPath", odataName)
                    .with("token", "Bearer " + TOKEN)
                    .with("accept", accept)
                    .with("Name", entityType)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Odataの削除→子供（deleteEntType）があるため削除出来ない
            TResponse response = deleteTest(odataName, HttpStatus.SC_FORBIDDEN);
            String code = PersoniumCoreException.Dav.HAS_CHILDREN.getCode();
            String message = PersoniumCoreException.Dav.HAS_CHILDREN.getMessage();
            response.checkErrorResponse(code, message);

            // EntityTypeの削除
            entityTypeDelete(odataName, TOKEN, accept, entityType, Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);

            // Odataの削除→子供（deleteEntType）が無いため削除出来る
            deleteTest(odataName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // EntityTypeの削除
            entityTypeDelete(odataName, TOKEN, accept, entityType, Setup.TEST_CELL1, -1);

            // Odataの削除
            deleteTest(odataName, -1);
        }
    }

    /**
     * Odata削除時の子供データチェックテスト. 階層の内容↓ deleteOdata deleteComplexType deleteComplexTypeProperty
     */
    @Test
    public final void Odata削除時のComplex子供データチェックテスト() {

        String odataName = "deleteOdata";
        String complexTypeName = "deleteComplexType";
        String complexTypePropertyName = "deleteComplexTypeProperty";
        try {
            // Odataコレクションの作成
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    odataName);

            // ComplexTypeの作成
            ComplexTypeUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    odataName, complexTypeName, HttpStatus.SC_CREATED);

            // Odataの削除→子供（ComplexType）があるため削除出来ない
            TResponse response = deleteTest(odataName, HttpStatus.SC_FORBIDDEN);
            String code = PersoniumCoreException.Dav.HAS_CHILDREN.getCode();
            String message = PersoniumCoreException.Dav.HAS_CHILDREN.getMessage();
            response.checkErrorResponse(code, message);

            // ComplexTypePropertyの作成
            ComplexTypePropertyUtils.create(Setup.TEST_CELL1, Setup.TEST_BOX1, odataName, complexTypePropertyName,
                    complexTypeName, "Edm.String", HttpStatus.SC_CREATED);

            // Odataの削除→子供（ComplexType + ComplexTypeProeprty）があるため削除出来ない
            response = deleteTest(odataName, HttpStatus.SC_FORBIDDEN);
            code = PersoniumCoreException.Dav.HAS_CHILDREN.getCode();
            message = PersoniumCoreException.Dav.HAS_CHILDREN.getMessage();
            response.checkErrorResponse(code, message);

            // ComplexTypePropertyの削除
            ComplexTypePropertyUtils.delete(Setup.TEST_CELL1, Setup.TEST_BOX1, odataName,
                    complexTypePropertyName, complexTypeName, HttpStatus.SC_NO_CONTENT);

            // ComplexTypeの削除
            ComplexTypeUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    odataName, complexTypeName, HttpStatus.SC_NO_CONTENT);

            // Odataの削除→子供（ComplexType + ComplexTypeProeprty）が無いため削除出来る
            deleteTest(odataName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // ComplexTypePropertyの削除
            ComplexTypePropertyUtils.delete(Setup.TEST_CELL1, Setup.TEST_BOX1, odataName,
                    complexTypePropertyName, complexTypeName, -1);

            // ComplexTypeの削除
            ComplexTypeUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    odataName, complexTypeName, -1);

            // Odataの削除
            deleteTest(odataName, -1);
        }
    }

    /**
     * Serviceコレクション削除時の子供データチェックテスト. 階層の内容↓ deleteService ┗__src ┗davFile.txt
     */
    @Test
    public final void Serviceコレクション削除時の子供データチェックテスト() {

        String sriveceName = "deleteService";
        String src = "__src";
        String davFileName = "davFile.js";
        try {
            // serviceコレクションの作成
            DavResourceUtils.createServiceCollection(TOKEN, HttpStatus.SC_CREATED, sriveceName);

            // ファイルの作成
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, TOKEN, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    sriveceName + "/" + src + "/" + davFileName, HttpStatus.SC_CREATED);

            // serviceコレクションの削除→子供（__src/davFile.js）があるため削除出来ない
            TResponse response = deleteTest(sriveceName, HttpStatus.SC_FORBIDDEN);
            String code = PersoniumCoreException.Dav.HAS_CHILDREN.getCode();
            String message = PersoniumCoreException.Dav.HAS_CHILDREN.getMessage();
            response.checkErrorResponse(code, message);

            // ファイルの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    sriveceName + "/" + src + "/" + davFileName, HttpStatus.SC_NO_CONTENT, Setup.TEST_BOX1);

            // serviceの削除→子供（__src/davFile.js）が無いため削除出来る
            deleteTest(sriveceName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // ファイルの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    sriveceName + "/" + src + "/" + davFileName, -1, Setup.TEST_BOX1);

            // serviceコレクションの削除
            deleteTest(sriveceName, -1);
        }
    }

    /**
     * DELETEの実行.
     * @return DELETEの実行結果
     */
    private TResponse deleteTest(final String path, int code) {
        // コレクションの削除
        return Http.request("box/delete-col.txt")
                .with("cellPath", "testcell1")
                .with("path", path)
                .with("token", TOKEN)
                .returns()
                .statusCode(code);
    }

    /**
     * resourcetypeのタグ内テスト. テスト内容 ・resourcetypeタグの要素数=1 ・resourcetypeタグ内のタグ数=1 ・collectionタグ・serviceTypeタグの存在チェック
     */
    private void serviceColTypeTest(final Document doc, final String serviceType) {

        Element root = doc.getDocumentElement();
        NodeList resourcetypeList = root.getElementsByTagName("resourcetype");

        assertEquals(1, resourcetypeList.getLength());
        Element resourcetype = (Element) resourcetypeList.item(0);
        NodeList children = resourcetype.getChildNodes();
        String dc = null;
        String collection = null;
        int elementCounut = 0;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                elementCounut++;
                Element childElement = (Element) child;
                if (childElement.getTagName().equals(serviceType)) {
                    dc = childElement.getTagName();
                } else if (childElement.getTagName().equals("collection")) {
                    collection = childElement.getTagName();
                }
            }
        }
        assertEquals(2, elementCounut);
        assertNotNull(dc);
        assertNotNull(collection);
    }

    /**
     * depth:1でのPROPFINDのレスポンスbodyテスト.
     */
    private void depthTest(final NodeList nodeList, final String resorce, final String statusLine) {

        String statusResult = "";
        Element resourcetype = null;
        for (int c = 0; c < nodeList.getLength(); c++) {
            resourcetype = (Element) nodeList.item(c);
            NodeList hrefchildren = resourcetype.getElementsByTagName("href");
            assertNotNull(hrefchildren);
            assertEquals(1, hrefchildren.getLength());
            String hrefResult = (hrefchildren.item(0)).getFirstChild().getNodeValue();
            if (hrefResult.equals(resorce)) {
                NodeList statuschild = resourcetype.getElementsByTagName("status");
                assertNotNull(statuschild);
                assertEquals(1, statuschild.getLength());
                statusResult = (statuschild.item(0)).getFirstChild().getNodeValue();
                break;
            }
        }
        assertEquals(statusResult, statusLine);
    }

    /**
     * Depthの異常値テスト.
     */
    @Test
    public final void Depthの異常値のテスト() {
        String path = "depthcol";
        try {
            // コレクションの作成
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // コレクションの取得(depth:infinity)
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "infinity")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_FORBIDDEN);

            // コレクションの取得(depth:error)
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "error")
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // コレクションの取得(depth:null)
            Http.request("box/propfind-col-allprop-depth-null.txt")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteTest(path, -1);
        }
    }

    /**
     * PROPPATCHのテスト. プロパティ設定=>更新=>削除
     */
    @Test
    public final void PROPPATCHのテスト() {
        String path = "patchcol";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            TResponse tresponseWebDav = null;

            // DAVコレクションの作成

            tresponseWebDav = Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag1 = tresponseWebDav.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag1);

            // PROPPATCH設定実行
            tresponseWebDav = Http.request("box/proppatch-set.txt").with("cell", testcell).with("box", boxName)
                    .with("path", path).with("token", TOKEN)
                    .with("author1", "Test User1")
                    .with("hoge", "hoge")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
            String etag2 = tresponseWebDav.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag2);
            assertFalse(etag1.equals(etag2));

            // プロパティ変更の確認
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(testcell, boxName, path);
            HashMap<String, String> map = new HashMap<String, String>();
            // TODO 名前空間のテストを実施する。
            map.put("Author", "Test User1");
            map.put("hoge", "hoge");
            proppatchResponseTest(root, resorce, map);

            // プロパティの変更
            tresponseWebDav = Http.request("box/proppatch-set.txt").with("cell", testcell).with("box", boxName)
                    .with("path", path).with("token", TOKEN)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティの変更確認
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            HashMap<String, String> map2 = new HashMap<String, String>();
            map.put("Author", "Author1 update");
            map.put("hoge", "fuga");
            proppatchResponseTest(root2, resorce, map2);

            // プロパティの削除
            tresponseWebDav = Http.request("box/proppatch-remove.txt").with("path", path).with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティの削除確認
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root3 = tresponseWebDav.bodyAsXml().getDocumentElement();
            HashMap<String, String> map3 = new HashMap<String, String>();
            map.put("Author", null);
            map.put("hoge", null);
            proppatchResponseTest(root3, resorce, map3);

            // set、remove同時実行の実験
            // TODO SETとREMOVEが同時に行われるレスポンスはsetとremoveの内容が同じPROPSTATで返ってきているがわけるべき
            // TODO SETのレスポンス内のプロパティに設定値が入っているのはまずそう。 <key>value</key> => <key/>
            tresponseWebDav = Http.request("box/proppatch.txt").with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1).with("path", path).with("token", "Bearer " + TOKEN)
                    .with("author1", "Author1 update").with("hoge", "fuga")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

        } finally {
            deleteTest(path, -1);
        }
    }

    /**
     * PROPPATCH階層設定のテスト. プロパティ設定=>更新=>削除
     */
    @Test
    public final void PROPPATCH階層設定のテスト() {
        String path = "patchclasscol";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            TResponse tresponseWebDav = null;

            // DAVコレクションの作成
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // PROPPATCH設定実行
            tresponseWebDav = Http.request("box/proppatch-class.txt").with("path", path).with("token", TOKEN)
                    .with("cellPath", "testcell1")
                    .with("name1", "Test User1")
                    .with("src1", "hoge")
                    .with("name2", "Test User2")
                    .with("src2", "fuga")
                    .with("name3", "Test User3")
                    .with("src3", "boy")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティ変更の確認
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(testcell, boxName, path);
            ArrayList<String> list = new ArrayList<String>();
            // TODO 名前空間のテストを実施する。
            list.add("name");
            list.add("Test User1");
            list.add("src");
            list.add("hoge");
            list.add("name");
            list.add("Test User2");
            list.add("src");
            list.add("fuga");
            list.add("name");
            list.add("Test User3");
            list.add("src");
            list.add("boy");
            proppatchClassResponseTest(root, resorce, "service", list);

            // プロパティの変更
            tresponseWebDav = Http.request("box/proppatch-class.txt").with("path", path).with("token", TOKEN)
                    .with("cellPath", "testcell1")
                    .with("name1", "Test User13")
                    .with("src1", "hoge3")
                    .with("name2", "Test User23")
                    .with("src2", "3fuga")
                    .with("name3", "3Test User3")
                    .with("src3", "3boy")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティの変更確認
            tresponseWebDav = Http.request("box/propfind-col-allprop.txt").with("path", path).with("token", TOKEN)
                    .with("depth", "0")
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            ArrayList<String> list2 = new ArrayList<String>();
            list2.add("name");
            list2.add("Test User13");
            list2.add("src");
            list2.add("hoge3");
            list2.add("name");
            list2.add("Test User23");
            list2.add("src");
            list2.add("3fuga");
            list2.add("name");
            list2.add("3Test User3");
            list2.add("src");
            list2.add("3boy");
            proppatchClassResponseTest(root2, resorce, "service", list2);

            // プロパティの削除
            tresponseWebDav = Http.request("box/proppatch-remove.txt").with("path", path).with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

        } finally {
            deleteTest(path, -1);
        }
    }

    /**
     * WebDAV_ACLのテスト.
     */
    @Test
    public final void WebDAV_ACLのテスト() {
        String path = "aclcol1";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            // コレクションの作成
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // ACLの設定
            Http.request("box/acl.txt")
                    .with("colname", path)
                    .with("token", TOKEN)
                    .with("roleBaseUrl", UrlUtils.roleResource(testcell, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // ACLの確認
            TResponse tresponseWebDav = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "1")
                    .with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            String resorce = UrlUtils.box(testcell, boxName, path);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            rolList.add("write");
            map.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role1"), rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("read");
            map2.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role2"), rolList2);
            list.add(map2);

            TestMethodUtils.aclResponseTest(root2, resorce, list, 1,
                    UrlUtils.roleResource(testcell, boxName, ""), null);

        } finally {
            deleteTest(path, -1);
        }
    }

    /**
     * WebDAV_ACLのPricipalフルパスのテスト.
     */
    @Test
    public final void WebDAV_ACLのPricipalフルパスのテスト() {
        String path = "aclcol1";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            // コレクションの作成
            DavResourceUtils.createWebDavCollection("box/mkcol-normal.txt", testcell, path, TOKEN,
                    HttpStatus.SC_CREATED);

            // ACLの設定
            DavResourceUtils.setACLwithRoleBaseUrl(testcell, TOKEN, HttpStatus.SC_OK, boxName, path,
                    "box/acl-setting-baseurl.txt", UrlUtils.roleResource(testcell, null, "role1"), "<D:read/>",
                    "");

            // ACLの確認
            TResponse tresponseWebDav = DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                    HttpStatus.SC_MULTI_STATUS, path);

            // PROPFOINDレスポンスボディの確認
            String resorce = UrlUtils.box(testcell, boxName, path);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            map.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role1"), rolList);
            list.add(map);

            TestMethodUtils.aclResponseTest(root2, resorce, list, 1,
                    UrlUtils.roleResource(testcell, boxName, ""), null);

        } finally {
            deleteTest(path, -1);
        }
    }

    /**
     * Service_ACLのテスト.
     */
    @Test
    public final void Service_ACLのテスト() {
        String path = "aclcol1";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            // コレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // ACLの設定
            Http.request("box/acl-service.txt")
                    .with("colname", path)
                    .with("token", TOKEN)
                    .with("roleBaseUrl", UrlUtils.roleResource(testcell, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // ACLの確認
            TResponse tresponseWebDav = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "1")
                    .with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            String resorce = UrlUtils.box(testcell, boxName, path);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("exec");
            map.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role1"), rolList);
            list.add(map);

            TestMethodUtils.aclResponseTest(root2, resorce, list, 2,
                    UrlUtils.roleResource(testcell, boxName, ""), null);

        } finally {
            deleteTest(path, -1);
        }

    }

    /**
     * OData_ACLのテスト.
     */
    @Test
    public final void OData_ACLのテスト() {
        String path = "aclcol1";
        String testcell = "testcell1";
        String boxName = "box1";

        try {
            // コレクションの作成
            Http.request("box/mkcol-odata.txt")
                    .with("cellPath", "testcell1")
                    .with("boxPath", "box1")
                    .with("path", path)
                    .with("token", TOKEN)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // ACLの設定
            Http.request("box/acl.txt")
                    .with("colname", path)
                    .with("token", TOKEN)
                    .with("roleBaseUrl", UrlUtils.roleResource(testcell, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // ACLの確認
            TResponse tresponseWebDav = Http.request("box/propfind-col-allprop.txt")
                    .with("path", path)
                    .with("depth", "1")
                    .with("token", TOKEN)
                    .returns();
            tresponseWebDav.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            String resorce = UrlUtils.box(testcell, boxName, path);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            rolList.add("write");
            map.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role1"), rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("read");
            map2.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role2"), rolList2);
            list.add(map2);

            TestMethodUtils.aclResponseTest(root2, resorce, list, 1,
                    UrlUtils.roleResource(testcell, boxName, ""), null);

        } finally {
            deleteTest(path, -1);
        }

    }

    /**
     * ROLEが存在しない場合でのACLのテスト.
     */
    @Test
    public final void ROLEが存在しない場合でのACLのテスト() {
        String path = "setodata";
        String testcell = "testcell1";

        // ACLの設定
        Http.request("box/acl-role-not-found.txt")
                .with("colname", path)
                .with("token", TOKEN)
                .with("roleBaseUrl", UrlUtils.roleResource(testcell, null, ""))
                .with("level", "")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    /**
     * xml:baseに存在しないCellを指定した場合でのACLのテスト.
     */
    @Test
    public final void xml_baseに存在しないCellを指定した場合でのACLのテスト() {
        // ACLの設定
        Http.request("box/acl-setting-single-collection.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("col", Setup.TEST_ODATA)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", UrlUtils.roleResource("notExistsCell", null, "role1"))
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * xml_baseに存在しないBoxを指定した場合でのACLのテスト.
     */
    @Test
    public final void xml_baseに存在しないBoxを指定した場合でのACLのテスト() {
        // ACLの設定
        Http.request("box/acl-setting-single-collection.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("col", Setup.TEST_ODATA)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", UrlUtils.roleResource(Setup.TEST_CELL1, "notExistsBox", "role1"))
                .with("level", "")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * xml_baseに存在しないCollectionを指定した場合でのACLのテスト.
     */
    @Test
    public final void xml_baseに存在しないCollectionを指定した場合でのACLのテスト() {
        // ACLの設定
        Http.request("box/acl-setting-href-with-baseurl.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("col", Setup.TEST_ODATA)
                .with("role", "notExistsCol/../__/role1")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", UrlUtils.roleResource(Setup.TEST_CELL1, null, ""))
                .with("level", "")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Roleに紐付いていないBox名を指定した場合でのACLのテスト.
     */
    @Test
    public final void Roleに紐付いていないBox名を指定した場合でのACLのテスト() {
        String testBox = "testBox01";
        String testRole = "testRole01";
        try {
            // Boxの作成
            BoxUtils.create(Setup.TEST_CELL1, testBox, TOKEN);

            // Boxに紐付かないRoleの作成
            RoleUtils.create(Setup.TEST_CELL1, TOKEN, testRole, null, HttpStatus.SC_CREATED);

            // ACLの設定
            Http.request("box/acl-setting-single-collection.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("col", Setup.TEST_ODATA)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(Setup.TEST_CELL1, testBox, testRole))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // Roleの削除
            RoleUtils.delete(Setup.TEST_CELL1, TOKEN, testRole, null);

            // Box1の削除
            BoxUtils.delete(Setup.TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * BOX_ACLのテスト.
     */
    @Test
    public final void Box_ACLのテスト() {
        String path = "box1";
        String testcell = "testcell1";

        try {

            // ACLの設定
            Http.request("box/acl.txt")
                    .with("colname", "")
                    .with("token", TOKEN)
                    .with("roleBaseUrl", UrlUtils.roleResource(testcell, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // ACLの確認
            TResponse tresponseWebDav = CellUtils.propfind(testcell + "/" + path,
                    TOKEN, "0", HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            String resorce = UrlUtils.boxRoot(testcell, path);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            rolList.add("write");
            map.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role1"), rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("read");
            map2.put(UrlUtils.aclRelativePath(Box.DEFAULT_BOX_NAME, "role2"), rolList2);
            list.add(map2);

            TestMethodUtils.aclResponseTest(root2, resorce, list, 1,
                    UrlUtils.roleResource(testcell, path, ""), null);

        } finally {
            // ACLの設定を下に戻す
            DavResourceUtils.setACL(testcell, AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK, "", ACL_AUTH_TEST_SETTING_FILE, Setup.TEST_BOX1,
                    "");
        }
    }

    /**
     * BOX_ACL異常系_大文字やハイフンを含むRole名をACL登録できることの確認.
     */
    @Test
    public final void BOX_ACL異常系_大文字やハイフンを含むRole名をACL登録できることの確認() {
        String path = "box1";
        String testcell = "testcell1";
        String token = AbstractCase.MASTER_TOKEN_NAME;

        String[] roles = {"RoleName", "role-name" };
        try {
            for (String role : roles) {
                try {
                    // 前準備
                    // 名前に大文字を含むロール登録
                    RoleUtils.create(testcell, token, role, path, HttpStatus.SC_CREATED);

                    // ACLの設定
                    DavResourceUtils.setACLwithBox(testcell, token, HttpStatus.SC_OK,
                            path, "", "box/acl-setting.txt", role, path, "<D:write/>", "");

                    // ACLの確認
                    TResponse tresponseWebDav = CellUtils.propfind(testcell + "/" + path,
                            TOKEN, "0", HttpStatus.SC_MULTI_STATUS);

                    // PROPFOINDレスポンスボディの確認
                    String resorce = UrlUtils.boxRoot(testcell, path);
                    Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
                    List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
                    Map<String, List<String>> map = new HashMap<String, List<String>>();
                    List<String> rolList = new ArrayList<String>();
                    rolList.add("write");
                    map.put(role, rolList);
                    list.add(map);

                    TestMethodUtils.aclResponseTest(root, resorce, list, 1,
                            UrlUtils.roleResource(testcell, path, ""), null);

                } finally {
                    // Role削除
                    RoleUtils.delete(testcell, token, role, path);
                }
            }
        } finally {
            // ACL設定を元に戻す
            DavResourceUtils.setACL(testcell, AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK, "", ACL_AUTH_TEST_SETTING_FILE, Setup.TEST_BOX1,
                    "");
        }
    }

    /**
     * PropFindとPropPatchの権限テスト.
     */
    @Test
    public final void PropFindとPropPatchの権限テスト() {
        final String accountRead = "accountRead";
        final String accountReadAcl = "accountReadAcl";
        final String testCol = "testCol";
        try {
            // Accountを作成する
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    accountRead, accountRead, HttpStatus.SC_CREATED);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    accountReadAcl, accountReadAcl, HttpStatus.SC_CREATED);

            // ロール追加（Read）
            RoleUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "roleRead",
                    null, HttpStatus.SC_CREATED);
            // ロール追加（Read-Acl）
            RoleUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "roleReadAcl",
                    null, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountRead, null, "roleRead", HttpStatus.SC_NO_CONTENT);
            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountReadAcl, null, "roleReadAcl", HttpStatus.SC_NO_CONTENT);

            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, testCol);
            // ACL設定
            DavResourceUtils.setACL(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, testCol,
                    ACL_AUTH_PROPPATCH_TEST_SETTING_FILE, Setup.TEST_BOX1, "");
            // 読み込みのみ
            String readToken = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, accountRead, accountRead);
            // 読み込みとACL読み込み
            String readAclToken = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, accountReadAcl, accountReadAcl);

            // PropFind
            TResponse res = DavResourceUtils.propfind("box/propfind-col-allprop.txt", readToken,
                    HttpStatus.SC_MULTI_STATUS, testCol);

            // PROPFIND(XML形式)のACL部分のみ文字列操作で抽出する
            // ACLタグが存在しない事を確認する
            NodeList list = res.bodyAsXml().getElementsByTagNameNS("DAV:", "acl");
            assertTrue(res.getBody(), list.getLength() == 0);

            TResponse res2 = DavResourceUtils.propfind("box/propfind-col-allprop.txt", readAclToken,
                    HttpStatus.SC_MULTI_STATUS, testCol);
            // PROPFIND(XML形式)のACL部分のみ文字列操作で抽出する
            // ACLタグが存在することを確認する
            NodeList list2 = res2.bodyAsXml().getElementsByTagNameNS("DAV:", "acl");
            assertTrue(res2.getBody(), list2.getLength() > 0);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, testCol,
                    AbstractCase.MASTER_TOKEN_NAME, -1);
            // 結びつけの解除
            ResourceUtils.linkAccountRollDelete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountReadAcl, null, "roleReadAcl");
            ResourceUtils.linkAccountRollDelete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountRead, null, "roleRead");
            // Roleの削除
            RoleUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "roleRead", null);
            RoleUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "roleReadAcl", null);
            // Accountの削除
            AccountUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountReadAcl, HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    accountRead, HttpStatus.SC_NO_CONTENT);

        }
    }

    /**
     * ContentLengthが未設定の場合のPROPFIND確認テスト.
     */
    @Test
    public final void ContentLengthが未設定の場合のPROPFIND確認テスト() {
        TResponse tresponse = Http.request("box/propfind-non-content-length.txt")
                .with("path", Setup.TEST_ODATA)
                .with("depth", "0")
                .with("token", TOKEN)
                .returns();
        tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

    }

    /**
     * bodyのpropfindタグが不正の場合にPROPFINDで400になる確認.
     */
    @Test
    public final void bodyのpropfindタグが不正の場合にPROPFINDで400になる確認() {

        // 正 propfind
        // 誤 propfinds
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfinds xmlns:D=\"DAV:\"><D:allprop/></D:propfinds>";

        TResponse tresponse = Http.request("box/propfind-body.txt")
                .with("path", Setup.TEST_ODATA)
                .with("depth", "0")
                .with("token", TOKEN)
                .with("body", body)
                .returns();
        tresponse.statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    /**
     * bodyのallpropタグが不正の場合にPROPFINDで400になる確認. TODO V1.1 PROPFINDの正式実装後有効化する
     */
    @Test
    @Ignore
    public final void bodyのallpropタグが不正の場合にPROPFINDで400になる確認() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprope/></D:propfind>";

        TResponse tresponse = Http.request("box/propfind-body.txt")
                .with("path", Setup.TEST_ODATA)
                .with("depth", "0")
                .with("token", TOKEN)
                .with("body", body)
                .returns();
        tresponse.statusCode(HttpStatus.SC_BAD_REQUEST);

    }

    /**
     * 存在しないパスにPUTで409が返る事を確認する.
     */
    @Test
    public final void 存在しないパスにPUTで409が返る事を確認する() {

        String davFileName = "davFile.txt";

        // 存在しないWebDavコレクションへのDavファイルの作成
        // testcell1/box1/hogecol/davFile.txt
        DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, TOKEN, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                "hogecol" + "/" + davFileName, HttpStatus.SC_CONFLICT);
    }

    /**
     * 存在しないパスにMKCOLで409が返る事を確認する.
     */
    @Test
    public final void 存在しないパスにMKCOLで409が返る事を確認する() {

        String colName = "colHoge/colhuga";

        // 存在しないパスへのOdataコレクションの作成
        // testcell1/box1/colHoge/colhuga
        DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CONFLICT, Setup.TEST_CELL1, Setup.TEST_BOX1,
                colName);
    }

    /**
     * 存在しない階層パスにPUTで409が返る事を確認する.
     */
    @Test
    public final void 存在しない階層パスにPUTで409が返る事を確認する() {

        String davFileName = "davFile.txt";

        // 存在しないWebDavコレクションへのDavファイルの作成
        // testcell1/box1/hogecol/davFile.txt
        DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, TOKEN, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                "hogecol" + "/" + "hugacol/" + davFileName, HttpStatus.SC_CONFLICT);
    }

    /**
     * XMLから指定したタグ名の要素を返す.
     * @param doc ドキュメント
     * @param tagName タグ名
     * @return result 要素の値
     */
    private String getXmlNodeValue(final Document doc, final String tagName) {
        return DavResourceUtils.getXmlNodeValue(doc, tagName);
    }

    /**
     * PROPPATCHの返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce PROPPATCHを設定したリソースパス
     * @param map チェックするプロパティのKeyValue KeyとValueに値を入れれば値があることのチェック ValueをnullにするとKeyが無いことのチェック（removeの確認に使う）
     */
    private void proppatchResponseTest(Element doc, String resorce, Map<String, String> map) {
        NodeList response = doc.getElementsByTagName("response");
        assertEquals(1, response.getLength());
        Element node = (Element) response.item(0);
        assertEquals(
                resorce,
                node.getElementsByTagName("href").item(0).getFirstChild().getNodeValue());
        assertEquals(
                "HTTP/1.1 200 OK",
                node.getElementsByTagName("status").item(0).getFirstChild().getNodeValue());

        for (Iterator<String> it = map.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = map.get(key);
            String textContext = null;
            NodeList tmp = node.getElementsByTagName("prop").item(0).getChildNodes();
            for (int i = 0; i < tmp.getLength(); i++) {
                Node child = tmp.item(i);
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    if (childElement.getLocalName().equals(key)) {
                        textContext = childElement.getTextContent();
                        break;
                    }
                }
            }
            assertEquals(value, textContext);
        }
    }

    /**
     * PROPPATCH階層型の返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce PROPPATCHを設定したリソースパス
     * @param lapName propタグ内でプロパティをラップしているタグ名
     * @param map チェックするプロパティのKeyValue KeyとValueに値を入れれば値があることのチェック ValueをnullにするとKeyが無いことのチェック（removeの確認に使う）
     */
    private void proppatchClassResponseTest(Element doc, String resorce, String lapName, ArrayList<String> list) {
        NodeList response = doc.getElementsByTagName("response");
        assertEquals(1, response.getLength());
        Element node = (Element) response.item(0);
        assertEquals(
                resorce,
                node.getElementsByTagName("href").item(0).getFirstChild().getNodeValue());
        assertEquals(
                "HTTP/1.1 200 OK",
                node.getElementsByTagName("status").item(0).getFirstChild().getNodeValue());

        String lapNameFlg = null;
        NodeList tmp = node.getElementsByTagName("prop").item(0).getChildNodes();
        for (int i = 0; i < tmp.getLength(); i++) {
            Node child = tmp.item(i);
            if (child instanceof Element) {
                // プロパティをラップしているタグ名判断
                Element childElement = (Element) child;
                if (childElement.getLocalName().equals(lapName)) {
                    lapNameFlg = "true";
                    // Pathタグの内のプロパティ判断
                    NodeList pTmp = childElement.getChildNodes();
                    for (int ii = 0; ii < pTmp.getLength(); ii++) {
                        Node pChild = pTmp.item(ii);
                        if (pChild instanceof Element) {
                            Element pChildElement = (Element) pChild;
                            NamedNodeMap attrs = pChildElement.getAttributes();
                            if (attrs != null) {
                                String falg = null;
                                for (Iterator<String> it = list.listIterator(); it.hasNext();) {
                                    Object key = it.next();
                                    Object value = it.next();
                                    Node attr = (Node) attrs.getNamedItem((String) key);
                                    if (attr != null && attr.getNodeValue().equals(value)) {
                                        falg = "true";
                                    }
                                }
                                assertNotNull(falg);
                            }
                        }
                    }
                }
            }
        }
        // プロパティをラップしているタグ名判断
        assertNotNull(lapNameFlg);
    }

    /**
     * entityTypeの削除.
     * @param odataName
     * @param token
     * @param accept
     * @return
     */
    private TResponse entityTypeDelete(final String odataName, final String token,
            final String accept, final String entSetName, final String cellPath, final int code) {
        TResponse tresponse = Http.request("box/entitySet-delete.txt")
                .with("cellPath", cellPath)
                .with("boxPath", "box1")
                .with("odataSvcPath", odataName)
                .with("token", token)
                .with("accept", accept)
                .with("Name", entSetName)
                .returns()
                .statusCode(code);
        return tresponse;
    }
}
