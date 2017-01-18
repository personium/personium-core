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
package com.fujitsu.dc.test.jersey.box.dav.col;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * MOVEメソッドに対応していないリソースへのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionMethodNotAllowedTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";

    /**
     * コンストラクタ.
     */
    public MoveCollectionMethodNotAllowedTest() {
        super("com.fujitsu.dc.core.rs");
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
        DcRequest req = DcRequest.move(srcUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.DESTINATION, destUrl);
        req.header(HttpHeaders.OVERWRITE, "F");
        DcResponse response = AbstractCase.request(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
        DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
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
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
            DcCoreException expectedException = DcCoreException.Misc.METHOD_NOT_ALLOWED;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }
}
