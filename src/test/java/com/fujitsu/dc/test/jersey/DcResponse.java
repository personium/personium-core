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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * DAVのレスポンス型.
 */
public class DcResponse {
    /** レスポンスオブジェクト. */
    private HttpResponse response;

    /**
     * コンストラクタ.
     */
    public DcResponse() {
    }

    /**
     * コンストラクタ.
     * @param resObj レスポンスオブジェクト
     */
    public DcResponse(final HttpResponse resObj) {
        this.response = resObj;
    }

    /**
     * ステータスコードの取得.
     * @return ステータスコード
     */
    public final int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * すべてのレスポンスヘッダの一覧を取得.
     * @return レスポンスヘッダ一覧
     */
    public final Header[] getResponseAllHeaders() {
        return response.getAllHeaders();
    }

    /**
     * 指定したレスポンスヘッダの一覧を取得.
     * @param value 取得するレスポンスヘッダ名
     * @return レスポンスヘッダ一覧
     */
    public final Header[] getResponseHeaders(final String value) {
        return response.getHeaders(value);
    }

    /**
     * 指定したレスポンスヘッダの一覧を取得.
     * @param name 取得するレスポンスヘッダ名
     * @return レスポンスヘッダ一覧
     */
    public final String getFirstHeader(String name) {
        try {
            return response.getFirstHeader(name).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * レスポンスボディをストリームで取得.
     * @return ストリーム
     */
    public final InputStream bodyAsStream() {
        HttpEntity ent = response.getEntity();
        if (ent == null) {
            return null;
        }
        try {
            return ent.getContent();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * レスポンスボディを文字列で取得.
     * @return ボディテキスト
     * @throws DaoException DAO例外
     */
    public final String bodyAsString() throws DaoException {
        return this.bodyAsString("utf-8");
    }

    /**
     * レスポンスボディを文字列で取得.
     * @param enc 文字コード
     * @return ボディテキスト
     * @throws DaoException DAO例外
     */
    public final String bodyAsString(final String enc) throws DaoException {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try {
            is = this.getResponseBodyInputStream(response);
            isr = new InputStreamReader(is, enc);
            reader = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            int chr;
            while ((chr = reader.read()) != -1) {
                sb.append((char) chr);
            }
            return sb.toString();
        } catch (IOException e) {
            throw DaoException.create("io exception", 0);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                throw DaoException.create("io exception", 0);
            } finally {
                try {
                    if (isr != null) {
                        isr.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e2) {
                    throw DaoException.create("io exception", 0);
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (Exception e3) {
                        throw DaoException.create("io exception", 0);
                    }
                }
            }
        }
    }

    /**
     * レスポンスボディをJSONで取得.
     * @return JSONオブジェクト
     */
    public final JSONObject bodyAsJson() {
        String res = null;
        try {
            res = bodyAsString();
        } catch (DaoException e) {
            fail(e.getMessage());
        }
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
    public final Document bodyAsXml() {
        String str = "";
        try {
            str = bodyAsString();
        } catch (DaoException e) {
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
     * レスポンスボディのストリームを受け取る.
     * @param res Responseオブジェクト
     * @return ストリーム
     * @throws IOException IO例外
     */
    protected final InputStream getResponseBodyInputStream(final HttpResponse res) throws IOException {
        // GZip 圧縮されていたら解凍する。
        Header[] contentEncodingHeaders = res.getHeaders("Content-Encoding");
        if (contentEncodingHeaders.length > 0 && "gzip".equalsIgnoreCase(contentEncodingHeaders[0].getValue())) {
            return new GZIPInputStream(res.getEntity().getContent());
        } else {
            return res.getEntity().getContent();
        }
    }
}
