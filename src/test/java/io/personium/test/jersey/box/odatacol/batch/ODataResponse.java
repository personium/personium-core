/**
 * Personium
 * Copyright 2014-2021 - 2017 Personium Project Authors
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
package io.personium.test.jersey.box.odatacol.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.personium.test.jersey.DaoException;

// This class has been copied from personium-client for testing.
/**
 * It creates a new object of ODataResponse. This class represents Response object.
 */
public class ODataResponse {
    // /** レスポンスヘッダの生文字列. */
    /** Raw string of the response header. */
    String rawHeaders = "";
    // /** １つ１つのヘッダに分解したレスポンスヘッダのハッシュマップ. */
    /** Hash map of the response header decomposed into one header one. */
    HashMap<String, String> headers;
    // /** ステータスコード. */
    /** Status code. */
    int statusCode;
    // /** レスポンスボディ. */
    /** Response body. */
    String body = "";

    // /**
    // * コンストラクタ.
    // * @param header ヘッダー文字列
    // * @param body ボディ文字列
    // */
    /**
     * This is the parameterized constructor initializing its class variables.
     * @param header Header string
     * @param body Body string
     */
    public ODataResponse(String header, String body) {
        this.rawHeaders = header;
        this.body = body;
        this.headers = this.parseHeaders(header);
    }

    // /**
    // * ステータスコードの取得.
    // * @return ステータスコード
    // */
    /**
     * This method returns the status code.
     * @return Status Code value
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    // /**
    // * 解析前のヘッダー文字列を取得.
    // * @return ヘッダー文字列
    // */
    /**
     * This method gets the header string before the analysis.
     * @return Header string
     */
    public String getRawHeaders() {
        return this.rawHeaders;
    }

    // /**
    // * レスポンスヘッダのハッシュマップを取得.
    // * @return ヘッダのハッシュマップ
    // */
    /**
     * This method gets the hash map of the response header.
     * @return HashMap Header
     */
    public HashMap<String, String> getHeaders() {
        return this.headers;
    }

    // /**
    // * 指定したレスポンスヘッダの値を取得する.
    // * @param key ヘッダのキー
    // * @return 指定したキーの値
    // */
    /**
     * This method gets the value of a response header that is specified.
     * @param key Header Key
     * @return Value of the key
     */
    public String getHeader(final String key) {
        return headers.get(key);
    }

    // /**
    // * レスポンスボディを文字列で取得.
    // * @return ボディテキスト
    // */
    /**
     * This method returns the response in String format.
     * @return Body Text
     */
    public final String bodyAsString() {
        return this.body;
    }

    // /**
    // * レスポンスボディをJSONで取得.
    // * @return JSONオブジェクト
    // * @throws DaoException DAO例外
    // */
    /**
     * This method returns the response in JSON format.
     * @return JSON object
     * @throws DaoException Exception thrown
     */
    public final JSONObject bodyAsJson() throws DaoException {
        String res = bodyAsString();
        try {
            return (JSONObject) new JSONParser().parse(res);
        } catch (ParseException e) {
            throw DaoException.create("parse exception: " + e.getMessage(), 0);
        }
    }

    // /**
    // * レスポンスボディをXMLで取得.
    // * @return XML DOMオブジェクト
    // */
    /**
     * This method returns the response in XML format.
     * @return XML DOM object
     */
    public final Document bodyAsXml() {
        DocumentBuilder builder = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document document = null;
        InputStream is = new ByteArrayInputStream(body.getBytes());
        try {
            document = builder.parse(is);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    HashMap<String, String> parseHeaders(String value) {
        String[] lines = value.split(ODataBatchResponseParser.CRLF);
        // １行目がから ステータスコードを取得
        /** Get the status code from the first line. */
        if (lines[0].startsWith("HTTP")) {
            this.statusCode = Integer.parseInt(lines[0].split(" ")[1]);
        }
        // ２行目以降のレスポンスヘッダをハッシュマップにセット
        /** Set the hash map the response header from the second row. */
        HashMap<String, String> map = new HashMap<String, String>();
        for (String line : lines) {
            String[] key = line.split(":");
            if (key.length > 1) {
                // 前後に空白が含まれている可能性があるため、トリムしてからセットする
                /** Because there is a possibility of spaces in front and rear, so sets it after trim. */
                map.put(key[0].trim(), key[1].trim());
            }
        }
        return map;
    }
}
