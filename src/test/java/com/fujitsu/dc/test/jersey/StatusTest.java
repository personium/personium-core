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

import static org.junit.Assert.assertNotNull;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * __statusのテスト.
 */
public class StatusTest extends ODataCommon {
    /** ログ用オブジェクト. */
    //private static Logger log = LoggerFactory.getLogger(StatusRequest.class);

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public StatusTest() {
        super("com.fujitsu.dc");
    }

    /**
     * __status リクエストのテスト.
     */
    @Test
    public void ステータス確認のテスト() {
        DcRequest req = DcRequest.get(UrlUtils.status());
        DcResponse res = request(req);
        JSONObject json = res.bodyAsJson();

        assertNotNull(json.get("ElasticSearch"));
        assertNotNull(json.get("properties"));
        assertNotNull(json.get("ads"));
    }
}
