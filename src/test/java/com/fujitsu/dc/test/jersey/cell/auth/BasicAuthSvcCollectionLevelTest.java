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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * Basic認証のBoxレベル_Serviceコレクションリソースに対するテスト.
 * テストで使用するセル・ボックスは、他のBasic認証テストと同じものを使用する。
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthSvcCollectionLevelTest extends JerseyTest {

    private String cellName = Setup.TEST_CELL_BASIC;
    private String boxName = Setup.TEST_BOX1;
    private String colName = "setsvccol";
    private String fileName = "basicAuthResourceTestFile";

    private String userName = "account4";
    private String password = "password4";
    private String invalidPassword = "invalidPassword";
    private String token = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, password).getBytes());
    private String tokenForACLWrite = "Basic " + Base64.encodeBase64String(("account7:password7").getBytes());
    private String invalidToken = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, invalidPassword).getBytes());;

    /**
     * コンストラクタ.
     */
    public BasicAuthSvcCollectionLevelTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Basic認証_スキーマ情報を持たないBox配下_DavコレクションレベルAPIの操作.
     */
    @Test
    public final void Basic認証_スキーマ情報を持たないBox配下_DavコレクションレベルAPIの操作() {
        // スキーマなしBox配下のDavコレクション
        svcCollectionValidate();
        svcSourceCollectionValidate();
        svcSourceValidate();
    }

    /**
     * Basic認証ースキーマなしBox配下のサービスコレクションの操作.
     */
    private void svcCollectionValidate() {
        String thisMethodColName = "basicAuthResourceSvcCollection";
        // Serviceコレクションの操作
        try {
            // Serviceコレクション
            // コレクション作成(Basic認証-成功)
            DavResourceUtils.createServiceCollection(token,
                    HttpStatus.SC_CREATED, cellName, boxName, colName);
            // コレクション作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createServiceCollection(invalidToken,
                    HttpStatus.SC_UNAUTHORIZED, cellName, boxName, thisMethodColName);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName,
                    boxName + "/" + colName, 1, HttpStatus.SC_MULTI_STATUS);
            // コレクションPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + colName, 1, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", colName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // コレクションPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", colName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, colName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            // コレクションACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, colName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションOPTIONS(Basic認証-成功)
            ResourceUtils.optionsWithAnyAuthSchema(cellName, token, colName, HttpStatus.SC_OK);
            // コレクションOPTIONS(Basic認証-失敗)
            res = ResourceUtils.optionsWithAnyAuthSchema(cellName, invalidToken, colName,
                    HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクション削除(Basic認証-成功)
            DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, colName, token,
                    HttpStatus.SC_NO_CONTENT);
            // コレクション削除(Basic認証-失敗)
            res = DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, colName, invalidToken,
                    HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteCollection(cellName, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Basic認証ースキーマなしBox配下のサービスソースコレクションの操作.
     */
    private void svcSourceCollectionValidate() {
        final String srcCol = colName + "/__src/";
        final String srcFile = srcCol + fileName;
        try {
            // サービスコレクション作成(Basic認証-成功)
            DavResourceUtils.createServiceCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);
            // ファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge", boxName,
                    srcFile, HttpStatus.SC_CREATED);

            // サービスソースコレクションをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token,
                    cellName, boxName + "/" + srcCol, 1, HttpStatus.SC_MULTI_STATUS);
            // サービスソースコレクションをPROPFIND(Basic認証-失敗)
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken,
                    cellName, boxName + "/" + srcCol, 1, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // サービスソースコレクションOPTIONS(Basic認証-成功)
            // TODO 本来は、200が返却されるべきだが、現在は環境によって動作が異なるため、401でないことのみチェックしている
            res = ResourceUtils.optionsWithAnyAuthSchema(cellName, token, srcCol, -1);
            assertThat(res.getStatusCode()).isNotEqualTo(HttpStatus.SC_UNAUTHORIZED);
            // サービスソースコレクションOPTIONS(Basic認証-失敗)
            // TODO 本来は、401が返却されるべき
            // res = ResourceUtils.optionsWithAnyAuthSchema(cellName, invalidToken, srcCol,
            // HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME,
                    srcFile, -1, boxName);
            DavResourceUtils.deleteCollection(cellName, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Basic認証ースキーマなしBox配下のサービスソースコレクション配下のWebDavファイルの操作.
     */
    private void svcSourceValidate() {
        final String srcFile = colName + "/__src/" + fileName;
        try {
            // サービスコレクション作成(Basic認証-成功)
            DavResourceUtils.createServiceCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);
            // ファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge", boxName,
                    srcFile, HttpStatus.SC_CREATED);
            // ファイル作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavFile(cellName, invalidToken, "box/dav-put-anyAuthSchema.txt",
                    "hoge", boxName, srcFile, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt",
                    boxName, srcFile, HttpStatus.SC_OK);
            // ファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt", boxName,
                    srcFile, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token,
                    cellName, boxName + "/" + srcFile, 1, HttpStatus.SC_MULTI_STATUS);
            // ファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + srcFile, 1, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルをPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", srcFile)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // ファイルをPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", srcFile)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_METHOD_NOT_ALLOWED,
                    boxName, srcFile, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            // ファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, srcFile, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルOPTIONS(Basic認証-成功)
            // TODO 本来は、200が返却されるべきだが、現在は環境によって動作が異なるため、401でないことのみチェックしている
            res = ResourceUtils.optionsWithAnyAuthSchema(cellName, token, srcFile, -1);
            assertThat(res.getStatusCode()).isNotEqualTo(HttpStatus.SC_UNAUTHORIZED);
            // ファイルOPTIONS(Basic認証-失敗)
            // TODO 本来は、401が返却されるべき
            // res = ResourceUtils.optionsWithAnyAuthSchema(cellName, invalidToken, srcFile,
            // HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    srcFile, HttpStatus.SC_NO_CONTENT, boxName);
            // ファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    srcFile, HttpStatus.SC_UNAUTHORIZED, boxName);
            checkAuthenticateHeaderForSchemalessBoxLevel(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME,
                    srcFile, -1, boxName);
            DavResourceUtils.deleteCollection(cellName, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    private void checkAuthenticateHeaderForSchemalessBoxLevel(TResponse res, String expectedCellName) {
        // WWW-Authenticateヘッダチェック
        String bearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        String basic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(2, headers.size());
        assertThat(headers).contains(bearer);
        assertThat(headers).contains(basic);
    }

}
