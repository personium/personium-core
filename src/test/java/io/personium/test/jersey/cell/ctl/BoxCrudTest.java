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
package io.personium.test.jersey.cell.ctl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * BoxのCRUDのIT.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxCrudTest extends ODataCommon {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(BoxCrudTest.class);

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BoxCrudTest() {
        super("io.personium.core.rs");
    }

    private static final String CELL_NAME = Setup.TEST_CELL1;
    private static final String ENTITY_TYPE_BOX = "Box";
    private static final String TEST_BOX_NAME = "testBox";
    private static final String TEST_BOX_NAME_WITH_SCHEMA = "testBoxWithSchema";
    private static final String TEST_BOX_NAME_WITH_SCHEMA2 = "testBoxWithSchema2";
    private static final String TEST_BOX_SCHEMA = "https://example.com/schema1/";

    /**
     * BOX新規登録のテストSchema指定なし.
     */
    @Test
    public void BOX新規登録のテストSchema指定なし() {
        try {
            createBoxRequest(TEST_BOX_NAME, null)
                    .returns().statusCode(201);
            createBoxRequest(TEST_BOX_NAME, null)
                    .returns().statusCode(409);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME).returns();
        }
    }

    /**
     * BOX新規登録時にNameに空文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にNameに空文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"", TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にNameにアンダーバー始まり文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にNameにアンダーバー始まり文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"_xxx", TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にNameにハイフン始まり文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にNameにハイフン始まり文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"-xxx", TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にNameにスラッシュ文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にNameにスラッシュ文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"xx/xx", TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にNameに__ctlを指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時に__ctlを指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"__ctl", TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にNameを1文字指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にNameを1文字指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String boxName = "1";
        String[] key = {"Name", "Schema" };
        String[] value = {boxName, TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest(boxName).returns();
        }
    }

    /**
     * BOX新規登録時にNameを128文字指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にNameを128文字指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String boxName = "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678";
        String[] key = {"Name", "Schema" };
        String[] value = {boxName, TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest(boxName).returns();
        }
    }

    /**
     * BOX新規登録時にNameを129文字指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にNameを129文字指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String boxName = "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678a";
        String[] key = {"Name", "Schema" };
        String[] value = {boxName, TEST_BOX_SCHEMA };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchamaに空文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchamaに空文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchamaにtrailing_slashの無いURL形式文字_https_を指定した場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchamaにtrailing_slashの無いURL形式文字_https_を指定した場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "https://xxx.com/test" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchamaに正規化されていないパスを含むURL形式文字_https_を指定した場合_400になることを確認.
     */
    @Test
    public void  BOX新規登録時にSchamaに正規化されていないパスを含むURL形式文字_https_を指定した場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "https://xxx.com/test/1/2/../3/./../test/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchemaをURL形式文字_https_を指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをURL形式_https_文字を指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "https://xxx.com/test/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest("testBox").returns();
        }
    }

    /**
     * BOX新規登録時にSchamaにtrailing_slashの無いURL形式文字_http_を指定した場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchamaにtrailing_slashの無いURL形式文字_http_を指定した場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "http://xxx.com/test" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchamaに正規化されていないパスを含むURL形式文字_http_を指定した場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchamaに正規化されていないパスを含むURL形式文字_http_を指定した場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "http://xxx.com/test/0/../1/./2/test/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchemaをURL形式文字_http_を指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをURL形式_http_文字を指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "http://xxx.com/test/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest("testBox").returns();
        }
    }

    /**
     * BOX新規登録時にSchemaをURL形式personium_localunit文字を指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをURL形式personium_localunit文字を指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "personium-localunit:/schema/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest("testBox").returns();
        }
    }

    /**
     * BOX新規登録時にSchemaをURN形式文字を指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをURN形式文字を指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "urn:xxx:xxx" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 201になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest("testBox").returns();
        }
    }

    /**
     * BOX新規登録時にSchemaをFTP形式文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをFTP形式文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "ftp://xxx.com" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchemaに1024文字を指定して場合_201になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaに1024文字を指定して場合_201になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String schema = "http://" + StringUtils.repeat("x", 1012) + ".com/";
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", schema };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        try {
            PersoniumResponse res = request(req);

            // 400になることを確認
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } finally {
            deleteBoxRequest("testBox").returns();
        }
    }

    /**
     * BOX新規登録時にSchemaに1025文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaに1025文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String schema = "http://" + StringUtils.repeat("x", 1013) + ".com/";
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", schema };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にSchemaをURL形式でない文字を指定して場合_400になることを確認.
     */
    @Test
    public void BOX新規登録時にSchemaをURL形式でない文字を指定して場合_400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema" };
        String[] value = {"testBox", "xxx" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にリクエストボディに管理情報__publishedを指定した場合_レスポンスコード400になることを確認.
     */
    @Test
    public void BOX新規登録時にリクエストボディに管理情報__publishedを指定した場合_レスポンスコード400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema", PUBLISHED };
        String[] value = {TEST_BOX_NAME, "http://xxx.com/test", "/Date(0)/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にリクエストボディに管理情報__updatedを指定した場合_レスポンスコード400になることを確認.
     */
    @Test
    public void BOX新規登録時にリクエストボディに管理情報__updatedを指定した場合_レスポンスコード400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema", UPDATED };
        String[] value = {TEST_BOX_NAME, "http://xxx.com/test", "/Date(0)/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録時にリクエストボディに管理情報__metadataを指定した場合_レスポンスコード400になることを確認.
     */
    @Test
    public void BOX新規登録時にリクエストボディに管理情報__metadataを指定した場合_レスポンスコード400になることを確認() {
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
        String[] key = {"Name", "Schema", METADATA };
        String[] value = {TEST_BOX_NAME, "http://xxx.com/test", "test" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        PersoniumResponse res = request(req);

        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * BOX新規登録のテストSchema指定あり.
     */
    @Test
    public void BOX新規登録のテストSchema指定あり() {
        try {
            log.debug("Creating box");
            createBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, TEST_BOX_SCHEMA)
                    .returns().statusCode(201);
            // 主キー衝突
            createBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, TEST_BOX_SCHEMA)
                    .returns().statusCode(409);
            // UK衝突
            log.debug("Creating box 2");
            createBoxRequest(TEST_BOX_NAME_WITH_SCHEMA2, TEST_BOX_SCHEMA)
                    .returns().statusCode(409);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME_WITH_SCHEMA).returns();
            deleteBoxRequest(TEST_BOX_NAME_WITH_SCHEMA2).returns();
        }
    }

    /**
     * BOX更新のテスト.
     */
    @Test
    public void BOX更新のテスト() {
        try {
            log.debug("Creating box");
            // BOX1
            String etag1 = createBoxRequest(TEST_BOX_NAME, null)
                    .returns().statusCode(201).getHeader(HttpHeaders.ETAG);

            // Box2
            String etag2 = createBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, null)
                    .returns().statusCode(201).getHeader(HttpHeaders.ETAG);

            // Box2のUK変更が成功(ここでスキーマつきになる)
            etag2 = updateBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, TEST_BOX_NAME_WITH_SCHEMA,
                    TEST_BOX_SCHEMA, etag2)
                    .returns().statusCode(204).getHeader(HttpHeaders.ETAG);

            // Box2のEtag違い更新が失敗
            updateBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, TEST_BOX_NAME, TEST_BOX_SCHEMA, etag1)
                    .returns().statusCode(412);

            // Box2の主キー変更が衝突で失敗
            updateBoxRequest(TEST_BOX_NAME_WITH_SCHEMA, TEST_BOX_NAME, TEST_BOX_SCHEMA, etag2)
                    .returns().statusCode(409);

            // Box1のUK変更が衝突で失敗
            updateBoxRequest(TEST_BOX_NAME, TEST_BOX_NAME, TEST_BOX_SCHEMA, etag1)
                    .returns().statusCode(409);

            // Box1のUK変更が成功
            updateBoxRequest(TEST_BOX_NAME, TEST_BOX_NAME, "http://example.net/hoge/", etag1)
                    .returns().statusCode(204).getHeader(HttpHeaders.ETAG);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME_WITH_SCHEMA).returns();
            deleteBoxRequest(TEST_BOX_NAME).returns();
        }
    }

    /**
     * BOX更新時にリクエストボディに管理情報__publishedを指定した場合_レスポンスコード400になることを確認.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void BOX更新時にリクエストボディに管理情報__publishedを指定した場合_レスポンスコード400になることを確認() {
        try {
            PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
            String[] key = {"Name", "Schema" };
            String[] value = {TEST_BOX_NAME, "http://xxx.com/test" };
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
            request(req);

            JSONObject updateBody = new JSONObject();
            updateBody.put("Name", "updateBox");
            updateBody.put(PUBLISHED, "/Date(0)/");

            updateBoxRequest(TEST_BOX_NAME, "*", updateBody.toJSONString(), HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME).returns();
        }
    }

    /**
     * BOX新規登録時にリクエストボディに管理情報__updatedを指定した場合_レスポンスコード400になることを確認.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void BOX更新時にリクエストボディに管理情報__updatedを指定した場合_レスポンスコード400になることを確認() {
        try {
            PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
            String[] key = {"Name", "Schema" };
            String[] value = {TEST_BOX_NAME, "http://xxx.com/test" };
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
            request(req);

            JSONObject updateBody = new JSONObject();
            updateBody.put("Name", "updateBox");
            updateBody.put(UPDATED, "/Date(0)/");

            updateBoxRequest(TEST_BOX_NAME, "*", updateBody.toJSONString(), HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME).returns();
        }
    }

    /**
     * BOX新規登録時にリクエストボディに管理情報__metadataを指定した場合_レスポンスコード400になることを確認.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void BOX更新時にリクエストボディに管理情報__metadataを指定した場合_レスポンスコード400になることを確認() {
        try {
            PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(CELL_NAME, ENTITY_TYPE_BOX));
            String[] key = {"Name", "Schema" };
            String[] value = {TEST_BOX_NAME, "http://xxx.com/test" };
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
            request(req);

            JSONObject updateBody = new JSONObject();
            updateBody.put("Name", "updateBox");
            updateBody.put(METADATA, "test");

            updateBoxRequest(TEST_BOX_NAME, "*", updateBody.toJSONString(), HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteBoxRequest(TEST_BOX_NAME).returns();
        }
    }

    /**
     * Box削除時にDav用の管理データが削除されることを確認.
     */
    @Test
    public void Box削除時にDav用の管理データが削除されることを確認() {
        String boxName = "boxDavName";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // DAV管理データ作成のため、PROPFINDの実行
            DavResourceUtils.propfind("box/propfind-box-allprop.txt", MASTER_TOKEN_NAME,
                    HttpStatus.SC_MULTI_STATUS, boxName);

            // PROPFINDを行い、Box管理用のDAVが存在する確認する
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop.txt", MASTER_TOKEN_NAME,
                    HttpStatus.SC_MULTI_STATUS, boxName);
            Element root = res.bodyAsXml().getDocumentElement();
            NodeList response = root.getElementsByTagName("response");
            assertTrue("response length is " + response.getLength(), response.getLength() > 0);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, boxName, HttpStatus.SC_NO_CONTENT);

            // PROPFINDを行い、Box管理用のDAVが無いこと確認する
            DavResourceUtils.propfind("box/propfind-box-allprop.txt", MASTER_TOKEN_NAME,
                    HttpStatus.SC_NOT_FOUND, boxName);

        } finally {
            deleteBoxRequest(boxName).returns();
        }
    }

    /**
     * Box名にクオート無しの数値型式名を指定して取得した場合400エラーとなること.
     */
    @Test
    public final void Box名にクオート無しの数値型式名を指定して取得した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String boxName = "123456";
        String url = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, "Box", boxName);
        PersoniumResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        checkErrorResponse(res.bodyAsJson(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * Box名にクオート無しの数値型式名を指定して更新した場合400エラーとなること.
     */
    @Test
    public final void Box名にクオート無しの数値型式名を指定して更新した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String boxName = "123456";
        String url = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, "Box", boxName);
        PersoniumResponse res = this.restPut(url, "");

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        checkErrorResponse(res.bodyAsJson(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * Box名にクオート無しの数値型式名を指定して削除した場合400エラーとなること.
     */
    @Test
    public final void Box名にクオート無しの数値型式名を指定して削除した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String boxName = "123456";
        String url = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, "Box", boxName);
        PersoniumResponse res = this.restDelete(url);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        checkErrorResponse(res.bodyAsJson(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * Box作成リクエストを生成.
     * @param name Box名
     * @param schema schema url
     * @return リクエストオブジェクト
     */
    public static Http createBoxRequest(String name, String schema) {
        if (schema == null) {
            return Http.request("cell/box-create.txt").with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME).with("boxPath", name);
        } else {
            return Http.request("cell/box-create-with-scheme.txt").with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME).with("boxPath", name).with("schema", schema);
        }
    }

    /**
     * Box更新リクエストを生成(リクエストボディを指定する).
     * @param name Box名
     * @param etag Etag
     * @param body リクエストボディ
     * @param sc 期待するレスポンスコード
     */
    public static void updateBoxRequest(String name, String etag, String body, int sc) {
        Http.request("cell/box-update-without-body.txt")
                .with("cellPath", CELL_NAME)
                .with("boxPath", name)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("etag", etag)
                .with("body", body)
                .returns()
                .statusCode(sc);
    }

    /**
     * Box更新リクエストを生成.
     * @param name Box名
     * @param newName 新しいBox名
     * @param schema 新しいBoxのSchema
     * @param etag Etag
     * @return リクエストオブジェクト
     */
    public static Http updateBoxRequest(String name, String newName, String schema, String etag) {
        return Http.request("cell/box-update.txt").with("cellPath", CELL_NAME).with("boxPath", name)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("etag", etag).with("newBoxPath", newName)
                .with("schema", schema);
    }

    /**
     * Box削除リクエストを生成.
     * @param name Box名
     * @return リクエストオブジェクト
     */
    public static Http deleteBoxRequest(String name) {
        return Http.request("cell/box-delete.txt")
                .with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("boxPath", name);
    }
}
