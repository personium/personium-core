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
package io.personium.test.jersey.box.dav.file;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

import static org.fest.assertions.Assertions.assertThat;

/**
 * サービスソースに対する親子関係のアクセス制御のテスト.<br />
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ServiceSourceAccessControlTest extends JerseyTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "CollectionAclTestCell";
    private static final String BOX_NAME = "box1";
    private static final String PARENT_COL_NAME = "parentCollection";
    private static final String TARGET_SOURCE_FILE_PATH = "__src/targetFile";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT = "account";
    private static final String ROLE = "role";

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "io.personium.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public ServiceSourceAccessControlTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * すべてのテストで最初に実行する処理.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    @Before
    public void before() throws JAXBException {
        createTestCollection();
    }

    /**
     * すべてのテストで最後に実行する処理.
     */
    @After
    public void after() {
        CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
    }

    /**
     * 親コレクションに権限がないアカウントでファイルのDELETEを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでファイルのDELETEを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.deleteWebDavFile(CELL_NAME, token, BOX_NAME, path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwrite権限があるアカウントでファイルのDELETEを行い204となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限があるアカウントでファイルのDELETEを行い204となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.deleteWebDavFile(CELL_NAME, token, BOX_NAME, path);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 親コレクションに権限がないアカウントでファイルの作成を行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでファイルの作成を行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH + "new");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.createWebDavFile(token, CELL_NAME, BOX_NAME + "/" + path, "testFileBody",
                MediaType.TEXT_PLAIN, HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwrite権限があるアカウントでファイルの作成を行い201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限があるアカウントでファイルの作成を行い201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH + "new");

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.createWebDavFile(token, CELL_NAME, BOX_NAME + "/" + path, "testFileBody",
                MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
    }

    /**
     * 親コレクションに権限がないアカウントでファイルの更新を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでファイルの更新を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.createWebDavFile(token, CELL_NAME, BOX_NAME + "/" + path, "testFileBody",
                MediaType.TEXT_PLAIN, HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwrite権限があるアカウントでファイルの更新ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限があるアカウントでファイルの更新ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.createWebDavFile(token, CELL_NAME, BOX_NAME + "/" + path, "testFileBody",
                MediaType.TEXT_PLAIN, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 親コレクションに権限がないアカウントでファイルの取得を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでファイルの取得を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.getWebDav(CELL_NAME, token, BOX_NAME, path, HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにread権限があるアカウントでファイルの取得ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにread権限があるアカウントでファイルの取得ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.getWebDav(CELL_NAME, token, BOX_NAME, path, HttpStatus.SC_OK);
    }

    /**
     * 移動元の親コレクションに権限がないアカウントでファイルの移動を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親コレクションに権限がないアカウントでファイルの移動を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, "destFile.js");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        setAcl(dstColName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 移動元の親コレクションにwrite権限があるアカウントでファイルの移動ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親コレクションにwrite権限があるアカウントでファイルの移動ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, "destFile.js");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        setAcl(dstColName, ROLE, "write");

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination, HttpStatus.SC_CREATED);
    }

    /**
     * 移動元の親コレクションに権限がないアカウントでファイルの上書き移動を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親コレクションに権限がないアカウントでファイルの上書き移動を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);
        setAcl(dstColName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 移動元の親コレクションにwrite権限があるアカウントでファイルの上書き移動ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親コレクションにwrite権限があるアカウントでファイルの上書き移動ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);
        setAcl(dstColName, ROLE, "write");

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 移動先の親コレクションに権限がなし_かつ_上書き対象ファイルに権限がないアカウントでファイルの上書き移動を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションに権限がなし_かつ_上書き対象ファイルに権限がないアカウントでファイルの上書き移動を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 移動先の親コレクションに権限がない_かつ_上書き対象ファイルにwrite権限があるアカウントでファイルの上書き移動を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションに権限がない_かつ_上書き対象ファイルにwrite権限があるアカウントでファイルの上書き移動を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);

        // ACL設定
        setAcl(dstColName + "/" + dstFileName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 移動先の親コレクションにwrite権限がある_かつ_上書き対象ファイルに権限がないアカウントでファイルの上書き移動ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションにwrite権限がある_かつ_上書き対象ファイルに権限がないアカウントでファイルの上書き移動ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);

        // ACL設定
        setAcl(dstColName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 移動先の親コレクションにwrite権限がある_かつ_上書き対象ファイルにwrite権限があるアカウントでファイルの上書き移動ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションにwrite権限がある_かつ_上書き対象ファイルにwrite権限があるアカウントでファイルの上書き移動ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String dstFileName = "destinationFile.txt";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, dstFileName);

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + dstColName + "/" + dstFileName, "testFileBody", MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);

        // ACL設定
        setAcl(dstColName, ROLE, "write");
        setAcl(dstColName + "/" + dstFileName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 移動先の親コレクションに権限がないアカウントでファイルの移動を行い403になること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションに権限がないアカウントでファイルの移動を行い403になること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, "destFile.js");

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 移動先の親コレクションにwrite権限があるアカウントでファイルの移動ができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親コレクションにwrite権限があるアカウントでファイルの移動ができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String dstColName = "dstColName";
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName, "destFile.js");

        // 移動元ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // 移動先コレクション作成
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                dstColName);

        // ACL設定
        setAcl(dstColName, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.moveWebDav(token, CELL_NAME, BOX_NAME + "/" + path, destination,
                "*", "T", "infinity", HttpStatus.SC_CREATED);
    }

    /**
     * 親コレクションに権限がないアカウントでファイルのPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでファイルのPROPFINDを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにreadproperties権限があるアカウントでファイルのPROPFINDを行いACL以外の情報が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限があるアカウントでファイルのPROPFINDを行いACL以外の情報が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertNotContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadacl権限があるアカウントでPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限があるアカウントでPROPFINDを行い403エラーとなること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がないアカウントでPROPPATCHを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでPROPPATCHを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.setProppatch(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME, path);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_SOURCE_FILE_PATH);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.setProppatch(token, HttpStatus.SC_MULTI_STATUS, CELL_NAME, BOX_NAME, path);
    }

    /**
     * Accountの自分セルローカルトークンを取得する.
     * @param account Account名
     * @return トークン
     */
    private String getToken(String account) {
        return ResourceUtils.getMyCellLocalToken(CELL_NAME, account, PASSWORD);
    }

    /**
     * テスト用のコレクションを作成し、テストに必要なAccountやACLの設定を作成する.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    private void createTestCollection() throws JAXBException {

        // Collection作成
        CellUtils.create(CELL_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL_NAME, BOX_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                BOX_NAME, PARENT_COL_NAME);
        DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME,
                BOX_NAME + "/" + PARENT_COL_NAME + "/" + TARGET_SOURCE_FILE_PATH, "testFileBody",
                MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);

        // Role作成
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE, HttpStatus.SC_CREATED);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT, ROLE, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたコレクションに対しPrivilegeを設定.
     * @param collection コレクション名
     * @param role ロール名
     * @param privileges 権限(カンマ区切りで複数指定可能)
     * @throws JAXBException ACLのパースに失敗
     */
    private void setAcl(String collection, String role, String... privileges) throws JAXBException {
        setAcl(MASTER_TOKEN, HttpStatus.SC_OK, collection, role, privileges);
    }

    /**
     * 指定されたコレクションに対しPrivilegeを設定.
     * @param token 認証トークン
     * @param code 期待するレスポンスコード
     * @param collection コレクション名
     * @param role ロール名
     * @param privileges 権限(カンマ区切りで複数指定可能)
     * @return レスポンス
     * @throws JAXBException ACLのパースに失敗
     */
    private TResponse setAcl(String token, int code, String collection, String role, String... privileges)
            throws JAXBException {
        Acl acl = new Acl();
        for (String privilege : privileges) {
            acl.getAce().add(DavResourceUtils.createAce(false, role, privilege));
        }
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.DEFAULT_BOX_NAME));
        return DavResourceUtils.setAcl(token, CELL_NAME, BOX_NAME, collection, acl, code);
    }

}
