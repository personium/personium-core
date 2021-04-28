/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.test.jersey;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.setup.Setup;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * OPTIONSメソッドに関するテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class OptionsMethodTest extends PersoniumTest {

    /**
     * Constructor.
     */
    public OptionsMethodTest() {
        super(new PersoniumCoreApplication());
        RuntimeDelegate.setInstance(new org.glassfish.jersey.internal.RuntimeDelegateImpl());
    }

//    @Override
//    protected Application configure() {
//        return new PersoniumCoreApplication();
//    }
//    private WebAppDescriptor initApplication() {
//        new WebAppDescriptor.Builder();
//
//        PersoniumCoreApplication application = new PersoniumCoreApplication();
//        List<String> classList = new ArrayList<String>();
//        for (Class<?> clazz : application.getClasses()) {
////            builder.append(test.getPackage().getName());
////            builder.append(".");
//            classList.add(clazz.getName());
//        }
//        return classList;
//    }

    /**
     * OPTIONSメソッドの実行.
     * @param path リソースパス
     * @return Tresponseオブジェクト
     */
    public static TResponse optionsRequest(final String path) {
        return optionsRequest(path, HttpStatus.SC_OK);
    }

    /**
     * OPTIONSメソッドの実行.
     * @param path リソースパス
     * @param code 期待するレスポンスコード
     * @return Tresponseオブジェクト
     */
    public static TResponse optionsRequest(final String path, final int code) {
        TResponse res = Http.request("options.txt")
                .with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * 認証なしのOPTIONSメソッドがリクエストされた場合にpersoniumで受け付けている全メソッドが返却されること.
     * @throws URISyntaxException URISyntaxException
     */
    @Test
    public void 認証なしのOPTIONSメソッドがリクエストされた場合にpersoniumで受け付けている全メソッドが返却されること() throws URISyntaxException {
        TResponse response = Http.request("options-noauth.txt")
                .with("path", "/")
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
        assertTrue(checkResponse(response, "OPTIONS,GET,POST,PUT,DELETE,HEAD,MERGE,MKCOL,MOVE,PROPFIND,PROPPATCH,ACL"));
    }

    /**
     * 認証ありのユニットレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること.
     */
    @Test
    public void 認証ありのユニットレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること() {
        // Unitスキーマ
        assertTrue(checkResponse(optionsRequest("/__ctl/\\$metadata"), "OPTIONS,GET"));
        // Cell
        assertTrue(checkResponse(optionsRequest("/__ctl/Cell"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/__ctl/Cell('testcell1')"), "OPTIONS,GET,PUT,MERGE,DELETE"));
    }

    /**
     * 認証ありのセルレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること.
     */
    @Test
    public void 認証ありのセルレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること() {
        // Cellスキーマ
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/\\$metadata"), "OPTIONS,GET"));
        // Account
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Account"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Account('account1')"),
                "OPTIONS,GET,PUT,MERGE,DELETE"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Account('account1')/_Role"), "OPTIONS,GET,POST"));
        // Role(NavigationProperty) ※セル制御レベルで共通
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Role('role1')/_Box"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Role('role1')/_Account"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Role('role1')/_ExtCell"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Role('role1')/_Relation"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Role('role1')/_ExtRole"), "OPTIONS,GET,POST"));
        // Box
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Box"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/Box('box1')"), "OPTIONS,GET,PUT,MERGE,DELETE"));
        // TODO Message 204が返却される
        // assertTrue(checkResponse(optionsRequest("/testcell1/__message"), "OPTIONS"));
        // assertTrue(checkResponse(optionsRequest("/testcell1/__message/send"), "OPTIONS,POST"));
        // assertTrue(checkResponse(optionsRequest("/testcell1/__message/port"), "OPTIONS,POST"));
        // assertTrue(checkResponse(optionsRequest("/testcell1/__message/received/id"), "OPTIONS,POST"));
        // TODO POSTは未提供メソッドのため返却してはいけない
        assertTrue(checkResponse(optionsRequest("/testcell1/__ctl/ReceivedMessage"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(
                optionsRequest("/testcell1/__ctl/ReceivedMessage('id')"), "OPTIONS,GET,PUT,MERGE,DELETE"));
        assertTrue(checkResponse(
                optionsRequest("/testcell1/__ctl/ReceivedMessage('id')/_AccountRead"), "OPTIONS,GET,POST"));
        // $links
        assertTrue(checkResponse(
                optionsRequest("/testcell1/__ctl/Role('id')/\\$links/_Box"), "OPTIONS,GET,DELETE,PUT,POST"));
        assertTrue(checkResponse(
                optionsRequest("/testcell1/__ctl/Role('id')/\\$links/_Box('id')"), "OPTIONS,GET,DELETE,PUT,POST"));
    }

    /**
     * 認証ありのユーザODATAレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること.
     */
    @Test
    public void 認証ありのユーザODATAレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること() {
        // Collection
        assertTrue(checkResponse(
                optionsRequest("/testcell1/box1/setodata"), "OPTIONS,GET,DELETE,MOVE,PROPFIND,PROPPATCH,ACL"));
        // ユーザスキーマ
        assertTrue(checkResponse(optionsRequest("/testcell1/box1/setodata/\\$metadata"), "OPTIONS,GET"));
        // TODO 204が返却される
        // assertTrue(checkResponse(optionsRequest("/testcell1/box1/setodata/\\$metadata/\\$metadata"), "OPTIONS,GET"));
        // Entity（他のスキーマ定義と共通）
        assertTrue(checkResponse(
                optionsRequest("/testcell1/box1/setodata/\\$metadata/EntityType"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/\\$metadata/EntityType('id')"),
                "OPTIONS,GET,PUT,MERGE,DELETE"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/\\$metadata/EntityType('id')/_AssociationEnd"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/\\$metadata/EntityType('id')/_Property"), "OPTIONS,GET,POST"));
        // ユーザデータ
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/Category"), "OPTIONS,GET,POST"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/Category('id')"), "OPTIONS,GET,PUT,MERGE,DELETE"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/Price('id')/_Sales"), "OPTIONS,GET,POST"));
        // $links
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/Price('id')/\\$links/_Sales"),
                "OPTIONS,GET,DELETE,PUT,POST"));
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setodata/Price('id')/\\$links/_Sales('id')"),
                "OPTIONS,GET,DELETE,PUT,POST"));
    }

    /**
     * 認証ありのDAV_SVCレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること.
     */
    @Test
    public void 認証ありのDAV_SVCレベルOPTIONSメソッドがpersoniumで受け付けているメソッドが返却されること() {
        // Collection(WebDAV/Service)
        assertTrue(checkResponse(
                optionsRequest("/testcell1/box1/setdavcol"),
                "OPTIONS,GET,PUT,DELETE,MKCOL,MOVE,PROPFIND,PROPPATCH,ACL"));
        assertTrue(checkResponse(
                optionsRequest("/testcell1/box1/service_relay"), "OPTIONS,DELETE,MOVE,PROPFIND,PROPPATCH,ACL"));

        // Serviceソースコレクション
        assertTrue(checkResponse(
                optionsRequest("/testcell1/box1/service_relay/__src"), "OPTIONS,PROPFIND"));

        // WebDAVファイル
        assertTrue(checkResponse(optionsRequest(
                "/testcell1/box1/setdavcol/dav.txt"),
                "OPTIONS,GET,PUT,DELETE,MOVE,PROPFIND,PROPPATCH,ACL"));

        // TODO サービスソース 204が返却される
        // assertTrue(checkResponse(optionsRequest("/testcell1/box1/service_relay/svc"),
        // "OPTIONS,GET,POST,PUT,DELETE,HEAD"));

        // __src配下
        try {
            String path = String.format("%s/service_relay/__src/test.js", Setup.TEST_BOX1);
            DavResourceUtils.createWebDavFile(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    path, "hello", "text/javascript", HttpStatus.SC_CREATED);
            assertTrue(checkResponse(
                    optionsRequest("/testcell1/box1/service_relay/__src/test.js"),
                    "OPTIONS,GET,PUT,DELETE,MOVE,PROPFIND,PROPPATCH,ACL"));
        } finally {
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    "service_relay/__src/test.js");

        }
    }

    private boolean checkResponse(TResponse res, String methodStr) {
        String values = res.getHeader("Access-Control-Allow-Methods");
        if (methodStr == null || methodStr.length() == 0) {
            return (values.length() == 0);
        } else {
            return methodStr.equals(values.replaceAll(" ", ""));
        }
    }
}
