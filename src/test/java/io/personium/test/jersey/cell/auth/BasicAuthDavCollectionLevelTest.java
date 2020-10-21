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
package io.personium.test.jersey.cell.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.model.Box;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.bar.BarInstallTestUtils;
import io.personium.test.setup.Setup;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Basic認証のBoxレベルのリソースに対するテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthDavCollectionLevelTest extends PersoniumTest {

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
        super(new PersoniumCoreApplication());
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
        AuthTestCommon.waitForIntervalLock();
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
            AuthTestCommon.waitForIntervalLock();

            // スキーマなしのBox直下のファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt", boxName, fileName,
                    HttpStatus.SC_OK);
            // スキーマなしのBox直下のファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt", boxName,
                    fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // スキーマなしのBox直下のファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName, boxName + "/"
                    + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // スキーマなしのBox直下のファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

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
            AuthTestCommon.waitForIntervalLock();

            // スキーマなしのBox直下のファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, fileName, "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            // スキーマなしのBox直下のファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, fileName, "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

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
            AuthTestCommon.waitForIntervalLock();

            // スキーマなしのBox直下のファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    fileName, HttpStatus.SC_NO_CONTENT, boxName);
            // スキーマなしのBox直下のファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    fileName, HttpStatus.SC_UNAUTHORIZED, boxName);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
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
                    Box.MAIN_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME, "<D:read/>",
                    "<D:write/>", "");

            // メインボックス直下にファイル作成(Basic認証-成功)
            DavResourceUtils.createWebDavFile(cellName, token, "box/dav-put-anyAuthSchema.txt", "hoge",
                    Box.MAIN_BOX_NAME, fileName, HttpStatus.SC_CREATED);
            // メインボックス直下にファイル作成(Basic認証-失敗)
            TResponse res = DavResourceUtils.createWebDavFile(cellName, invalidToken,
                    "box/dav-put-anyAuthSchema.txt", "hoge", Box.MAIN_BOX_NAME, fileName,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックスにACL(read-acl + write-acl)を設定
            DavResourceUtils.setACLwithBox(cellName, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Box.MAIN_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role7", "role7", Box.MAIN_BOX_NAME, "<D:read-acl/>",
                    "<D:write-acl/>", "");

            // メインボックス直下のファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    Box.MAIN_BOX_NAME, fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Box.MAIN_BOX_NAME, "<D:read/>", "<D:write/>", "");
            // メインボックス直下のファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    Box.MAIN_BOX_NAME, fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Box.MAIN_BOX_NAME, "<D:read/>", "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックスにACL(read + write)を設定
            DavResourceUtils.setACLwithBox(cellName, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Box.MAIN_BOX_NAME, "",
                    "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME, "<D:read/>",
                    "<D:write/>", "");

            // メインボックス直下のファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt", Box.MAIN_BOX_NAME,
                    fileName, HttpStatus.SC_OK);
            // メインボックス直下のファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt",
                    Box.MAIN_BOX_NAME, fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックス直下のファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName,
                    Box.MAIN_BOX_NAME + "/" + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // メインボックス直下のファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    Box.MAIN_BOX_NAME + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックス直下のファイルをPROPPATCH(Basic認証-成功)
            Http.request("box/proppatch.txt").with("cell", cellName).with("box", Box.MAIN_BOX_NAME)
                    .with("path", fileName)
                    .with("token", token)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_MULTI_STATUS);
            // メインボックス直下のファイルをPROPPATCH(Basic認証-失敗)
            res = Http.request("box/proppatch.txt").with("cell", cellName).with("box", Box.MAIN_BOX_NAME)
                    .with("path", fileName)
                    .with("token", invalidToken)
                    .with("author1", "Author1 update")
                    .with("hoge", "fuga")
                    .returns().statusCode(HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックス直下のファイルを変名(Basic認証-成功)
            String dstFileName = "dstFileName";
            String destinationPath = UrlUtils.box(cellName, Box.MAIN_BOX_NAME, dstFileName);
            DavResourceUtils.moveWebDavWithAnyAuthSchema(token, cellName, Box.MAIN_BOX_NAME + "/" + fileName,
                    destinationPath, HttpStatus.SC_CREATED);
            String originalPath = UrlUtils.box(cellName, Box.MAIN_BOX_NAME, fileName);
            DavResourceUtils.moveWebDav(AbstractCase.MASTER_TOKEN_NAME, cellName, Box.MAIN_BOX_NAME + "/"
                    + dstFileName, originalPath, -1);
            // メインボックス直下のファイルをMOVE(Basic認証-失敗)
            DavResourceUtils.moveWebDavWithAnyAuthSchema(invalidToken, cellName, Box.MAIN_BOX_NAME + "/"
                    + fileName, destinationPath, HttpStatus.SC_UNAUTHORIZED);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // メインボックス直下のファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    fileName, HttpStatus.SC_NO_CONTENT, Box.MAIN_BOX_NAME);
            // メインボックス直下のファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    fileName, HttpStatus.SC_UNAUTHORIZED, Box.MAIN_BOX_NAME);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
        } finally {
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME, fileName,
                    -1, Box.MAIN_BOX_NAME);
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
            AuthTestCommon.waitForIntervalLock();

            // コレクションPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token, cellName,
                    boxName + "/" + thisMethodColName, 1, HttpStatus.SC_MULTI_STATUS);
            // コレクションPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + thisMethodColName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

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
            AuthTestCommon.waitForIntervalLock();

            // コレクションACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, thisMethodColName, "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            // コレクションACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, thisMethodColName, "box/acl-2role-setting.txt", "role4", "role4", Box.MAIN_BOX_NAME,
                    "<D:read/>", "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // コレクションOPTIONS(Basic認証-成功)
            ResourceUtils.optionsWithAnyAuthSchema(cellName, token, thisMethodColName, HttpStatus.SC_OK);
            // コレクションOPTIONS(Basic認証-失敗)
            res = ResourceUtils.optionsWithAnyAuthSchema(cellName, invalidToken, thisMethodColName,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // コレクション削除(Basic認証-成功)
            DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, thisMethodColName, token,
                    HttpStatus.SC_NO_CONTENT);
            // コレクション削除(Basic認証-失敗)
            res = DavResourceUtils.deleteCollectionWithAnyAuthSchema(cellName, boxName, thisMethodColName,
                    invalidToken,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
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
            AuthTestCommon.waitForIntervalLock();

            // ファイルを取得(Basic認証-成功)
            DavResourceUtils.getWebDavFile(cellName, token, "box/dav-get-anyAuthSchema.txt",
                    boxName, colName + "/" + fileName, HttpStatus.SC_OK);
            // ファイルを取得(Basic認証-失敗)
            res = DavResourceUtils.getWebDavFile(cellName, invalidToken, "box/dav-get-anyAuthSchema.txt", boxName,
                    colName + "/" + fileName, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

            // ファイルをPROPFIND(Basic認証-成功)
            DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", token,
                    cellName, boxName + "/" + colName + "/" + fileName, 1, HttpStatus.SC_MULTI_STATUS);
            // ファイルをPROPFIND(Basic認証-失敗)
            res = DavResourceUtils.propfind("box/propfind-box-allprop-anyAuthSchema.txt", invalidToken, cellName,
                    boxName + "/" + colName + "/" + fileName, 1, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

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
            AuthTestCommon.waitForIntervalLock();

            // ファイルにACL設定(Basic認証-成功)
            DavResourceUtils.setACLwithBox(cellName, tokenForACLWrite, HttpStatus.SC_OK,
                    boxName, colName + "/" + fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Box.MAIN_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            // ファイルにACL設定(Basic認証-失敗)
            res = DavResourceUtils.setACLwithBox(cellName, invalidToken, HttpStatus.SC_UNAUTHORIZED,
                    boxName, colName + "/" + fileName, "box/acl-2role-setting.txt", "role4", "role4",
                    Box.MAIN_BOX_NAME,
                    "<D:read/>",
                    "<D:write/>", "");
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();

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
            AuthTestCommon.waitForIntervalLock();

            // ファイルを削除(Basic認証-成功)
            DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, token,
                    colName + "/" + fileName, HttpStatus.SC_NO_CONTENT, boxName);
            // ファイルを削除(Basic認証-失敗)
            res = DavResourceUtils.deleteWebDavFile("box/dav-delete-anyAuthSchema.txt", cellName, invalidToken,
                    colName + "/" + fileName, HttpStatus.SC_UNAUTHORIZED, boxName);
            AuthTestCommon.checkAuthenticateHeader(res, cellName);
            // 認証失敗のアカウントロックが解除されるのを待ち合わせる
            AuthTestCommon.waitForIntervalLock();
        } finally {
            // MOVEで使用したcollectionの削除
            DavResourceUtils.deleteCollection(cellName, boxName, dstColName, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", cellName, AbstractCase.MASTER_TOKEN_NAME,
                    colName + "/" + fileName, -1, boxName);
        }
    }

}
