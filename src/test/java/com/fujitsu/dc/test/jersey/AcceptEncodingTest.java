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
package com.fujitsu.dc.test.jersey;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Accept-Encodingヘッダのテスト.
 * Accept-Encodingヘッダを処理するミドルウェアが存在しない場合はテストが失敗する。
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AcceptEncodingTest extends AbstractCase {

    /**
     *  コンストラクタ.
     */
    public AcceptEncodingTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Accept-Encodingヘッダにgzipを指定して、レスポンスボディがgzipで返却されること.
     */
    @Test
    public final void Accept_Encodingヘッダにgzipを指定した場合にレスポンスボディがgzipで返却されること() {
        String locationUrlGet = UrlUtils.unitCtl(Cell.EDM_TYPE_NAME);
        DcRequest req = DcRequest.get(locationUrlGet + "?$inlinecount=allpages");
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.ACCEPT_ENCODING, "gzip");
        DcResponse res = AbstractCase.request(req);

        // GZip 圧縮されていることをレスポンスヘッダから確認する
        String contentEncodingHeader = res.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
        assertEquals("gzip", contentEncodingHeader);
    }
}
