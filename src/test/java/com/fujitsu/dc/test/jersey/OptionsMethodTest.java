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
package com.fujitsu.dc.test.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * OPTIONSメソッドに関するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class OptionsMethodTest extends JerseyTest {

    /**
     * Constructor.
     */
    public OptionsMethodTest() {
        super("com.fujitsu.dc.core.rs");
    }

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
        // 被テストオブジェクトを準備
        DcCoreContainerFilter containerFilter = new DcCoreContainerFilter();
        // ContainerRequiestを準備
        WebApplication wa = mock(WebApplication.class);
        InBoundHeaders headers = new InBoundHeaders();
        // X-FORWARDED-* 系のヘッダ設定
        String scheme = "https";
        String host = "example.org";
        headers.add(DcCoreUtils.HttpHeaders.X_FORWARDED_PROTO, scheme);
        headers.add(DcCoreUtils.HttpHeaders.X_FORWARDED_HOST, host);
        ContainerRequest request = new ContainerRequest(wa, HttpMethod.OPTIONS,
                new URI("http://dc1.example.com/hoge"),
                new URI("http://dc1.example.com/hoge/hoho"),
                headers, null);
        // HttpServletRequestのmockを準備
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        when(mockServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://dc1.example.com"));
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getContextPath()).thenReturn("");
        when(mockServletRequest.getServletContext()).thenReturn(mockServletContext);
        containerFilter.setHttpServletRequest(mockServletRequest);
        try {
            containerFilter.filter(request);
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            assertEquals(response.getStatus(), HttpStatus.SC_OK);
            MultivaluedMap<String, Object> meta = response.getMetadata();
            List<Object> values = meta.get("Access-Control-Allow-Methods");
            assertEquals(values.size(), 1);
            String value = (String) values.get(0);
            String[] methods = value.split(",");
            Map<String, String> masterMethods = new HashMap<String, String>();
            masterMethods.put(HttpMethod.OPTIONS, "");
            masterMethods.put(HttpMethod.GET, "");
            masterMethods.put(HttpMethod.POST, "");
            masterMethods.put(HttpMethod.PUT, "");
            masterMethods.put(HttpMethod.DELETE, "");
            masterMethods.put(HttpMethod.HEAD, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MERGE, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MKCOL, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MOVE, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH, "");
            masterMethods.put(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.ACL, "");
            for (String method : methods) {
                if (method.trim() == "") {
                    continue;
                }
                String m = masterMethods.remove(method.trim());
                if (m == null) {
                    fail("Method " + method + " is not defined.");
                }
            }
            if (!masterMethods.isEmpty()) {
                fail("UnExcpected Error.");
            }
        }
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
