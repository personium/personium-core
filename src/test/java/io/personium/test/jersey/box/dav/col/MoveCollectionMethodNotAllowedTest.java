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
package io.personium.test.jersey.box.dav.col;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.UrlUtils;

/**
 * MOVEメソッドに対応していないリソースへのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionMethodNotAllowedTest extends PersoniumTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";

    /**
     * コンストラクタ.
     */
    public MoveCollectionMethodNotAllowedTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ユーザスキーマのMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザスキーマのMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "$metadata");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * ユーザスキーマメタデータのMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザスキーマメタデータのMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "$metadata/$metadata");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * エンティティタイプ一覧のMOVEで405エラーとなること.
     */
    @Test
    public final void エンティティタイプ一覧のMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "$metadata", EntityType.EDM_TYPE_NAME);
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * エンティティタイプのMOVEで405エラーとなること.
     */
    @Test
    public final void エンティティタイプのMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "$metadata",
                EntityType.EDM_TYPE_NAME + "('SalesDetail')");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * ユーザOData一覧のMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザOData一覧のMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "SalesDetail");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * ユーザODataのMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザODataのMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "SalesDetail('userdata001')");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * ユーザODataの$batchに対するMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザODataの$batchに対するMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "$batch");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * NP経由ユーザOData一覧のMOVEで405エラーとなること.
     */
    @Test
    public final void NP経由ユーザOData一覧のMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "SalesDetail('userdata001')/_Sales");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * ユーザODataの$links一覧のMOVEで405エラーとなること.
     */
    @Test
    public final void ユーザODataの$links一覧のMOVEで405エラーとなること() {
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        // 移動
        String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "SalesDetail('userdata001')/$links/_Sales");
        PersoniumRequest req = PersoniumRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        PersoniumResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * serviceコレクション配下のリソースのMOVEで405エラーとなること.
     */
    @Test
    public final void serviceコレクション配下のリソースのMOVEで405エラーとなること() {
        final String srcCol = "srcColName";
        final String destColName = "destColName";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcCol);

            // 移動
            // このリソースはサービス実行用であるが、MOVEメソッドは実行対象外としているため405が返却される。
            String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol, "dummyResource");
            PersoniumRequest req = PersoniumRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
            PersoniumCoreException expectedException = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }
}
