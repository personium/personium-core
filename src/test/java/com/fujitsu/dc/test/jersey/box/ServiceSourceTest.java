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
package com.fujitsu.dc.test.jersey.box;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * MKCOLのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ServiceSourceTest extends JerseyTest {

    String testcell = "testcell1";
    String boxName = "box1";
    String serviceColPath = "servicecol";
    String srcPath = "servicecol/__src";
    String resourcePath = "servicecol/__src/hello.js";
    String jsSource = "function(request){return {status: 200,"
            + "headers: {\"Content-Type\":\"text/html\"},body: [\"hello world!\"]};}";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ.
     */
    public ServiceSourceTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ServiceSourceのPROPFINDで設定が取得できること.
     */
    @Test
    public final void ServiceSourceのPROPFINDで設定が取得できること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // サービスソースの取得（bodyあり）
            Http.request("box/propfind-col-allprop.txt")
                    .with("path", srcPath)
                    .with("depth", "0")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);
        } finally {
            // サービスコレクションの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSource内のCollection作成が405になること.
     */
    @Test
    public final void ServiceSource内のCollection作成が405になること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // サービスソースコレクションに対してMKCOLを実施
            Http.request("box/mkcol-normal.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            // サービスコレクションの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceにJavascriptファイルを登録ができること.
     */
    @Test
    public final void ServiceSourceにJavascriptファイルを登録ができること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);
        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceにJavascriptファイルを更新ができること.
     */
    @Test
    public final void ServiceSourceにJavascriptファイルを更新ができること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // 登録したJavascriptソースを更新
            String updateSource = "function(request){return {status: 200,"
                    + "headers: {\"Content-Type\":\"text/html\"},body: [\"hi world!\"]};}";
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", updateSource)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 更新したソースを取得して、変更が反映されていることを確認
            TResponse response = Http.request("box/dav-get.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            response.checkHeader(HttpHeaders.CONTENT_TYPE, "text/javascript");
            assertEquals(updateSource, response.getBody().trim());

        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceにJavascriptファイルを取得ができること.
     */
    @Test
    public final void ServiceSourceにJavascriptファイルを取得ができること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースを取得
            TResponse response = Http.request("box/dav-get.txt")
                    .with("cellPath", "testcell1")
                    .with("box", "box1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            response.checkHeader(HttpHeaders.CONTENT_TYPE, "text/javascript");
            assertEquals(jsSource, response.getBody().trim());

        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceの存在しないファイル取得が404になること.
     */
    @Test
    public final void ServiceSourceの存在しないファイル取得が404になること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースを取得
            Http.request("box/dav-get.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", "box1")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceにJavascriptファイルを削除ができること.
     */
    @Test
    public final void ServiceSourceにJavascriptファイルを削除ができること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースを削除
            Http.request("box/dav-delete.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", "box1")
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceの存在しないファイル削除が404になること.
     */
    @Test
    public final void ServiceSourceの存在しないファイル削除が404になること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースを削除
            Http.request("box/dav-delete.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", "box1")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceに登録したJavascriptファイルにPROPPATCHして設定追加できること.
     */
    @Test
    public final void ServiceSourceに登録したJavascriptファイルにPROPPATCHして設定追加できること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // プロパティの追加
            Http.request("box/proppatch-set.txt")
                    .with("cell", "testcell1")
                    .with("box", "box1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("author1", "Test User1")
                    .with("hoge", "hoge")
                    .returns()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティ変更の確認
            TResponse tresponseWebDav = Http.request("box/propfind-col-allprop.txt")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("depth", "0")
                    .returns()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);

            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resource = UrlUtils.box(testcell, boxName, resourcePath);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("Author", "Test User1");
            map.put("hoge", "hoge");
            checkProppatchResponse(root, resource, map);
        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceに登録したJavascriptファイルにPROPPATCHして設定削除できること.
     */
    @Test
    public final void ServiceSourceに登録したJavascriptファイルにPROPPATCHして設定削除できること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースに登録
            Http.request("box/dav-put.txt")
                    .with("cellPath", "testcell1")
                    .with("path", resourcePath)
                    .with("contentType", "text/javascript")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("source", jsSource)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // プロパティの追加
            Http.request("box/proppatch-set.txt")
                    .with("cell", "testcell1")
                    .with("box", "box1")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("author1", "Test User1")
                    .with("hoge", "hoge")
                    .returns()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティの削除
            Http.request("box/proppatch-remove.txt")
                    .with("path", resourcePath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);

            // プロパティの削除確認
            TResponse tresponseWebDav =
                    Http.request("box/propfind-col-allprop.txt")
                            .with("path", resourcePath)
                            .with("token", AbstractCase.MASTER_TOKEN_NAME)
                            .with("depth", "0")
                            .returns()
                            .statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resource = UrlUtils.box(testcell, boxName, resourcePath);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("Author", null);
            map.put("hoge", null);
            checkProppatchResponse(root, resource, map);

        } finally {
            // Jsソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, MASTER_TOKEN,
                    resourcePath, -1, Setup.TEST_BOX1);
            // Collectionの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * ServiceSourceの存在しない階層パス指定で404が返却されること.
     */
    @Test
    public final void ServiceSourceの存在しない階層パス指定で404が返却されること() {
        try {
            // サービスコレクションの作成
            Http.request("box/mkcol-service.txt")
                    .with("cellPath", "testcell1")
                    .with("path", serviceColPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Javascriptソースをサービスソースを取得
            Http.request("box/dav-get.txt")
                    .with("cellPath", "testcell1")
                    .with("path", srcPath + "/test/test/test.js")
                    .with("box", "box1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        } finally {
            // Boxの削除
            deleteCollection(serviceColPath);
        }
    }

    /**
     * CollectionDELETEの実行.
     */
    private void deleteCollection(final String path) {
        // Boxの削除
        Http.request("box/delete-col.txt")
                .with("cellPath", "testcell1")
                .with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * PROPPATCHの返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce PROPPATCHを設定したリソースパス
     * @param map チェックするプロパティのKeyValue
     *        KeyとValueに値を入れれば値があることのチェック
     *        ValueをnullにするとKeyが無いことのチェック（removeの確認に使う）
     */
    private void checkProppatchResponse(Element doc, String resorce, Map<String, String> map) {
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
}
