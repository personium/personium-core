/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.test.jersey.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * ユーザデータ向けのbarファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BarInstallUserDataTest extends PersoniumTest {

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";
    private static final String BAR_FILE_USERDATA_CREATE = "/V1_1_2_bar_userdata_create.bar";
    private static final String BAR_FILE_USERDATA_PARSE_ERROR = "/V1_1_2_bar_userdata_parse_error.bar";
    private static final String BAR_FILE_USERDATA_VALIDATE_ERROR = "/V1_1_2_bar_userdata_validate_error.bar";
    private static final String BAR_FILE_USERDATA_CONFLICT_ERROR = "/V1_1_2_bar_userdata_conflict_error.bar";
    private static final String BAR_FILE_USERDATA_NOTEXSIST_90_DATA = "/V1_1_2_bar_userdata_not_exist_90_data.bar";
    private static final String BAR_FILE_USERDATA_NOTEXSIST_ENTITYTYPE =
            "/V1_1_2_bar_userdata_not_exist_entitytype.bar";
    private static final String BAR_FILE_USERDATA_DIFFERENT_FILE_NAME = "/V1_1_2_bar_userdata_different_file_name.bar";

    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BarInstallUserDataTest() {
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
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
        BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, -1);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, -1);
    }

    /**
     * barファイルインストールでユーザデータを登録が正常終了すること.
     * @throws Exception レスポンス取得失敗
     */
    @Test
    public final void barファイルインストールでユーザデータを登録が正常終了すること() throws Exception {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_CREATE);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータの内容チェック
            checkUserData(Setup.TEST_CELL1, INSTALL_TARGET, odataColName, "entity1", "barInstallTest");
            checkUserDataForDouble(Setup.TEST_CELL1, INSTALL_TARGET, odataColName, "entity2");
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);
            for (int i = 1; i < 12; i++) {
                final String id = String.format("double2-%02d", i);
                resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", id);
                ODataCommon.deleteOdataResource(resourceUrl);
            }

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "cprop2-1", "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "cprop2-2", "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "prop2-1", "entity2");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "prop2-2", "entity2");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "prop2-3", "entity2");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールでWebDAVコレクション配下のODataコレクションにユーザデータが登録できること.
     */
    @Test
    public final void barファイルインストールでWebDAVコレクション配下のODataコレクションにユーザデータが登録できること() {
        String testBarFileName = "/V1_1_2_bar_userdata_create_under_webdavCol.bar";

        final String reqCell = Setup.TEST_CELL1;
        final String reqBox = INSTALL_TARGET;
        final String odataColName = "davcol/odatacol";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + testBarFileName);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqBox, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqBox;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータの登録確認
            res = UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqBox, odataColName, "entity1",
                    "barInstallTest", HttpStatus.SC_OK);
            String locationHeader = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest");
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", "barInstallTest");
            additional.put("name", "barInstall");
            additional.put("property1", "property");
            Map<String, String> compProp = new HashMap<String, String>();
            compProp.put("compProp1", "compProp1");
            additional.put("property2", compProp);
            ODataCommon.checkResponseBody(res.bodyAsJson(), locationHeader, "UserData.entity1", additional);

            res = UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqBox, odataColName, "entity1",
                    "barInstallTest2", HttpStatus.SC_OK);
            locationHeader = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest2");
            additional = new HashMap<String, Object>();
            additional.put("__id", "barInstallTest2");
            additional.put("name", "barInstall2");
            additional.put("property1", "property");
            compProp = new HashMap<String, String>();
            compProp.put("compProp1", "compProp1");
            additional.put("property2", compProp);
            ODataCommon.checkResponseBody(res.bodyAsJson(), locationHeader, "UserData.entity1", additional);

        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqBox, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, "davcol", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールでユーザデータLinkの登録が正常終了すること.
     */
    @Test
    public final void barファイルインストールでユーザデータLinkの登録が正常終了すること() {
        final String barFilename = "/V1_3_12_bar_userdata_create_link.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータのリンク削除
            ResourceUtils.deleteUserDataLinks("barInstallTest", "barInstallTest2", "entity2",
                    reqCell, INSTALL_TARGET, odataColName, "entity1", HttpStatus.SC_NO_CONTENT);
        } finally {

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 同一名のAssociationEndでユーザデータLinkの登録が正常終了すること.
     */
    @Test
    public final void 同一名のAssociationEndでユーザデータLinkの登録が正常終了すること() {
        final String barFilename = "/V1_3_12_bar_same_associationend_name.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータのリンク削除
            ResourceUtils.deleteUserDataLinks("barInstallTest", "barInstallTest2", "entity2",
                    reqCell, INSTALL_TARGET, odataColName, "entity1", HttpStatus.SC_NO_CONTENT);
        } finally {

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='associationEnd',_EntityType.Name='entity1'";
            String navKey = "Name='associationEnd',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "associationEnd", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "associationEnd", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ロール名の先頭がコロンの場合にユーザデータLinkの登録が異常終了すること.
     */
    @Test
    public final void ロール名の先頭がコロンの場合にユーザデータLinkの登録が異常終了すること() {
        final String barFilename = "/V1_3_12_bar_role_name_top_colon.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='associationEnd',_EntityType.Name='entity1'";
            String navKey = "Name='associationEnd',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "associationEnd", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "associationEnd", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ロール名の末尾がコロンの場合にユーザデータLinkの登録が異常終了すること.
     */
    @Test
    public final void ロール名の末尾がコロンの場合にユーザデータLinkの登録が異常終了すること() {
        final String barFilename = "/V1_3_12_bar_role_name_last_colon.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='associationEnd',_EntityType.Name='entity1'";
            String navKey = "Name='associationEnd',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "associationEnd", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "associationEnd", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ロール名にコロンが含まれていない場合にユーザデータLinkの登録が異常終了すること.
     */
    @Test
    public final void ロール名にコロンが含まれていない場合にユーザデータLinkの登録が異常終了すること() {
        final String barFilename = "/V1_1_2_bar_userdata_create_link.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

        } finally {

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで2つのコレクションのユーザデータLink登録が正常終了すること.
     */
    @Test
    public final void barファイルインストールで2つのコレクションのユーザデータLink登録が正常終了すること() {
        final String barFilename = "/V1_3_12_bar_2col_userdata_link.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName1 = "col1";
        final String odataColName2 = "col2";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqPath, odataColName1, "entity1",
                    "barInstallTest", HttpStatus.SC_OK);
            UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqPath, odataColName2, "entity2",
                    "barInstallTest2", HttpStatus.SC_OK);
        } finally {
            // ユーザデータLinkの削除
            ResourceUtils.deleteUserDataLinks("barInstallTest", "barInstallTest2", "entity2",
                    reqCell, INSTALL_TARGET, odataColName1, "entity1", -1);

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName1, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName1, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName1, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName1, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName1, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName1, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName1, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName1, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName1, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName1, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName1, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, odataColName1, AbstractCase.MASTER_TOKEN_NAME,
                    -1);

            // ユーザデータLinkの削除
            ResourceUtils.deleteUserDataLinks("barInstallTest", "barInstallTest2", "entity2",
                    reqCell, INSTALL_TARGET, odataColName2, "entity1", -1);

            // ユーザデータの削除
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName2, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName2, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName2, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName2, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName2, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName2, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName2, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName2, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName2, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName2, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName2, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, odataColName2, AbstractCase.MASTER_TOKEN_NAME,
                    -1);
        }
    }

    /**
     * barファイルインストールで10_odatarelations_jsonのFromTypeに存在しないEnyityTypeを指定した場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで10_odatarelations_jsonのFromTypeに存在しないEnyityTypeを指定した場合に異常終了すること() {
        final String barFileName = RESOURCE_PATH + "/V1_1_2_bar_userdata_link_notexsist_entitytype.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(barFileName);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで10_odatarelations_jsonのToIdに存在しないEnyitySetを指定した場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで10_odatarelations_jsonのToIdに存在しないEnyitySetを指定した場合に異常終了すること() {
        final String barFileName = RESOURCE_PATH + "/V1_1_2_bar_userdata_link_notexsist_entityset.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(barFileName);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで10_odatarelations_jsonのLink情報が重複した場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで10_odatarelations_jsonのLink情報が重複した場合に異常終了すること() {
        final String barFileName = RESOURCE_PATH + "/V1_1_2_bar_userdata_link_conflict.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(barFileName);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ユーザデータLinkの削除
            ResourceUtils.deleteUserDataLinks("barInstallTest", "barInstallTest2", "entity2",
                    reqCell, INSTALL_TARGET, odataColName, "entity1", -1);

            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity2", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで10_odatarelations_jsonのファイル名が不正の場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで10_odatarelations_jsonのファイル名が不正の場合に異常終了すること() {
        final String barFileName = RESOURCE_PATH + "/V1_1_2_bar_userdata_link_file_invalid.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(barFileName);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // AssociationEnd
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                    INSTALL_TARGET, "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                    INSTALL_TARGET, "entity2-entity1", -1);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールでno_jsonに不正な値が存在する場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールでno_jsonに不正な値が存在する場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_VALIDATE_ERROR);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ComplexTypePropertyの削除
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールでno_jsonがjson形式でない場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールでno_jsonがjson形式でない場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_PARSE_ERROR);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ComplexTypePropertyの削除
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで同じIDを登録する場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで同じIDを登録する場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_CONFLICT_ERROR);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで90_data配下にno_jsonが存在しない場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで90_data配下にno_jsonが存在しない場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_NOTEXSIST_90_DATA);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ComplexTypePropertyの削除
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールで存在しないEntityTypeディレクトリ名を指定した場合に異常終了すること.
     */
    @Test
    public final void barファイルインストールで存在しないEntityTypeディレクトリ名を指定した場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_NOTEXSIST_ENTITYTYPE);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ComplexTypePropertyの削除
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * barファイルインストールでno_jsonと異なる名前を指定した場合に異常終了すること().
     */
    @Test
    public final void barファイルインストールでno_jsonと異なる名前を指定した場合に異常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_USERDATA_DIFFERENT_FILE_NAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ComplexTypePropertyの削除
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp2", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内のEntityType名が不正な場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内のEntityType名が不正な場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_entitytype_name_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            Setup.entityTypeDelete("odatacol1", "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用の00_metadata_xml内に同じEntityTypeが存在する場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内に同じEntityTypeが存在する場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_entitytype_conflict.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            Setup.entityTypeDelete("odatacol1", "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用の00_metadata_xml内のComplexType名が不正な場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内のComplexType名が不正な場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_complextype_name_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用の00_metadata_xml内に同じComplexTypeが存在する場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内に同じComplexTypeが存在する場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_complextype_conflict.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";
        String complexUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            ODataCommon.deleteOdataResource(complexUrl);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内に同じAssociationEndが存在する場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内に同じAssociationEndが存在する場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_association_role_conflict.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, colName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, colName, "entity1", INSTALL_TARGET,
                    "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, colName, "entity2", INSTALL_TARGET,
                    "entity2-entity1", -1);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にAssociationEndのEntityが存在しない場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にAssociationEndのEntityが存在しない場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_association_entity_notexist.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
            String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
            AssociationEndUtils.deleteLink(reqCell, colName, INSTALL_TARGET, key, navKey, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, colName, "entity1", INSTALL_TARGET,
                    "entity1-entity2", -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, colName, "entity2", INSTALL_TARGET,
                    "entity2-entity1", -1);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にPropertyのName属性値が不正の場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にPropertyのName属性値が不正の場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_property_name_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にPropertyのCollectionKind属性値が不正の場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にPropertyのCollectionKind属性値が不正の場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_property_colkind_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にProperty名が重複した場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にProperty名が重複した場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_property_conflict.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String propUrl = UrlUtils.property(reqCell, INSTALL_TARGET, colName, "property1", "entity1");
            ODataCommon.deleteOdataResource(propUrl);
            String complexUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");
            ODataCommon.deleteOdataResource(complexUrl);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にComplexTypePropertyのName属性値が不正の場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にComplexTypePropertyのName属性値が不正の場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_compprop_name_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    colName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にComplexTypePropertyのCollectionKind属性値が不正の場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にComplexTypePropertyのCollectionKind属性値が不正の場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_compprop_colkind_invalid.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    colName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ODataCol用の00_metadata_xml内にComplexTypeProperty名が重複した場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xml内にComplexTypeProperty名が重複した場合に異常終了する() {
        final String barFilePath = "/V1_1_2_bar_compprop_conflict.bar";
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String colName = "odatacol1";

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            String resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    colName, "property1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, colName, "complex2");
            ODataCommon.deleteOdataResource(resourceUrl);
            Setup.entityTypeDelete(colName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(colName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
            BarInstallTestUtils.deleteCollection(colName);
        }
    }

    /**
     * ユーザデータの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType名
     * @param userId ユーザID
     */
    private void checkUserData(String cellName, String boxName, String colName, String entityName, String userId) {
        String url = UrlUtils.userdata(cellName, boxName, colName, entityName, userId);
        PersoniumResponse res = ODataCommon.getOdataResource(url);
        JSONObject json = res.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get("__id").toString();
        assertEquals(userId, value);
        value = results.get("property1").toString();
        assertEquals("ぷろぱてぃ１", value);
        JSONObject jsonObj = (JSONObject) results.get("property2");
        value = jsonObj.get("compProp1").toString();
        assertEquals("compProp1", value);
        value = results.get("property3").toString();
        assertEquals("プロパティ３", value);
    }

    /**
     * ユーザデータの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType名
     * @throws Exception レスポンスの読み込み失敗
     */
    private void checkUserDataForDouble(String cellName,
            String boxName,
            String colName,
            String entityName) throws Exception {
        final String query = "?$orderby=__id";
        final String token = PersoniumUnitConfig.getMasterToken();
        PersoniumResponse res = UserDataUtils.listEntities(cellName, boxName, colName, entityName, query, token,
                HttpStatus.SC_OK);
        String resString = res.bodyAsString();
        JSONObject json = (JSONObject) new JSONParser().parse(resString);
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        assertEquals(11, results.size());

        // 各登録データのレスポンスチェック
        expectJsonValue((JSONObject) results.get(0), 1, "-1");
        expectJsonValue((JSONObject) results.get(1), 2, "0");
        expectJsonValue((JSONObject) results.get(2), 3, "1");
        expectJsonValue((JSONObject) results.get(3), 4, "1"); // 1.0 は 1 に丸められる
        expectJsonValue((JSONObject) results.get(4), 5, "1.23456789012345E9"); // JSONパース時に指数へ変換(書式は他でテスト済み)
        expectJsonValue((JSONObject) results.get(5), 6, "-1.79E308");
        expectJsonValue((JSONObject) results.get(6), 7, "-2.23E-308");
        expectJsonValue((JSONObject) results.get(7), 8, "2.23E-308");
        expectJsonValue((JSONObject) results.get(8), 9, "1.79E308");

        // nullを登録した場合のレスポンスチェック
        // －CollectionKindが'None'の場合はデフォルト値が設定されている
        // －CollectionKindが'List'の場合はnullが設定されている
        JSONObject body = (JSONObject) results.get(9);
        String uid = String.format("double2-%02d", 10);
        assertEquals(uid, body.get("__id"));
        // Property: Edm.Double(None)
        assertEquals("12345.6789", body.get("prop2-1").toString());
        // Property: Edm.Double(List)
        assertNull(body.get("prop2-2"));
        // ComplexTypeProperty: Edm.Double(None)
        JSONObject innerJson = (JSONObject) body.get("prop2-3");
        assertEquals("12345.6789", innerJson.get("cprop2-1").toString());
        // ComplexTypeProperty: Edm.Double(List)
        assertNull(innerJson.get("cprop2-2"));

        // 空配列を登録した場合のレスポンスチェック
        body = (JSONObject) results.get(10);
        uid = String.format("double2-%02d", 11);
        assertEquals(uid, body.get("__id"));
        // Property: Edm.Double(None)
        assertEquals("12345.12345", body.get("prop2-1").toString());
        // Property: Edm.Double(List)
        JSONArray innerArray = (JSONArray) body.get("prop2-2");
        assertEquals(0, innerArray.size());
        // ComplexTypeProperty: Edm.Double(None)
        innerJson = (JSONObject) body.get("prop2-3");
        assertEquals("12345.12345", innerJson.get("cprop2-1").toString());
        // ComplexTypeProperty: Edm.Double(List)
        JSONArray innerCArray = (JSONArray) innerJson.get("cprop2-2");
        assertEquals(0, innerCArray.size());
    }

    /**
     * Double型ユーザODataのインストール結果をチェックする.
     * @param json レスポンスJSON
     * @param id ユーザODataの__id
     * @param expected 期待値（全プロパティで同じ値にしている）
     */
    private void expectJsonValue(JSONObject json, int id, String expected) {
        String uid = String.format("double2-%02d", id);
        assertEquals(uid, json.get("__id"));
        // Property: Edm.Double(None)
        assertEquals(expected, json.get("prop2-1").toString());
        // Property: Edm.Double(List)
        JSONArray innerArray = (JSONArray) json.get("prop2-2");
        if (null != expected) {
            assertEquals(2, innerArray.size());
        }
        for (Object item : innerArray) {
            assertEquals(expected, item.toString());
        }
        // ComplexTypeProperty: Edm.Double(None)
        JSONObject innerJson = (JSONObject) json.get("prop2-3");
        assertEquals(expected, innerJson.get("cprop2-1").toString());
        // ComplexTypeProperty: Edm.Double(List)
        JSONArray innerCArray = (JSONArray) innerJson.get("cprop2-2");
        if (null != expected) {
            assertEquals(2, innerCArray.size());
        }
        for (Object item : innerCArray) {
            assertEquals(expected, item.toString());
        }
    }
}
