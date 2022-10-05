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
package io.personium.test.jersey.box;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.Http;

/**
 * MKCOLのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class DavValidateTest extends PersoniumTest {
    ArrayList<String> invalidNames;
    private static final String CELL_NAME = "testcell1";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public DavValidateTest() {
        super(new PersoniumCoreApplication());
        invalidNames = new ArrayList<String>();
        invalidNames.add("a%5c");
        invalidNames.add("a%20");
        invalidNames.add("a%2F");
        invalidNames.add("a%3A");
        invalidNames.add("a%2A");
        invalidNames.add("a%3F");
        invalidNames.add("a%22");
        invalidNames.add("a%3C");
        invalidNames.add("a%3E");
        invalidNames.add("a%7C");
        invalidNames.add(StringUtils.repeat("12345678", 32)); // 256
    }

    /**
     * ファイル名のバリデートチェック.
     */
    @Test
    public final void ファイル名のバリデートチェック() {
        for (var name : invalidNames) {
            final Http theReq = this.putFileRequest(name, FILE_BODY, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_FORBIDDEN);
        }
    }

    /**
     * コレクション名のバリデートチェック.
     */
    @Test
    public final void コレクション名のバリデートチェック() {
        for (var name : invalidNames) {
            final Http theReq = this.mkColRequest(name);
            theReq.returns().statusCode(HttpStatus.SC_FORBIDDEN);
        }
    }

    /**
     * File作成リクエストを生成.
     * @param boxName box名
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http putFileRequest(String fileName, String fileBody, String etag, String boxName) {
        return Http.request("box/dav-put.txt")
                .with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", fileName)
                .with("box", boxName)
                .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                .with("source", fileBody);
    }

    /**
     * MKCOLリクエストを生成.
     * @param path 生成するパス
     * @return リクエストオブジェクト
     */
    Http mkColRequest(String path) {
        return Http.request("box/mkcol-normal.txt")
                .with("cellPath", CELL_NAME)
                .with("path", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME);
    }
}

