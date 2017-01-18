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
package com.fujitsu.dc.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.jersey.core.header.InBoundHeaders;

/**
 * テストHTTP応答.
 */
public final class TResponse {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(TResponse.class);

    int statusCode;
    MultivaluedMap<String, String> headers = new InBoundHeaders();
    StringWriter bodyWriter = new StringWriter();

    /**
     * StatusCodeが期待の値であることを確認します。
     * expectedStatusCodeが-1の場合ステータスコードをチェックしない。
     * @param expectedStatusCode 期待されるStatusCode
     * @return TResponse
     */
    public TResponse statusCode(int expectedStatusCode) {
        if (expectedStatusCode != -1) {
            assertEquals(expectedStatusCode, this.statusCode);
        }
        return this;
    }

    /**
     * locationHeaderが期待の値であることを確認します。
     * @param expectedLocationHeaderValue 期待されるLocationHeaderの値
     * @return TResponse
     */
    public TResponse location(String expectedLocationHeaderValue) {
        assertEquals(expectedLocationHeaderValue, this.headers.getFirst(HttpHeaders.LOCATION));
        return this;
    }

    /**
     * Content-Typeヘッダが期待の値であることを確認します。
     * @param expectedContentTypeHeaderValue 期待されるHeaderの値
     * @return TResponse
     */
    public TResponse contentType(String expectedContentTypeHeaderValue) {
        assertEquals(expectedContentTypeHeaderValue, this.headers.getFirst(HttpHeaders.CONTENT_TYPE));
        return this;
    }

    /**
     * 任意のレスポンスヘッダが期待の値であることを確認します。
     * @param headerKey 比較するHeader
     * @param expectedHeaderValue 期待されるHeaderの値
     * @return TResponse
     */
    public TResponse checkHeader(String headerKey, String expectedHeaderValue) {
        assertEquals(expectedHeaderValue, this.headers.getFirst(headerKey));
        return this;
    }

    /**
     * Json形式のエラーレスポンスのチェック.
     * @param expectedCode 期待するエラ―コード
     * @param expectedValue 期待するメッセージ
     * @return TResponse
     */
    public TResponse checkErrorResponse(String expectedCode, String expectedValue) {
        String code = (String) ((JSONObject) this.bodyAsJson()).get("code");
        String value = (String) ((JSONObject) ((JSONObject) this.bodyAsJson()).get("message")).get("value");
        assertEquals(expectedCode, code);
        assertEquals(expectedValue, value);
        return this;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * @return the statusCode
     */
    public String getLocationHeader() {
        return this.headers.getFirst(HttpHeaders.LOCATION);
    }

    /**
     * @return Response Body
     */
    public String getBody() {
        return this.bodyWriter.toString();
    }

    /**
     * @param headerKey ヘッダ名
     * @return ヘッダ値
     */
    public String getHeader(String headerKey) {
        return this.headers.getFirst(headerKey);
    }

    /**
     * @param headerKey ヘッダ名
     * @return ヘッダ値の配列
     */
    public List<String> getHeaders(String headerKey) {
        return this.headers.get(headerKey);
    }

    /**
     * レスポンスボディをJSONで取得.
     * @return JSONオブジェクト
     */
    public JSONObject bodyAsJson() {
        String res = null;
        res = this.bodyWriter.toString();
        JSONObject jsonobject = null;
        try {
            jsonobject = (JSONObject) new JSONParser().parse(res);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        return jsonobject;
    }

    /**
     * レスポンスボディをXMLで取得.
     * @return XML DOMオブジェクト
     */
    public Document bodyAsXml() {
        String str = "";
        try {
            str = this.bodyWriter.toString();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        DocumentBuilder builder = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            fail(e.getMessage());
        }
        Document document = null;
        InputStream is = new ByteArrayInputStream(str.getBytes());
        try {
            document = builder.parse(is);
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return document;
    }

    /**
     * locationHeaderが期待の値であることを確認します。
     * @param inspector ロジック
     * @return TResponse
     */
    public TResponse inspect(Inspector inspector) {
        inspector.inspect(this);
        return this;
    }

    /**
     * inspector.
     */
    public interface Inspector {
        /**
         * @param response response
         */
        void inspect(TResponse response);
    }

    TResponse(BufferedReader sReader) {
        String line = null;
        try {
            line = sReader.readLine();
            String[] l1 = line.split(" ");
            this.statusCode = Integer.valueOf(l1[1]);
            for (;;) {
                if (this.readHeader(sReader) <= 0) {
                    break;
                }
            }

            String body = null;
            if ("chunked".equals(this.headers.getFirst("Transfer-Encoding"))) {
                body = readBodyChunked(sReader);
            } else {
                body = readBodyNonChunk(sReader);
            }
            log.debug(body);
            this.bodyWriter.write(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                sReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    String readBodyNonChunk(BufferedReader is) throws IOException {
        return IOUtils.toString(is);
    }

    String readBodyChunked(BufferedReader br) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            int hex = 2 * 2 * 2 * 2;
            int chunk = Integer.parseInt(line, hex);
            if (chunk == 0) {
                break;
            }
            int chr;
            for (int i = 0; i < chunk; i++) {
                chr = br.read();
                body.append((char) chr);
            }
            chr = br.read();
            if (chr != '\r') {
                fail("Chunk終了後のCRが無い");
            }
            chr = br.read();
            if (chr != '\n') {
                fail("Chunk終了後のLFが無い");
            }
        }
        return body.toString();
    }

    int readHeader(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null) {
            return -1;
        }
        if (line.length() == 0) {
            return 0;
        }
        int idx = line.indexOf(":");
        String k = line.substring(0, idx);
        String v = line.substring(idx + 1);
        this.headers.add(k, v.trim());
        return 1;
    }

    /**
     * デバッグ出力します.
     * @return TResponse
     */
    public TResponse debug() {
        log.debug("SC=" + this.statusCode);
        log.debug("response headers-----");
        for (String k : this.headers.keySet()) {
            log.debug("   " + k + " : " + this.headers.getFirst(k));
        }
        log.debug("response body-----");
        log.debug(this.getBody());
        return this;
    }

}
