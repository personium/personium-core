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
package io.personium.test.jersey.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.ResourceUtils;

/**
 * BoxURL取得 APIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxUrlTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public BoxUrlTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * 指定したschemaのBoxURLが取得できること.
     */
    @Test
    public final void 指定したschemaのBoxURLが取得できること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"),
                    res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 指定したローカルユニットschemaのBoxURLがLocalUnitで取得できること.
     */
    @Test
    public final void schemaパラメタとしてhttpURLの指定でlocalunitURLをschemaとするBoxが取得できること() {
        try {
            // テスト準備
            // スキーマ設定(Box更新)
            // Setupでセル1にBoxのSchemaとして登録されている urlをhttpからpersonium-localunitに一時的に更新。
            BoxUtils.update(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, "*", Setup.TEST_BOX1,
                    UriUtils.SCHEME_LOCALUNIT + ":/" + Setup.TEST_CELL_SCHEMA1 + "/", HttpStatus.SC_NO_CONTENT);

            // テスト実施
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            String httpUrl = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, httpUrl), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"),
                    res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            // Box Schema更新（元に戻す）
            BoxUtils.update(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, "*", Setup.TEST_BOX1,
                    UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * schemaパラメタとしてhttpURLの指定でlocalunitURLをschemaとするBoxが取得できること.
     */
    @Test
    public final void schemaパラメタとしてlocalunitURLの指定でhttpURLをschemaとするBoxが取得できること() {
        try {
            // Setupを流用
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            String localunitUrl = UriUtils.SCHEME_LOCALUNIT + ":/" + Setup.TEST_CELL_SCHEMA1 + "/";
            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, localunitUrl), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"),
                    res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, Setup.TEST_BOX1 + "/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 指定したローカルユニットschemaのBoxURLが不正な場合にエラーで返却されること.
     * http、https、persoium-localunit以外.
     */
    @Test
    public final void 指定したローカルユニットschemaのBoxURLが不正な場合にエラーで返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            String boxSchema = "testbox1";
            // ボックススキーマ名のみ
            String boxRoot = boxSchema + "/";
            String boxUrl = UrlUtils.boxUrl(Setup.TEST_CELL1, boxRoot);

            res = rest.getAcceptEncodingGzip(boxUrl, requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * BoxURL取得でPOST以外のメソッドを指定した場合に405が返却されること.
     */
    @Test
    public final void BoxURL取得でPOST以外のメソッドを指定した場合に405が返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.del(UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * schemaが空指定の場合に400が返却されること.
     */
    @Test
    public final void schemaが空指定の場合に400が返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1, ""), requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("schema", "");
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * URI形式でないschemaを指定した場合に400が返却されること.
     */
    @Test
    public final void URI形式でないschemaを指定した場合に400が返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1, "test"), requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("schema", "test");
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 指定したschemaのBoxが存在しない場合に403が返却されること.
     */
    @Test
    public final void 指定したschemaのBoxが存在しない場合に403が返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * マスタートークンを使用してschema指定がない場合に403が返却されること.
     */
    @Test
    public final void マスタートークンを使用してschema指定がない場合に403が返却されること() {
        try {
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        }
    }

    /**
     * BoxRead権限のあるユーザーでschema指定がある場合に302が返却されること.
     */
    @Test
    public final void BoxRead権限のあるユーザーでschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * アクセス権限のないユーザーでschema指定がある場合に403が返却されること.
     */
    @Test
    public final void アクセス権限のないユーザーでschema指定がある場合に403が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account2", "password2");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * ALL権限のあるユーザーでschema指定がある場合に302が返却されること.
     */
    @Test
    public final void ALL権限のあるユーザーでschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:all/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * 誰でも参照可能な設定でトークンを使用せずschema指定がある場合に302が返却されること.
     */
    @Test
    public final void 誰でも参照可能な設定でトークンを使用せずschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:all/>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * 誰でも参照可能な設定で参照権限ありのトークンを使用してschema指定がある場合に302が返却されること.
     */
    @Test
    public final void 誰でも参照可能な設定で参照権限ありのトークンを使用してschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:all/>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * 誰でも参照可能な設定で参照権限なしのトークンを使用してschema指定がある場合に302が返却されること.
     */
    @Test
    public final void 誰でも参照可能な設定で参照権限なしのトークンを使用してschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:all/>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:write/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * 誰でも参照可能な設定で不正トークンを使用してschema指定がある場合に302が返却されること.
     */
    @Test
    public final void 誰でも参照可能な設定で不正トークンを使用してschema指定がある場合に302が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:all/>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer test");

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * 不正トークンを使用してschema指定がある場合に401が返却されること.
     */
    @Test
    public final void 不正トークンを使用してschema指定がある場合に401が返却されること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer test");

            res = rest.getAcceptEncodingGzip(
                    UrlUtils.boxUrl(Setup.TEST_CELL1, UrlUtils.cellRoot("boxurltestschema")),
                    requestheaders);
            assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreAuthzException.TOKEN_PARSE_ERROR;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がなしかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がなしかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がnoneかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がnoneかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='none' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がpublicかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がpublicかつクエリに指定されたスキーマのPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='public' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がconfidentialClientかつクエリに指定されたスキーマのConfidentialClientトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がconfidentialClientかつクエリに指定されたスキーマのConfidentialClientトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='confidential' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がなしかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がなしかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaAuthz(Setup.TEST_CELL_SCHEMA1));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がnoneかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がnoneかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='none' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";

            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaAuthz(Setup.TEST_CELL_SCHEMA1));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);

            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がpublicかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できないこと.
     */
    @Test
    public final void スキーマ設定がpublicかつクエリに指定されたスキーマでないPublicトークンを使用してクエリに指定されたスキーマのボックスURLが取得できないこと() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='public' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaAuthz(Setup.TEST_CELL_SCHEMA1));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.SCHEMA_MISMATCH;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がconfidentialClientかつクエリに指定されたスキーマでないConfidentialClientトークンを使用してクエリに指定されたスキーマのボックスURLが取得できないこと.
     */
    @Test
    public final void スキーマ設定がconfidentialかつクエリに指定されたスキーマでないConfidentialClientトークンを使用してクエリに指定されたスキーマのボックスURLが取得できないこと() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='confidential' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaAuthz(Setup.TEST_CELL_SCHEMA1));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1,
                    UrlUtils.cellRoot("boxurltestschema")), requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.SCHEMA_MISMATCH;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がnoneの場合にアクセストークンを使用してボックスURLが取得できないこと.
     */
    @Test
    public final void スキーマ設定がnoneの場合にアクセストークンを使用してボックスURLが取得できないこと() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='none' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());

        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がpublicの場合にアクセストークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がpublicの場合にアクセストークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='public' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());

        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がconfidentialの場合にアクセストークンを使用してボックスURLが取得できないこと.
     */
    @Test
    public final void スキーマ設定がconfidentialの場合にアクセストークンを使用してボックスURLが取得できないこと() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='confidential' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            String token = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account1", "password1");
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            PersoniumCoreException e = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            checkErrorResponse(res.bodyAsJson(), e.getCode(), e.getMessage());

        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            afterACLTest();
        }
    }

    /**
     * スキーマ設定をしていない場合にPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定をしていない場合にPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がnoneの場合にPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がnoneの場合にPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='none' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がpublicの場合にPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がpublicの場合にPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='public' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がconfidentialの場合にPublicトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がconfidentialの場合にPublicトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='confidential' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken("client"));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell("client");
            afterACLTest();
        }
    }

    /**
     * スキーマ設定をしていない場合にConfidentialClientトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定をしていない場合にConfidentialClientトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がnoneの場合にConfidentialClientトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がnoneの場合にConfidentialClientトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='none' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がpublicの場合にConfidentialClientトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がpublicの場合にConfidentialClientトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='public' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ設定がconfidentialの場合にConfidentialClientトークンを使用してボックスURLが取得できること.
     */
    @Test
    public final void スキーマ設定がconfidentialの場合にConfidentialClientトークンを使用してボックスURLが取得できること() {
        try {
            String aclXml = String.format("<D:acl xmlns:D='DAV:' xmlns:p='urn:x-personium:xmlns'"
                    + " p:requireSchemaAuthz='confidential' xml:base='%s/%s/__role/__/'>",
                    UrlUtils.getBaseUrl(), Setup.TEST_CELL1)
                    + "  <D:ace>"
                    + "    <D:principal>"
                    + "      <D:href>role1</D:href>"
                    + "    </D:principal>"
                    + "    <D:grant>"
                    + "      <D:privilege><D:read/></D:privilege>"
                    + "    </D:grant>"
                    + "  </D:ace>"
                    + "</D:acl>";
            beforeACLTest(aclXml);

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer "
                    + getSchemaToken(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME));

            res = rest.getAcceptEncodingGzip(UrlUtils.boxUrl(Setup.TEST_CELL1), requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.getFirstHeader(HttpHeaders.LOCATION));
            assertEquals(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest/"), res.bodyAsJson().get("Url"));
        } catch (PersoniumException e) {
            fail(e.getMessage());
        } finally {
            deleteApplicationCell(OAuth2Helper.Key.CONFIDENTIAL_ROLE_NAME);
            afterACLTest();
        }
    }

    /**
     * スキーマ認証済みのトークンを取得.
     */
    private String getSchemaToken(String role) {
        String token = null;
        try {
            // アプリセルの作成
            createAppCell();

            // アカウント作成
            createAccountForAppCell();

            // ロール作成
            createRoleForAppCell(role);

            // アカウントとロールのリンク作成
            linkAccountRole(role);

            // スキーマ認証トークンを返却する
            token = getSchemaAuthz(null);

        } catch (PersoniumException e) {
            fail("getConfidentialSchemaToken Fail : " + e.getMessage());
        }
        return token;
    }

    /**
     * アプリセルの削除.
     */
    private String deleteApplicationCell(String role) {
        String token = null;
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // アカウントとロールのリンク削除
        unlinkAccountRole(role);

        // ロール削除
        deleteRoleForAppCell(role);

        // アカウント削除
        try {
            rest = new PersoniumRestAdapter();
            rest.del(UrlUtils.cellCtl("boxurltestschema", "Account", "account1"), requestheaders);
        } catch (PersoniumException e) {
            System.out.println("boxurltestschema/__ctl/Account('account1') delete Fail : " + e.getMessage());
        }

        // アプリセル削除
        try {
            rest = new PersoniumRestAdapter();
            rest.del(UrlUtils.unitCtl("Cell", "boxurltestschema"), requestheaders);

        } catch (PersoniumException e) {
            System.out.println("boxurltestschema delete Fail : " + e.getMessage());
        }
        return token;
    }

    /**
     * ACL関連のテスト前処理.
     * @param aclXml ACL設定情報
     */
    @SuppressWarnings("unchecked")
    private void beforeACLTest(String aclXml) {
        try {
            // Box作成
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            JSONObject body = new JSONObject();
            body.put("Name", "boxUrlTest");
            body.put("Schema", UrlUtils.cellRoot("boxurltestschema"));
            res = rest.post(UrlUtils.cellCtl(Setup.TEST_CELL1, "Box"), body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

            // BoxACL設定
            rest = new PersoniumRestAdapter();
            res = null;
            requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestheaders.put("X-HTTP-Method-Override", "ACL");
            res = rest.post(UrlUtils.boxRoot(Setup.TEST_CELL1, "boxUrlTest"), aclXml, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        } catch (PersoniumException e) {
            fail("beforeACLTest Fail : " + e.getMessage());
        }
    }

    /**
     * ACL関連のテスト後処理.
     */
    private void afterACLTest() {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // Box削除
        try {
            res = rest.del(UrlUtils.cellCtl(Setup.TEST_CELL1, "Box", "boxUrlTest"), requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        } catch (PersoniumException e) {
            fail("afterACLTest Fail : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void createAppCell() throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        JSONObject body = new JSONObject();
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        body.put("Name", "boxurltestschema");
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        res = rest.post(UrlUtils.unitCtl("Cell"), body.toJSONString(), requestheaders);
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private void createAccountForAppCell() throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        JSONObject body = new JSONObject();
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        body.put("Name", "account1");
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put("X-Personium-Credential", "password1");
        res = rest.post(UrlUtils.cellCtl("boxurltestschema", "Account"), body.toJSONString(), requestheaders);
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private void createRoleForAppCell(String roleName) throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        JSONObject body = new JSONObject();
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        body.put("Name", roleName);
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        res = rest.post(UrlUtils.cellCtl("boxurltestschema", "Role"), body.toJSONString(), requestheaders);
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
    }

    private void linkAccountRole(String roleName) throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        String linkBody = String.format("{\"uri\":\"%s\"}",
                UrlUtils.getBaseUrl() + "/boxurltestschema/__ctl/Role('" + roleName + "')");
        res = rest.post(UrlUtils.getBaseUrl() + "/boxurltestschema/__ctl/Account('account1')/$links/_Role",
                linkBody, requestheaders);
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    private String getSchemaAuthz(String cell) throws PersoniumException {
        if (cell == null) {
            cell = "boxurltestschema";
        }
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(javax.ws.rs.core.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        // クライアントシークレット取得
        String authBody = "grant_type=password&username=account1&password=password1&p_target="
                + UrlUtils.cellRoot(Setup.TEST_CELL1);
        res = rest.post(UrlUtils.auth(cell), authBody,
                requestheaders);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // スキーマ認証
        authBody = "grant_type=password&username=account1&password=password1"
                + String.format("&client_id=%s", UrlUtils.cellRoot(cell))
                + String.format("&client_secret=%s", res.bodyAsJson().get("access_token"));
        res = rest.post(UrlUtils.auth(Setup.TEST_CELL1), authBody,
                requestheaders);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        return res.bodyAsJson().get("access_token").toString();
    }

    private void unlinkAccountRole(String roleName) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // アカウントとロールのリンク削除
        try {
            rest.del(UrlUtils.getBaseUrl() + "/boxurltestschema/__ctl/Account('account1')/$links/_Role('"
                    + roleName + "')", requestheaders);
        } catch (PersoniumException e) {
            System.out.println("/boxurltestschema/__ctl/Account('account1')/$links/_Role('"
                    + roleName + "') delete Fail : " + e.getMessage());
        }
    }

    private void deleteRoleForAppCell(String roleName) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // ロール削除
        try {
            rest.del(UrlUtils.cellCtl("boxurltestschema", "Role", roleName), requestheaders);
        } catch (PersoniumException e) {
            System.out.println("/boxurltestschema/__ctl/Role('" + roleName + "') delete Fail : " + e.getMessage());
        }

    }
}
