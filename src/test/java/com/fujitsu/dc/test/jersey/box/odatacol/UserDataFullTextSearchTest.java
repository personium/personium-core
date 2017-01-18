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
package com.fujitsu.dc.test.jersey.box.odatacol;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * UserData全文検索のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataFullTextSearchTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataFullTextSearchTest() {
        super();
    }

    /**
     * 全文検索クエリを指定して1件のみヒットした場合の一覧が正しいこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリを指定して1件のみヒットした場合の一覧が正しいこと() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち");
            body.put("name_en", "pochi");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま");
            body.put("name_en", "tama");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=たま");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata101");
            additionalprop.put("name_ja", "たま");
            additionalprop.put("name_en", "tama");
            additionalprop.put("number", 1);
            additional.put("userdata101", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリを指定して全件ヒットした場合の一覧が正しいこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリを指定して全件ヒットした場合の一覧が正しいこと() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち１号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち２号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=ぽち");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));
            uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata101')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち１号");
            additionalprop.put("name_en", "pochi1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata101");
            additionalprop.put("name_ja", "ぽち２号");
            additionalprop.put("name_en", "pochi2nd");
            additionalprop.put("number", 1);
            additional.put("userdata101", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリを指定してヒットしない場合に空の一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリを指定してヒットしない場合に空の一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち");
            body.put("name_en", "pochi");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま");
            body.put("name_en", "tama");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=はち");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで日本語を指定して完全一致するデータの一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで日本語を指定して完全一致するデータの一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち１号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち２号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=ぽち１号");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち１号");
            additionalprop.put("name_en", "pochi1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで日本語を指定して部分一致するデータの一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで日本語を指定して部分一致するデータの一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち１号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち２号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=１号");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち１号");
            additionalprop.put("name_en", "pochi1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角英数字を指定して完全一致するデータの一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角英数字を指定して完全一致するデータの一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち１号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち２号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi1st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち１号");
            additionalprop.put("name_en", "pochi1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角英小文字で登録したデータを半角英大文字で検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角英小文字で登録したデータを半角英大文字で検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち");
            body.put("name_en", "pochi");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま");
            body.put("name_en", "tama");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=POCHI");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち");
            additionalprop.put("name_en", "pochi");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角英大文字で登録したデータを半角英小文字で検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角英大文字で登録したデータを半角英小文字で検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち");
            body.put("name_en", "POCHI");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま");
            body.put("name_en", "TAMA");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち");
            additionalprop.put("name_en", "POCHI");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角英数字を指定して部分一致するデータはヒットしないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角英数字を指定して部分一致するデータはヒットしないこと() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち１号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち２号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角空白で区切られた登録データを完全一致で検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角空白で区切られた登録データを完全一致で検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち １号");
            body.put("name_en", "pochi 1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち ２号");
            body.put("name_en", "pochi 2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi%201st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち １号");
            additionalprop.put("name_en", "pochi 1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角空白で区切られた登録データをキーワード検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角空白で区切られた登録データをキーワード検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち １号");
            body.put("name_en", "pochi 1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち ２号");
            body.put("name_en", "pochi 2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));
            uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata101')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち １号");
            additionalprop.put("name_en", "pochi 1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata101");
            additionalprop.put("name_ja", "ぽち ２号");
            additionalprop.put("name_en", "pochi 2nd");
            additionalprop.put("number", 1);
            additional.put("userdata101", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 複数キーワードで検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 複数キーワードで検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "骨が好きなぽち１号");
            body.put("name_en", "pochi 1st favorite bone");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "肉が好きなぽち２号");
            body.put("name_en", "pochi 2nd favorite meat");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=bone%201st%20pochi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "骨が好きなぽち１号");
            additionalprop.put("name_en", "pochi 1st favorite bone");
            additionalprop.put("number", 1);
            additional.put("userdata101", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");

            response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=好き%20１号%20ぽち");
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角空白で区切られた登録データを半角記号で区切った検索クエリでキーワード検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角空白で区切られた登録データを半角記号で区切った検索クエリでキーワード検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち １号");
            body.put("name_en", "pochi 1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち ２号");
            body.put("name_en", "pochi 2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi%261st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち １号");
            additionalprop.put("name_en", "pochi 1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリでダブルクォーテーションなしのand検索ができること.
     * 登録データ:「ポチたま」、「たま ポチ」 検索ワード：「ポチ%20たま」
     * 検索した結果、2件ヒットすること確認する。
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリでダブルクォーテーションなしのand検索ができること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ポチたま");
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま ポチ");
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=ポチ%20たま");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));
            uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata101')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;
            Map<String, Object> additionalprop2 = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ポチたま");
            additional.put("userdata100", additionalprop);
            additionalprop2 = new HashMap<String, Object>();
            additionalprop2.put("__id", "userdata101");
            additionalprop2.put("name_ja", "たま ポチ");
            additional.put("userdata101", additionalprop2);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリでダブルクォーテーション付きのand検索ができること.
     * 登録データ:「ポチたま」、「たま ポチ」 検索ワード：「%22ポチ%20たま%22」
     * 検索した結果、2件ヒットすることを確認する。
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリでダブルクォーテーション付きのand検索ができること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ポチたま");
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "たま ポチ");
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=%22ポチ%20たま%22");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));
            uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata101')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;
            Map<String, Object> additionalprop2 = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ポチたま");
            additional.put("userdata100", additionalprop);
            additionalprop2 = new HashMap<String, Object>();
            additionalprop2.put("__id", "userdata101");
            additionalprop2.put("name_ja", "たま ポチ");
            additional.put("userdata101", additionalprop2);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角記号で区切られた登録データを完全一致で検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角記号で区切られた登録データを完全一致で検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち&１号");
            body.put("name_en", "pochi&1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち&２号");
            body.put("name_en", "pochi&2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi%261st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち&１号");
            additionalprop.put("name_en", "pochi&1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角記号で区切られた登録データをキーワード検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角記号で区切られた登録データをキーワード検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち&１号");
            body.put("name_en", "pochi&1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち&２号");
            body.put("name_en", "pochi&2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));
            uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata101')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち&１号");
            additionalprop.put("name_en", "pochi&1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata101");
            additionalprop.put("name_ja", "ぽち&２号");
            additionalprop.put("name_en", "pochi&2nd");
            additionalprop.put("number", 1);
            additional.put("userdata101", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 半角記号で区切られた登録データを半角空白で区切った検索クエリでキーワード検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 半角記号で区切られた登録データを半角空白で区切った検索クエリでキーワード検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "ぽち&１号");
            body.put("name_en", "pochi&1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "ぽち&２号");
            body.put("name_en", "pochi&2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi%201st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "ぽち&１号");
            additionalprop.put("name_en", "pochi&1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで全角と半角を指定して部分一致するデータの一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで全角と半角を指定して部分一致するデータの一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽちichi号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽちni号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=ぽちichi");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "富士山ぽちichi号");
            additionalprop.put("name_en", "pochi1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで値を指定しない場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで値を指定しない場合400エラーとなること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽちichi号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽちni号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=");

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                    DcCoreException.OData.QUERY_INVALID_ERROR.params("q", "").getMessage());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角で256バイトを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角で256バイトを指定した場合400エラーとなること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽちichi号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽちni号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            String qValue = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "1234567890123456";

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=" + qValue);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                    DcCoreException.OData.QUERY_INVALID_ERROR.params("q", qValue).getMessage());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角255バイトを指定して検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角255バイトを指定して検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽちichi号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽちni号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            String qValue = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "123456789012345";

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=" + qValue);

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで全角255バイトを指定して検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで全角255バイトを指定して検索できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽちichi号");
            body.put("name_en", "pochi1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽちni号");
            body.put("name_en", "pochi2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            String qValue = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "123456789012あ";

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=" + qValue);

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角アンダーバーを指定してヒットしないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角アンダーバーを指定してヒットしないこと() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽち ichi号");
            body.put("name_en", "pochi 1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽち ni号");
            body.put("name_en", "pochi 2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi_1st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

    /**
     * 全文検索クエリで半角記号を指定して完全一致するデータの一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 全文検索クエリで半角記号を指定して完全一致するデータの一覧が取得できること() {
        JSONObject body;
        String userDataId1 = "userdata100";
        String userDataId2 = "userdata101";

        try {
            // 部分一致検索用データの登録
            body = new JSONObject();
            body.put("__id", userDataId1);
            body.put("name_ja", "富士山ぽち ichi号");
            body.put("name_en", "pochi                   1st");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            body = new JSONObject();
            body.put("__id", userDataId2);
            body.put("name_ja", "富士山ぽち ni号");
            body.put("name_en", "pochi                    2nd");
            body.put("number", 1);
            this.createUserDataWithDcClient(cellName, boxName, colName, entityTypeName, body);

            // 半角空白&+\,"'%?;:~!*@$=() のテスト
            DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, entityTypeName,
                    "?q=pochi%20%26%2B%5C%2C%22%27%25%3F%3B%3A%7E%21%2A%40%24%3D%28%291st");

            // レスポンスボディーのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('userdata100')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = null;

            additionalprop = new HashMap<String, Object>();
            additionalprop.put("__id", "userdata100");
            additionalprop.put("name_ja", "富士山ぽち ichi号");
            additionalprop.put("name_en", "pochi                   1st");
            additionalprop.put("number", 1);
            additional.put("userdata100", additionalprop);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
        }
    }

}
