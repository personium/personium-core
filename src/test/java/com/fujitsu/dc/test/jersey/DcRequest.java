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

import java.util.HashMap;

import javax.ws.rs.HttpMethod;

import org.json.simple.JSONObject;

/**
 * RESTテスト用のリクエストクラス.
 */
public class DcRequest {
    /** Requestヘッダのハッシュマップ. */
    private HashMap<String, String> headers;
    /** リクエスト先のURL. */
    private String urlString = "";
    /** Object形式のリクエストボディ. */
    private Object body = null;

    /** リクエストボディタイプ. */
    private enum BodyType {
        JSON, ATOM, STRING
    };

    /** リクエストボディタイプ値. */
    private BodyType bodyType = null;

    /** リクエストメソッド. */
    private String method = null;
    /** Cell ID. */
    private String cellID = "";
    /** Query文字列. */
    private String queryStr = "";

    /**
     * コンストラクタ.
     * @param url URL
     */
    public DcRequest(String url) {
        this.headers = new HashMap<String, String>();
        this.urlString = url;
    }

    /**
     * GETメソッドとしてDcRequestオブジェクトを生成する.
     * @param url URL
     * @return req DcRequestオブジェクト
     */
    public static DcRequest get(String url) {
        DcRequest req = new DcRequest(url);
        req.method = HttpMethod.GET;
        return req;
    }

    /**
     * PUTメソッドとしてRequestオブジェクトを生成する.
     * @param url URL
     * @return req DcRequestオブジェクト
     */
    public static DcRequest put(String url) {
        DcRequest req = new DcRequest(url);
        req.method = HttpMethod.PUT;
        return req;
    }

    /**
     * POSTメソッドとしてRequestオブジェクトを生成する.
     * @param url URL
     * @return req DcRequestオブジェクト
     */
    public static DcRequest post(String url) {
        DcRequest req = new DcRequest(url);
        req.method = HttpMethod.POST;
        return req;
    }

    /**
     * DELETEメソッドとしてDcRequestオブジェクトを生成する.
     * @param url URL
     * @return req DcRequestオブジェクト
     */
    public static DcRequest delete(String url) {
        DcRequest req = new DcRequest(url);
        req.method = HttpMethod.DELETE;
        return req;
    }

    /**
     * MOVEメソッドとしてRequestオブジェクトを生成する.
     * @param url URL
     * @return req DcRequestオブジェクト
     */
    public static DcRequest move(String url) {
        DcRequest req = new DcRequest(url);
        req.method = com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MOVE;
        return req;
    }

    /**
     * MKCOLメソッドとしてDcRequestオブジェクトを生成する.
     * @param inMethod HTTP method
     * @return DcRequestオブジェクト
     */
    public DcRequest method(String inMethod) {
        this.method = inMethod;
        return this;
    }

    /**
     * セルIDの取得.
     * @return セルID文字列
     */
    public String getCellId() {
        return this.cellID;
    }

    /**
     * URL文字列の取得.
     * @return URL文字列
     */
    public String getUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.urlString);

        if (!this.queryStr.isEmpty()) {
            sb.append("?");
            sb.append(this.queryStr);
        }

        return sb.toString();
    }

    /**
     * Requestヘッダのハッシュマップを取得する.
     * @return HashMapオブジェクト
     */
    public HashMap<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * JSON文字列を取得する.
     * @return 文字列化したJSON文字列
     */
    public String getBody() {
        if (this.bodyType == BodyType.JSON) {
            return getJsonBody();
            // TODO XML形式のボディを返却
            // } else if (bodyType == BodyType.ATOM) {
        } else if (this.bodyType == BodyType.STRING) {
            return (String) this.body;
        }
        return "";
    }

    /**
     * JSON文字列を取得する.
     * @return 文字列化したJSON文字列
     */
    public String getJsonBody() {
        return ((JSONObject) this.body).toJSONString();
    }

    /**
     * メソッド名を取得する.
     * @return メソッド名
     */
    public String getMethod() {
        return this.method;
    }

    /**
     * 指定したヘッダの値を取得する.
     * @param name ヘッダ名
     * @return ヘッダ値
     */
    public String getHeader(final String name) {
        return this.headers.get(name);
    }

    /**
     * Cell指定.
     * @param value Cell ID値
     * @return DcRequestオブジェクト
     */
    public DcRequest cell(final String value) {
        this.cellID = value;
        return this;
    }

    /**
     * query文字列設定.
     * @param value Query文字列
     * @return DcRequestオブジェクト
     */
    public DcRequest query(final String value) {
        this.queryStr = value;
        return this;
    }

    /**
     * Cell関連のURLを生成する.
     * @return 作成したCellのURL文字列
     */
    public String makeCellUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append("/__ctl/Cell");
        if (!this.cellID.isEmpty()) {
            sb.append("('");
            sb.append(this.cellID);
            sb.append("')");
        }
        return sb.toString();
    }

    /**
     * URLを指定する.
     * @param url URL文字列
     * @return DcRequestオブジェクト
     */
    public static DcRequest create(final String url) {
        return new DcRequest(url);
    }

    /**
     * リクエストヘッダを追加する.
     * @param key ヘッダ名
     * @param value 値
     * @return DcRequestオブジェクト
     */
    public DcRequest header(final String key, final String value) {
        if (value != null) {
            this.headers.put(key, value);
        }
        return this;
    }

    /**
     * リクエストボディ用のJSONにフィールドを追加する.
     * @param key フィールド名
     * @param value 値
     * @return DcRequestオブジェクト
     */
    @SuppressWarnings("unchecked")
    public DcRequest addJsonBody(final String key, final Object value) {
        if (this.body == null) {
            this.body = new JSONObject();
        }
        ((JSONObject) this.body).put(key, value);
        this.bodyType = BodyType.JSON;
        return this;
    }

    /**
     * リクエストボディ用のJSONにフィールドを複数追加する.
     * @param key フィールド名配列
     * @param value 値配列
     * @return DcRequestオブジェクト
     */
    @SuppressWarnings("unchecked")
    public DcRequest addJsonBody(final String[] key, final String[] value) {
        if (this.body == null) {
            this.body = new JSONObject();
        }
        for (int i = 0; i < key.length; i++) {
            ((JSONObject) this.body).put(key[i], value[i]);
        }
        this.bodyType = BodyType.JSON;
        return this;
    }

    /**
     * リクエストボディにStringを追加する.
     * @param value 値
     * @return DcRequestオブジェクト
     */
    public DcRequest addStringBody(final String value) {
        if (this.body == null) {
            this.body = new String();
        }
        this.body = value;
        this.bodyType = BodyType.STRING;
        return this;
    }
}
