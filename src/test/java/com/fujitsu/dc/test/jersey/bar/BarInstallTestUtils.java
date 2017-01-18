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
package com.fujitsu.dc.test.jersey.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.model.progress.ProgressInfo;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * barファイルインストールテスト用ユーティリティクラス.
 */
public class BarInstallTestUtils {
    private static final int SIZE_KB = 1024;
    private static final int READ_BUFFER_SIZE = 4096 * SIZE_KB;

    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(BarInstallTestUtils.class);

    /**
     * Barインストール対象.
     */
    public static final String INSTALL_TARGET = "installBox";

    /**
     * Barインストール時のタイムアウト(分).
     */
    public static final long BAR_INSTALL_TIMEOUT = 5 * 60 * 1000; // 5分以上かかってしまう場合はエラーとする

    /**
     * Barインストール時の問い合わせ間隔(秒).
     */
    public static final long BAR_INSTALL_SLEEP_TIME = 3 * 1000; // 2秒

    /**
     * constructor.
     */
    private BarInstallTestUtils() {
    }

    /**
     * barファイルを読み込む.
     * @param barFile barファイルのパス
     * @return 読み込んだbarファイル
     */
    public static byte[] readBarFile(File barFile) {
        InputStream is = ClassLoader.getSystemResourceAsStream(barFile.getPath());
        ByteBuffer buff = ByteBuffer.allocate(READ_BUFFER_SIZE);
        log.debug(String.valueOf(buff.capacity()));
        try {
            byte[] bbuf = new byte[SIZE_KB];
            int size;
            while ((size = is.read(bbuf)) != -1) {
                buff.put(bbuf, 0, size);
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to load bar file:" + barFile.getPath(), e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("failed to close bar file:" + barFile.getPath(), e);
            }
        }
        int size = buff.position();
        buff.flip();
        byte[] retv = new byte[size];
        buff.get(retv, 0, size);
        return retv;
    }

    /**
     * barインストール用リクエストを投入する.
     * @param requestFile Barファイルのパス名
     * @param cellName requestCellName
     * @param path リクエストパス
     * @param headers 追加のリクエストヘッダ
     * @param body ボディ
     * @return レスポンス
     */
    public static TResponse request(
            String requestFile,
            String cellName,
            String path,
            Map<String, String> headers,
            byte[] body) {
        return request(AbstractCase.MASTER_TOKEN_NAME, requestFile, cellName, path, headers, body);

    }

    /**
     * barインストール用リクエストを投入する.
     * @param token アクセストークン
     * @param requestFile Barファイルのパス名
     * @param cellName requestCellName
     * @param path リクエストパス
     * @param headers 追加のリクエストヘッダ
     * @param body ボディ
     * @return レスポンス
     */
    public static TResponse request(
            String token,
            String requestFile,
            String cellName,
            String path,
            Map<String, String> headers,
            byte[] body) {

        Http client = Http.request(requestFile)
                .with("cellPath", cellName)
                .with("path", path)
                .with("token", "Bearer " + token);

        String contType = headers.get(HttpHeaders.CONTENT_TYPE);
        if (contType != null) {
            client = client.with("contType", contType);
        }
        String contLength = headers.get(HttpHeaders.CONTENT_LENGTH);
        if (contLength != null) {
            client = client.with("contLength", contLength);
        }
        String overrideMethod = headers.get("X-HTTP-Method-Override");
        if (overrideMethod != null) {
            client = client.with("method", overrideMethod);
        }
        TResponse res = client.setBodyBinary(body)
                .returns()
                .debug();

        return res;
    }

    /**
     * ボディのリストを取得する.
     * @param body ボディ文字列
     * @return リスト化されたボディ
     */
    public static List<String[]> getListedBody(String body) {
        assertNotNull(body);
        List<String[]> list = new ArrayList<String[]>();
        String[] lines = body.split("\n");
        for (String line : lines) {
            String[] fields = line.replaceAll("\"", "").split(",");
            list.add(fields);
        }
        return list;
    }

