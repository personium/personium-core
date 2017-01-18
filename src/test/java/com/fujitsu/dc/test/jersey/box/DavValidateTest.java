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
package com.fujitsu.dc.test.jersey.box;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.Http;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * MKCOLのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class DavValidateTest extends JerseyTest {
    ArrayList<String> validNames;
    private static final String CELL_NAME = "testcell1";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public DavValidateTest() {
        super("com.fujitsu.dc.core.rs");
        validNames = new ArrayList<String>();
        //validNames.add("a%5c"); // %5C は リクエストを投げる事ができなかったため、手動でのみ確認
        validNames.add("a%20");
        validNames.add("a%2F");
        validNames.add("a%3A");
        validNames.add("a%2A");
        validNames.add("a%3F");
        validNames.add("a%22");
        validNames.add("a%3C");
        validNames.add("a%3E");
        validNames.add("a%7C");
        String name;
        name =  "1234567890123456789012345678901234567890123456789012345678901234567890";
        name += "1234567890123456789012345678901234567890123456789012345678901234567890";
        name += "1234567890123456789012345678901234567890123456789012345678901234567890";
        name += "1234567890123456789012345678901234567890123456";
        validNames.add(name);
    }

    /**
     * ファイル名のバリデートチェック.
     */
    @Test
    public final void ファイル名のバリデートチェック() {
        String name;
        for (int i = 0; i < validNames.size(); i++) {
            name = validNames.get(i);
            final Http theReq = this.putFileRequest(name, FILE_BODY, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_FORBIDDEN);
        }
    }

    /**
     * コレクション名のバリデートチェック.
     */
    @Test
    public final void コレクション名のバリデートチェック() {
        String name;
        for (int i = 0; i < validNames.size(); i++) {
            name = validNames.get(i);
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

