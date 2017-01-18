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
import com.fujitsu.dc.test.jersey.bar.BarInstallTestUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * Basic認証のBoxレベルのリソースに対するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthDavCollectionLevelTest extends JerseyTest {

    private String cellName = Setup.TEST_CELL_BASIC;
    private String boxName = Setup.TEST_BOX1;
    private String colName = "setdavcol";
    private String fileName = "basicAuthResourceTestFile";

    private String userName = "account4";
    private String password = "password4";
    private String invalidPassword = "invalidPassword";
    private String token = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, password).getBytes());
    private String tokenForACLWrite = "Basic " + Base64.encodeBase64String(("account7:password7").getBytes());
    private String invalidToken = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, invalidPassword).getBytes());

    /**
     * コンストラクタ.
     */
    public BasicAuthDavCollectionLevelTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Basic認証ースキーマ情報を持たないBoxレベルAPIの操作.
     */
    @Test
    public final void Basic認証ースキーマ情報を持たないBoxレベルAPIの操作() {
        // Boxインストール状況取得API
        getBarInstallProgress();

        // スキーマなしBox直下のファイルの操作
        fileInShemalessBox();

        // メインボックス直下のファイルの操作
        fileInMainBox();
    }

    /**
     * Basic認証ースキーマ情報を持たないBox配下のDavコレクションレベルAPIの操作.
     */
    @Test
    public final void Basic認証ースキーマ情報を持たないBox配下ーDavコレクションレベルAPIの操作() {
        // スキーマなしBox配下のDavコレクション
        davCollectionInSchemalessBox();

        // スキーマなしBox配下のDavコレクション配下のWebDavファイル
        fileInSchemalessBoxCollection();
    }

    /**
     * Basic認証ーBoxインストールの状況取得API.
     */
    private void getBarInstallProgress() {
        // Boxインストールの状況取得(Basic認証-成功)
        BarInstallTestUtils.getProgress(cellName, boxName, token, HttpStatus.SC_OK);
        // Boxインストールの状況取得(Basic認証-失敗)
        TResponse res = BarInstallTestUtils.getProgress(cellName, boxName, invalidToken, HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, cellName);
        // 認証失敗のアカウントロックが解除されるのを待ち合わせる
        AuthTestCommon.waitForAccountLock();
    }

    /**
     * Basic認証ースキーマなしBox直下のファイルの操作.
     */
    private void fileInShemalessBox() {
        try {
            // スキーマなしのBox直下にファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge", boxName,
                    fileName, HttpStatus.SC_CREATED);
            // スキーマなしのBox直下にファイル作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavFile(cellName, invalidToken, "box/dav-put-anyAuthSchema.txt",
                    "hoge", boxName, fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt", boxName, fileName,
                    HttpStatus.SC_OK);
            // スキーマなしのBox直下のファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt", boxName,
                    fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName, boxName + "/"
                    + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // スキーマなしのBox直下のファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルをPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName).with("path", fileName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // スキーマなしのBox直下のファイルをPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName).with("path", fileName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, fileName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            // スキーマなしのBox直下のファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, fileName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルを変名(Basic認証-成功)
            String dstFileName = "dstFileName";
            String destinationPath = UrlUtils.box(cellName, boxName, dstFileName);
            DavResourceUtils.moveWebDavWithAnyAuthSchema(token, cellName, boxName + "/" + fileName,
                    destinationPath,
                    HttpStatus.SC_CREATED);
            String originalPath = UrlUtils.box(cellName, boxName, fileName);
            DavResourceUtils.moveWebDav(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName + "/" + dstFileName,
                    originalPath, -1);
            // スキーマなしのBox直下のファイルをMOVE(Basic認証-失敗)
            DavResourceUtils.moveWebDavWithAnyAuthSchema(invalidToken, cellName, boxName + "/" + fileName,
                    destinationPath,
                    HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // スキーマなしのBox直下のファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    fileName, HttpStatus.SC_NO_CONTENT, boxName);
            // スキーマなしのBox直下のファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    fileName, HttpStatus.SC_UNAUTHORIZED, boxName);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME, fileName,
                    -1, boxName);
        }
    }

    /**
     * Basic認証ーメインボックス直下のファイルの操作.
     */
    private void fileInMainBox() {
        // WebDavファイル(メインボックス直下)
        try {
            // メインボックスにACL(read + write)を設定
            DavResourceUtils.setACLwithBox(cellName, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Role.DEFAULT_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME, "<D:read/>",
                    "<D:write/>", "");

            // メインボックス直下にファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge",
                    Role.DEFAULT_BOX_NAME, fileName, HttpStatus.SC_CREATED);
            // メインボックス直下にファイル作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavFile(cellName, invalidToken,
                    "box/dav-put-anyAuthSchema.txt", "hoge", Role.DEFAULT_BOX_NAME, fileName,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックスにACL(read-acl + write-acl)を設定
            DavResourceUtils.setACLwithBox(cellName, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Role.DEFAULT_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role7", "role7", Role.DEFAULT_BOX_NAME, "<D:read-acl/>",
                    "<D:write-acl/>", "");

            // メインボックス直下のファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    Role.DEFAULT_BOX_NAME, fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME, "<D:read/>", "<D:write/>", "");
            // メインボックス直下のファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    Role.DEFAULT_BOX_NAME, fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME, "<D:read/>", "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックスにACL(read + write)を設定
            DavResourceUtils.setACLwithBox(cellName, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Role.DEFAULT_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME, "<D:read/>",
                    "<D:write/>", "");

            // メインボックス直下のファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt", Role.DEFAULT_BOX_NAME,
                    fileName, HttpStatus.SC_OK);
            // メインボックス直下のファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt",
                    Role.DEFAULT_BOX_NAME, fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックス直下のファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName,
                    Role.DEFAULT_BOX_NAME + "/" + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // メインボックス直下のファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    Role.DEFAULT_BOX_NAME + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックス直下のファイルをPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", Role.DEFAULT_BOX_NAME)
                    .with("path", fileName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // メインボックス直下のファイルをPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", Role.DEFAULT_BOX_NAME)
                    .with("path", fileName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックス直下のファイルを変名(Basic認証-成功)
            String dstFileName = "dstFileName";
            String destinationPath = UrlUtils.box(cellName, Role.DEFAULT_BOX_NAME, dstFileName);
            DavResourceUtils.moveWebDavWithAnyAuthSchema(token, cellName, Role.DEFAULT_BOX_NAME + "/" + fileName,
                    destinationPath, HttpStatus.SC_CREATED);
            String originalPath = UrlUtils.box(cellName, Role.DEFAULT_BOX_NAME, fileName);
            DavResourceUtils.moveWebDav(AbstractCase.MASTER_TOKEN_NAME, cellName, Role.DEFAULT_BOX_NAME + "/"
                    + dstFileName, originalPath, -1);
            // メインボックス直下のファイルをMOVE(Basic認証-失敗)
            DavResourceUtils.moveWebDavWithAnyAuthSchema(invalidToken, cellName, Role.DEFAULT_BOX_NAME + "/"
                    + fileName, destinationPath, HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // メインボックス直下のファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    fileName, HttpStatus.SC_NO_CONTENT, Role.DEFAULT_BOX_NAME);
            // メインボックス直下のファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    fileName, HttpStatus.SC_UNAUTHORIZED, Role.DEFAULT_BOX_NAME);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME, fileName,
                    -1, Role.DEFAULT_BOX_NAME);
        }
    }

    /**
     * Basic認証ースキーマなしBox配下のWebDavコレクションの操作.
     */
    private void davCollectionInSchemalessBox() {
        String thisMethodColName = "basicAuthResourceDavCollection";
        // WebDavコレクションの操作
        try {
            // WebDavコレクション
            // コレクション作成(Basic認証-成功)
            DavResourceUtils.createWebDavCollectionWithAnyAuthSchema(token, HttpStatus.SC_CREATED, cellName, boxName,
                    thisMethodColName);
            // コレクション作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavCollectionWithAnyAuthSchema(invalidToken,
                    HttpStatus.SC_UNAUTHORIZED, cellName, boxName, thisMethodColName);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName,
                    boxName + "/" + thisMethodColName, 1, HttpStatus.SC_MULTI_STATUS);
            // コレクションPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + thisMethodColName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", thisMethodColName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // コレクションPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", thisMethodColName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, thisMethodColName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            // コレクションACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, thisMethodColName, "box/acl-2role-setting.txt", "role4", "role4", Role.DEFAULT_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクションOPTIONS(Basic認証-成功)
            ResourceUtils.optionsWithAnyAuthSchema(cellName, token, thisMethodColName, HttpStatus.SC_OK);
            // コレクションOPTIONS(Basic認証-失敗)
            res = ResourceUtils.optionsWithAnyAuthSchema(cellName, invalidToken, thisMethodColName,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // コレクション削除(Basic認証-成功)
            DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, thisMethodColName, token,
                    HttpStatus.SC_NO_CONTENT);
            // コレクション削除(Basic認証-失敗)
            res = DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, thisMethodColName,
                    invalidToken,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteCollection(cellName, boxName, thisMethodColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Basic認証ースキーマなしBox配下のWebDavコレクション配下のWebDavファイルの操作.
     */
    private void fileInSchemalessBoxCollection() {
        String dstColName = "dstColName";
        try {
            // ファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge", boxName,
                    colName + "/" + fileName, HttpStatus.SC_CREATED);
            // ファイル作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavFile(cellName, invalidToken, "box/dav-put-anyAuthSchema.txt",
                    "hoge", boxName, colName + "/" + fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt",
                    boxName, colName + "/" + fileName, HttpStatus.SC_OK);
            // ファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt", boxName,
                    colName + "/" + fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token,
                    cellName, boxName + "/" + colName + "/" + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // ファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + colName + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルをPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", colName + "/" + fileName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // ファイルをPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", boxName)
                    .with("path", colName + "/" + fileName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, colName + "/" + fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            // ファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, colName + "/" + fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Role.DEFAULT_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルをMOVE(Basic認証-成功)
            String destinationPath = UrlUtils.box(cellName, boxName, dstColName, fileName);
            DavResourceUtils.createWebDavCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName,
                    boxName, dstColName);
            DavResourceUtils.moveWebDavWithAnyAuthSchema(token, cellName, boxName + "/" + colName + "/" + fileName,
                    destinationPath, HttpStatus.SC_CREATED);
            String originalPath = UrlUtils.box(cellName, boxName, colName, fileName);
            DavResourceUtils.moveWebDav(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName + "/" + dstColName + "/"
                    + fileName, originalPath, -1);
            // ファイルをMOVE(Basic認証-失敗)
            DavResourceUtils.moveWebDavWithAnyAuthSchema(invalidToken, cellName, boxName + "/" + colName + "/"
                    + fileName, destinationPath, HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();

            // ファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    colName + "/" + fileName, HttpStatus.SC_NO_CONTENT, boxName);
            // ファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    colName + "/" + fileName, HttpStatus.SC_UNAUTHORIZED, boxName);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForAccountLock();
        } finally {
            // MOVEで使用したcollectionの削除
            DavResourceUtils.deleteCollection(cellName, boxName, dstColName, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME,
                    colName + "/" + fileName, -1, boxName);
        }
    }

}