    /**
     * Collection DELETEの実行.
     * @param colName コレクション名
     */
    public static void deleteCollection(final String colName) {
        // Boxの削除
        Http.request("box/delete-box-col.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("box", INSTALL_TARGET)
                .with("path", colName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(-1)
                .debug();
    }

    /**
     * Collection PROPFINDの実行.
     * @param cell セル名
     * @param box ボックス名
     * @param path 対象のコレクション名
     * @param depth Depthヘッダの値
     * @param token 認証用のトークン
     * @param sc 期待するレスポンスコード
     * @return TResponse PROPFINDの結果
     */
    public static TResponse propfind(final String cell, final String box,
            final String path, final String depth, final String token, final int sc) {
        return Http.request("box/propfind-col.txt")
                .with("path", path)
                .with("depth", depth)
                .with("token", token)
                .with("cell", cell)
                .with("box", box)
                .returns()
                .statusCode(sc);
    }

    /**
     * Collection PROPFINDの実行.
     * @param cell セル名
     * @param box ボックス名
     * @param path 対象のコレクション名
     * @param token 認証用のトークン
     * @param sc 期待するレスポンスコード
     * @return TResponse PROPFINDの結果
     */
    public static TResponse propfind(final String cell, final String box,
            final String path, final String token, final int sc) {
        return propfind(cell, box, path, "1", token, sc);
    }

    /**
     * barファイルインストール（非同期）の完了まで待ってからステータスを検証する.
     * @param location barファイルインストール状況確認APIのURL
     * @param schemaUrl スキーマURL
     * @param status 期待するステータス
     */
    public static void assertBarInstallStatus(String location, String schemaUrl, ProgressInfo.STATUS status) {
        waitBoxInstallCompleted(location);
        DcResponse res = ODataCommon.getOdataResource(location);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        JSONObject bodyJson = (JSONObject) ((JSONObject) res.bodyAsJson());

        assertEquals(status.value(), bodyJson.get("status"));
        assertEquals(schemaUrl, bodyJson.get("schema"));
        if (ProgressInfo.STATUS.FAILED == status) {
            assertNotNull(bodyJson.get("started_at"));
            assertNull(bodyJson.get("installed_at"));
            assertNotNull(bodyJson.get("progress"));
            assertNotNull(bodyJson.get("message"));
            assertNotNull(((JSONObject) bodyJson.get("message")).get("code"));
            assertNotNull(((JSONObject) bodyJson.get("message")).get("message"));
            assertNotNull(((JSONObject) ((JSONObject) bodyJson.get("message")).get("message")).get("value"));
            assertNotNull(((JSONObject) ((JSONObject) bodyJson.get("message")).get("message")).get("lang"));
        } else {
            assertNull(bodyJson.get("started_at"));
            assertNotNull(bodyJson.get("installed_at"));
            assertNull(bodyJson.get("progress"));
        }
    }

    /**
     * Boxインストールの完了を待つ.
     * @param location Location
     */
    public static void waitBoxInstallCompleted(String location) {
        DcResponse response = null;
        JSONObject bodyJson = null;

        long startTime = System.currentTimeMillis();
        while (true) {
            response = ODataCommon.getOdataResource(location);
            if (HttpStatus.SC_OK == response.getStatusCode()) {
                bodyJson = (JSONObject) ((JSONObject) response.bodyAsJson());
                if (!ProgressInfo.STATUS.PROCESSING.value().equals(bodyJson.get("status"))) {
                    return;
                }
                assertNull(bodyJson.get("installed_at"));
                assertNotNull(bodyJson.get("progress"));
                assertNotNull(bodyJson.get("started_at"));
            }
            if (System.currentTimeMillis() - startTime > BAR_INSTALL_TIMEOUT) {
                fail("Failed to bar file install: takes too much time. [" + BAR_INSTALL_TIMEOUT + "millis]");
            }
            try {
                Thread.sleep(BAR_INSTALL_SLEEP_TIME);
            } catch (InterruptedException e) {
                log.info("Interrupted: " + e.getMessage());
            }
        }

    }

    /**
     * Boxインストールの進行状況を取得する.
     * @param cellName セル名
     * @param path 進行状況を取得するBox名
     * @param token 認証情報(認証スキーマ付き)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse getProgress(final String cellName, final String path, final String token, final int code) {
        // Boxインストールの進行状況を取得する
        return Http.request("bar-install-get-progress.txt")
                .with("cellPath", cellName)
                .with("path", path)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
    }

}
