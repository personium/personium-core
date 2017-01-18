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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
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
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * Basic認証のUnitレベルのリソースに対するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BasicAuthUnitLevelTest extends JerseyTest {

    private String cellName = Setup.TEST_CELL_BASIC;
    private String userName = "account4";
    private String password = "password4";
    private String token = "Basic "
            + Base64.encodeBase64String(String.format(("%s:%s"), userName, password).getBytes());

    /**
     * コンストラクタ.
     */
    public BasicAuthUnitLevelTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Basic認証ーUnitレベルAPIの操作.
     */
    @Test
    public final void Basic認証ーUnitレベルAPIの操作() {
        String testCellName = "BasicAuthResourceTestCell";
        try {
            // Cell
            // 作成
            TResponse res = CellUtils.createWithAnyAuthSchema(testCellName, token, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForUnitLevel(res);

            // 取得
            res = CellUtils.getWithAnyAuthSchema(cellName, token, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForUnitLevel(res);

            // 一覧取得
            res = CellUtils.listWithAnyAuthSchema(token, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForUnitLevel(res);

            // 更新
            res = CellUtils.updateWithAnyAuthSchema(cellName, testCellName, token, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForUnitLevel(res);

            // 削除
            res = CellUtils.deleteWithAnyAuthSchema(token, cellName, HttpStatus.SC_UNAUTHORIZED);
            checkAuthenticateHeaderForUnitLevel(res);

        } finally {
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName, -1);
        }
    }

    /**
     * WWW-Authenticateヘッダが正しいことのチェック.
     * @param res レスポンス
     */
    private void checkAuthenticateHeaderForUnitLevel(TResponse res) {
        // WWW-Authenticateヘッダチェック
        String expected = String.format("Bearer realm=\"%s\"", UrlUtils.getBaseUrl() + "/");
        List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(1, headers.size());
        assertThat(headers).contains(expected);
    }
}
