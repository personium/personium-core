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
package io.personium.test.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.utils.CommonUtils.HttpHeaders;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * 認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class CrossDomainTest extends PersoniumTest {

    static final String ORIGIN = "example.com";
    static final String TEST_CELL1 = "testcell1";
    static final String ODATA_COL = "odatacol";
    static final String CROSSDOMAIN_XML = "<?xml version=\"1.0\"?>"
            + "<!DOCTYPE cross-domain-policy SYSTEM \"https://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">"
            + "<cross-domain-policy>"
            + "  <site-control permitted-cross-domain-policies=\"all\"/>"
            + "  <allow-access-from domain=\"*\"/>"
            + "  <allow-http-request-headers-from domain=\"*\" headers=\"*\"/>"
            + "</cross-domain-policy>";

    /**
     * コンストラクタ.
     */
    public CrossDomainTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * クロスドメインポリシーファイルが取得できること.
     */
    @Test
    public final void クロスドメインポリシーファイルが取得できること() {
        TResponse response =
                Http.request("crossdomain/crossdomainxml.txt")
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        String body = response.getBody();
        assertNotNull(body);
        String xml = CROSSDOMAIN_XML.replaceAll(">[\\s]*<", "><");
        body = body.replaceFirst("<!-- .*-->", "");
        body = body.replaceAll(">[\\s]*<", "><");
        assertEquals(xml, body);
    }

    /**
     * ユニット管理にOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ユニットサービスドキュメントにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/__ctl")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET");
    }

    /**
     * ユニット管理にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ユニットサービスドキュメントにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/__ctl")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * ユニットメタデータにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ユニットメタデータにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/__ctl/\\$metadata")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET");
    }

    /**
     * ユニットメタデータにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ユニットメタデータにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/__ctl/\\$metadata")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * CellEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void CellEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/__ctl/Cell")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * CellEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void CellEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/__ctl/Cell")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * CellEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void CellEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/__ctl/Cell('hoho')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * CellEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void CellEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/__ctl/Cell('hoho')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 認証にOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 認証にOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__token")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, POST");
    }

    /**
     * 認証にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 認証にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__token")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * CellサービスドキュメントにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void CellサービスドキュメントにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, POST, PROPFIND");
    }

    /**
     * CellサービスドキュメントにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void CellサービスドキュメントにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers-accept-headers.txt")
                        .with("path", "/testcell1")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .with("accept", MediaType.APPLICATION_XML)
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * Cell管理にOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void Cell管理にOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET");
    }

    /**
     * Cell管理にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void Cell管理にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * アカウントEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void アカウントEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Account")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * アカウントEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void アカウントEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Account")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * アカウントEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void アカウントEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Account('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * アカウントEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void アカウントEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Account('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * メッセージ送信EntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void メッセージ送信EntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/" + SentMessage.EDM_TYPE_NAME + "('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * メッセージ送信EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void メッセージ送信EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/" + SentMessage.EDM_TYPE_NAME + "('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * メッセージ送信EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void メッセージ送信EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/" + SentMessage.EDM_TYPE_NAME)
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * メッセージ送信EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void メッセージ送信EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/" + SentMessage.EDM_TYPE_NAME)
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * メッセージ受信EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void メッセージ受信EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/" + ReceivedMessage.EDM_TYPE_NAME)
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * メッセージ受信EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void メッセージ受信EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/" + ReceivedMessage.EDM_TYPE_NAME)
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * メッセージ受信EntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void メッセージ受信EntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/" + ReceivedMessage.EDM_TYPE_NAME + "('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * メッセージ受信EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void メッセージ受信EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/" + ReceivedMessage.EDM_TYPE_NAME + "('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 外部ロールEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 外部ロールEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/ExtRole")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * 外部ロールEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 外部ロールEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/ExtRole")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 外部ロールEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 外部ロールEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/ExtRole('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * 外部ロールEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 外部ロールEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/ExtRole('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 関係EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 関係EntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Relation")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * 関係EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 関係EntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Relation")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 関係EntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 関係EntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Relation('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * 関係EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 関係EntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Relation('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * ロールEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ロールEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Role")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * ロールEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ロールEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Role")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * ロールEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ロールEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Role('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * ロールEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ロールEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Role('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * ロール割り当てにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ロール割り当てにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Account('account14')/\\$links/_Role")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, DELETE, PUT, POST");
    }

    /**
     * ロール割り当てにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ロール割り当てにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Account('account4')/\\$links/_Role")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * BoxEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void BoxEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Box")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * BoxEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void BoxEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Box")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * BoxEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void BoxEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/Box('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * BoxEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void BoxEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/Box('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 外部セルEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 外部セルEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/ExtCell")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * 外部セルEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 外部セルEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/ExtCell")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * 外部セルEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void 外部セルEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/__ctl/ExtCell('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
    }

    /**
     * 外部セルEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void 外部セルEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/__ctl/ExtCell('id')")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * BoxサービスドキュメントにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void BoxサービスドキュメントにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/box1")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, PUT, DELETE, MKCOL, PROPFIND, PROPPATCH, ACL");
    }

    /**
     * BoxサービスドキュメントにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void BoxサービスドキュメントにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/box1")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * ServcieCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ServcieCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        try {
            // コレクションの作成
            createServiceCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/servicecol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, DELETE, MOVE, PROPFIND, PROPPATCH, ACL");
        } finally {
            // コレクションの削除
            deleteServiceCollection();
        }
    }

    /**
     * ServcieCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ServcieCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        try {
            // コレクションの作成
            createServiceCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/servicecol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteServiceCollection();
        }
    }

    /**
     * Servcie実行にOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Ignore
    @Test
    public final void Servcie実行にOPTIONSを指定してXHR2ヘッダーが返却されること() {
        // TODO サービス実行が実装された後に対応すること
        try {
            // コレクションの作成
            createServiceCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/servicecol/service")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, DELETE, PROPFIND, PROPPATCH, ACL");
        } finally {
            // コレクションの削除
            deleteServiceCollection();
        }
    }

    /**
     * Servcie実行にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Ignore
    @Test
    public final void Servcie実行にGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        // TODO サービス実行が実装された後に対応すること
        try {
            // コレクションの作成
            createServiceCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/servicecol/service")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteServiceCollection();
        }
    }

    /**
     * WebDAVCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void WebDAVCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        try {
            // コレクションの作成
            createDavCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/davcol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET, PUT, DELETE, MKCOL, MOVE, PROPFIND, PROPPATCH, ACL");
        } finally {
            // コレクションの削除
            deleteDavCollection();
        }
    }

    /**
     * WebDAVCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void WebDAVCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        try {
            // コレクションの作成
            createDavCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/davcol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteDavCollection();
        }
    }

    /**
     * WebDAVにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void WebDAVにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        try {
            // コレクションの作成
            createDavCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/davcol/test.txt")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET, PUT, DELETE, MKCOL, PROPFIND, PROPPATCH, ACL");
        } finally {
            // コレクションの削除
            deleteDavCollection();
        }
    }

    /**
     * WebDAVにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void WebDAVにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        try {
            // コレクションの作成
            createDavCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/davcol/test.txt")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_NOT_FOUND)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteDavCollection();
        }
    }

    /**
     * ODataCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ODataCollectionにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        try {
            // コレクションの作成
            createODataCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/odatacol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET, DELETE, MOVE, PROPFIND, PROPPATCH, ACL");
        } finally {
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ODataCollectionにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        try {
            // コレクションの作成
            createODataCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/odatacol")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataMetadataにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ODataMetadataにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        try {
            // コレクションの作成
            createODataCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/odatacol/\\$metadata")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET");
        } finally {
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataMetadataにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ODataMetadataにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        try {
            // コレクションの作成
            createODataCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/odatacol/\\$metadata")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void ODataEntitiesにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        String entSetName = "entity";
        try {
            // コレクションの作成
            createODataCollection();

            // EntityTypeの作成
            EntityTypeUtils.create(TEST_CELL1, PersoniumUnitConfig.getMasterToken(), ODATA_COL,
                    entSetName, HttpStatus.SC_CREATED);

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/odatacol/entity")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET, POST");
        } finally {
            // EntityTypeの削除
            EntityTypeUtils.delete(ODATA_COL, PersoniumUnitConfig.getMasterToken(), "application/xml",
                    entSetName, TEST_CELL1, -1);
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ODataEntitiesにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        String entSetName = "entity";
        try {
            // コレクションの作成
            createODataCollection();

            // EntityTypeの作成
            EntityTypeUtils.create(TEST_CELL1, PersoniumUnitConfig.getMasterToken(), ODATA_COL,
                    entSetName, HttpStatus.SC_CREATED);

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/odatacol/entity")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // EntityTypeの削除
            EntityTypeUtils.delete(ODATA_COL, PersoniumUnitConfig.getMasterToken(), "application/xml",
                    entSetName, TEST_CELL1, -1);
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Ignore
    @Test
    public final void ODataEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        // TODO Enitiy型登録が実装された後に対応すること checkXHR2Headerの期待する値見直し
        String entSetName = "id";
        try {
            // コレクションの作成
            createODataCollection();

            // EntityTypeの作成
            EntityTypeUtils.create(TEST_CELL1, PersoniumUnitConfig.getMasterToken(), ODATA_COL,
                    entSetName, HttpStatus.SC_CREATED);

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight.txt")
                            .with("path", "/testcell1/box1/odatacol/id")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_OK)
                            .debug();
            checkXHR2Header(response, "OPTIONS, GET, PUT, MERGE, DELETE");
        } finally {
            // EntityTypeの削除
            EntityTypeUtils.delete(ODATA_COL, PersoniumUnitConfig.getMasterToken(), "application/xml",
                    entSetName, TEST_CELL1, -1);
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * ODataEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void ODataEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {

        try {
            // コレクションの作成
            createODataCollection();

            TResponse response =
                    Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                            .with("path", "/testcell1/box1/odatacol/entity('id')")
                            .with("token", PersoniumUnitConfig.getMasterToken())
                            .returns()
                            .statusCode(HttpStatus.SC_NOT_FOUND)
                            .debug();
            checkXHR2HeaderOnlyOrigin(response);
        } finally {
            // コレクションの削除
            deleteODataCollection();
        }
    }

    /**
     * NP経由ODataEntityにOPTIONSを指定してXHR2ヘッダーが返却されること.
     */
    @Test
    public final void NP経由ODataEntityにOPTIONSを指定してXHR2ヘッダーが返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/box1/setodata/SalesDetail('userdata001')/_Sales")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkXHR2Header(response, "OPTIONS, GET, POST");
    }

    /**
     * NP経由ODataEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること.
     */
    @Test
    public final void NP経由ODataEntityにGETを指定してXHR2ヘッダーのALLOW_ORIGINのみ返却されること() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight-no-access-control-allow-headers.txt")
                        .with("path", "/testcell1/box1/odatacol/SalesDetail('userdata001')/_Sales")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_NOT_FOUND)
                        .debug();
        checkXHR2HeaderOnlyOrigin(response);
    }

    /**
     * レスポンスヘッダにXDcVersionが返却されることのテスト.
     */
    @Test
    public final void レスポンスヘッダにXDcVersionが返却されることのテスト() {
        TResponse response =
                Http.request("crossdomain/xhr2-preflight.txt")
                        .with("path", "/testcell1/box1/setodata/SalesDetail('userdata001')/_Sales")
                        .with("token", PersoniumUnitConfig.getMasterToken())
                        .returns()
                        .statusCode(HttpStatus.SC_OK)
                        .debug();
        checkDcVersionHeader(response);
    }

    /**
     * ServiceCollectionの作成.
     */
    private void createServiceCollection() {
        Http.request("box/mkcol-service.txt")
                .with("cellPath", "testcell1")
                .with("path", "servicecol")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * ServiceCollectionの削除.
     */
    private void deleteServiceCollection() {
        Http.request("box/delete-col.txt")
                .with("cellPath", "testcell1")
                .with("box", "box1")
                .with("path", "servicecol")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * WebDavCollectionの作成.
     */
    private void createDavCollection() {
        Http.request("box/mkcol-normal.txt")
                .with("cellPath", "testcell1")
                .with("path", "davcol")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * WebDavCollectionの削除.
     */
    private void deleteDavCollection() {
        Http.request("box/delete-col.txt")
                .with("cellPath", "testcell1")
                .with("box", "box1")
                .with("path", "davcol")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ODataCollectionの作成.
     */
    private void createODataCollection() {
        Http.request("box/mkcol-odata.txt")
                .with("cellPath", "testcell1")
                .with("boxPath", "box1")
                .with("path", ODATA_COL)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * ODataCollectionの削除.
     */
    private void deleteODataCollection() {
        Http.request("box/delete-col.txt")
                .with("cellPath", "testcell1")
                .with("box", "box1")
                .with("path", ODATA_COL)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * XHR2ヘッダーが存在していることを確認する.
     * @params response レスポンス情報
     * @params allowMethod 許可メソッド
     */
    private void checkXHR2Header(TResponse response, String allowMethod) {
        response.checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN)
                .checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethod)
                .checkHeader(HttpHeaders.ALLOW, allowMethod)
                .checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Accept");
    }

    /**
     * XHR2ヘッダーがORIGINのみ存在していることを確認する.
     * @params response レスポンス情報
     */
    private void checkXHR2HeaderOnlyOrigin(TResponse response) {
        response.checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN)
                .checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, null)
                .checkHeader(HttpHeaders.ALLOW, null)
                .checkHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, null);
    }

    /**
     * X-Personium-Versionヘッダーが存在していることを確認する.
     * @params response レスポンス情報
     * @params allowMethod 許可メソッド
     */
    private void checkDcVersionHeader(TResponse response) {
        if (PersoniumUnitConfig.getCoreVersion().compareTo("1.1.0") >= 0) {
            response.checkHeader(HttpHeaders.X_PERSONIUM_VERSION, PersoniumUnitConfig.getCoreVersion());
        } else {
            // Versionが1.1.0より古い場合は、X-Personium-Versionヘッダーを返さない
            response.checkHeader(HttpHeaders.X_PERSONIUM_VERSION, null);
        }
    }
}
