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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListFilterFunctionTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListFilterFunctionTest() {
        super();
    }

    /**
     * UserDataに前方一致検索クエリに英語キーワードを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに英語キーワードを指定して対象のデータのみ取得できること() {
        // englishプロパティの先頭が大文字Testの値のみ取得する
        String sdEntityTypeName = "SalesDetail";
        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28english%2c%27Test%27%29&$inlinecount=allpages");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata101", additionalprop);
        additionalprop.put("__id", "userdata101");
        additionalprop.put("test", "btest");
        additionalprop.put("japanese", "部分一致検索漢字のテスト");
        additionalprop.put("english", "Test Substringof Search value");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, null);

        // englishプロパティの先頭が小文字testの値のみ取得する
        response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28english%2c%27test%27%29&$inlinecount=allpages");

        // レスポンスボディーのチェック
        // URI
        uri = new HashMap<String, String>();
        uri.put("userdata102", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));

        // プロパティ
        additional = new HashMap<String, Map<String, Object>>();
        additionalprop = new HashMap<String, Object>();
        additional.put("userdata102", additionalprop);
        additionalprop.put("__id", "userdata102");
        additionalprop.put("test", "ctest");
        additionalprop.put("japanese", "部分一致けんさくのテスト");
        additionalprop.put("english", "test substringof search");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, null);
    }

    /**
     * UserDataに前方一致検索クエリに英語フレーズを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに英語フレーズを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28english%2c%27Search+substr%27%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata100", additionalprop);
        additionalprop.put("__id", "userdata100");
        additionalprop.put("test", "atest");
        additionalprop.put("japanese", "部分一致検索テスト");
        additionalprop.put("english", "Search substringof Test");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに前方一致検索クエリに前方に存在しない英語を指定して0件取得できること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに前方に存在しない英語を指定して0件取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28english%2c%27substringof%27%29&$inlinecount=allpages");

        // レスポンスボディーのチェック
        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null, "__id", 0, null);
    }

    /**
     * UserDataに前方一致検索クエリに日本語フレーズを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに日本語フレーズを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28japanese%2c%27部分一致検索漢字の%27%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata101", additionalprop);
        additionalprop.put("__id", "userdata101");
        additionalprop.put("test", "btest");
        additionalprop.put("japanese", "部分一致検索漢字のテスト");
        additionalprop.put("english", "Test Substringof Search value");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに前方一致検索クエリに前方に存在しない日本語を指定して0件取得できること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに前方に存在しない日本語を指定して0件取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=startswith%28japanese%2c%27一致検索%27%29&$inlinecount=allpages");

        // レスポンスボディーのチェック
        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null, "__id", 0, null);
    }

    /**
     * UserDataに前方一致検索クエリに検索対象のKeyのみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに検索対象のKeyのみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28test%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリにKey値にシングルクオートを指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリにKey値にシングルクオートを指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28%27test%27%29%2c%27test%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリにvalueにシングルクオートでくくらずに指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリにvalueにシングルクオートでくくらずに指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28test%2ctest%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリに整数値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに整数値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28number%2c1%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリに小数値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに小数値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28decimal%2c1.1%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリにnullを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリにnullを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28null%2cnull%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリに真偽値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに真偽値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        // Edm.Stringの検索条件：文字列： "string data"
        String query = "?\\$filter=startswith%28english%2ctrue%29&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(cellName, boxName, colName, sdEntityTypeName, query,
                DcCoreConfig.getMasterToken(), HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("english").getMessage());
    }

    /**
     * UserDataに前方一致検索クエリ引数を指定しなかった場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリ引数を指定しなかった場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに英語キーワードを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに英語キーワードを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=substringof%28%27value%27%2cenglish%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata101", additionalprop);
        additionalprop.put("__id", "userdata101");
        additionalprop.put("test", "btest");
        additionalprop.put("japanese", "部分一致検索漢字のテスト");
        additionalprop.put("english", "Test Substringof Search value");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに部分一致検索クエリに英語フレーズを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに英語フレーズを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=substringof%28%27Search+substringof%27%2cenglish%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata100", additionalprop);
        additionalprop.put("__id", "userdata100");
        additionalprop.put("test", "atest");
        additionalprop.put("japanese", "部分一致検索テスト");
        additionalprop.put("english", "Search substringof Test");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに部分一致検索クエリに日本語フレーズを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに日本語フレーズを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=substringof%28%27部分一致検索漢字%27%2cjapanese%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata101", additionalprop);
        additionalprop.put("__id", "userdata101");
        additionalprop.put("test", "btest");
        additionalprop.put("japanese", "部分一致検索漢字のテスト");
        additionalprop.put("english", "Test Substringof Search value");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに部分一致検索クエリに日本語キーワードを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに日本語キーワードを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        DcResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=substringof%28%27けんさく%27%2cjapanese%29");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata102", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata102", additionalprop);
        additionalprop.put("__id", "userdata102");
        additionalprop.put("test", "ctest");
        additionalprop.put("japanese", "部分一致けんさくのテスト");
        additionalprop.put("english", "test substringof search");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに部分一致検索クエリにTargetのみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに検索対象のKeyのみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28test%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリにValueのみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに検索対象のValueのみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28%27test%27%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリにKeyにシングルクオートでくくらずに指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリにKeyにシングルクオートでくくらずに指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28test%2ctest%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに整数値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに整数値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%281%2cnumber%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに小数値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに小数値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%281.1%2cdecimal%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリにnullを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリにnullを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28null%2csample%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに真偽値を指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに真偽値を指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        // Edm.Stringの検索条件：文字列： "string data"
        String query = "?\\$filter=substringof%28true%2cenglish%29&\\$inlinecount=allpages";
        TResponse res = UserDataUtils.list(cellName, boxName, colName, sdEntityTypeName, query,
                DcCoreConfig.getMasterToken(), HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(),
                DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("english").getMessage());
    }

}
