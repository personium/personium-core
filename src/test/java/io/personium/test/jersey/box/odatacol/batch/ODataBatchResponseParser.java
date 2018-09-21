/**
 * Personium
 * Copyright 2014 - 2017 FUJITSU LIMITED
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpHeaders;

// This class has been copied from personium-client for testing.
/**
 * This class is used to analyze the response of $ Batch.
 */
public class ODataBatchResponseParser {
    /** Variable BOUNDARY_KEY. */
    static final String BOUNDARY_KEY = "--batch_";
    /** Variable CHARSET_KEY. */
    static final String CHARSET_KEY = "--changeset_";
    /** Variable HTTP. */
    static final String HTTP = "HTTP/1.1";
    /** Variable CRLF. */
    static final String CRLF = System.getProperty("line.separator");
    /** Variable BLANK_LINE. */
    static final String BLANK_LINE = CRLF + CRLF;
    /** Variable CONTENTTYPE_HTTP. */
    static final String CONTENTTYPE_HTTP = "application/http";
    /** Variable CONTENTTYPE_MULTIPART. */
    static final String CONTENTTYPE_MULTIPART = "application/http";

    // /** レスポンス情報の一覧. */
    /** List of response information. */
    ArrayList<ODataResponse> resList = new ArrayList<ODataResponse>();

    // /**
    // * レスポンス解析.
    // * @param reader レスポンスボディReader
    // * @param boudaryKey Boundaryキー
    // * @return ODataResponseの配列
    // */
    /**
     * This method performs analysis on response using Reader.
     * @param reader Reader object
     * @param boudaryKey BoundaryKey
     * @return ODataResponse Array
     */
    public List<ODataResponse> parse(Reader reader, String boudaryKey) {
        parseBoundary(reader, BOUNDARY_KEY);
        return resList;
    }

    // /**
    // * レスポンス解析.
    // * @param in レスポンスボディ文字列
    // * @param boudaryKey Boundaryキー
    // * @return ODataResponseの配列
    // */
    /**
     * This method performs analysis on response using StringReader.
     * @param in Response Body String
     * @param boudaryKey BoundaryKey
     * @return ODataResponse Array
     */
    public List<ODataResponse> parse(String in, String boudaryKey) {
        parseBoundary(new StringReader(in), BOUNDARY_KEY);
        return resList;
    }

    /**
     * This method parses the boundary.
     * @param reader Reader object
     * @param boudaryKey BoundaryKey
     */
    void parseBoundary(Reader reader, String boudaryKey) {
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        try {
            String str = br.readLine();
            while (str != null) {
                if (str.startsWith(boudaryKey)) {
                    if (sb.length() > 0) {
                        parseBodyBlock(sb.toString());
                        sb = new StringBuilder();
                    }
                    str = br.readLine();
                    continue;
                }
                sb.append(str);
                sb.append(CRLF);
                str = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method parses the body.
     * @param body Response Body String
     */
    void parseBodyBlock(String body) {
        // 空行で分割する
        /** To separate by a blank line. */
        String[] blocks = body.split(BLANK_LINE);

        // ブロックが2個以上存在しなければHttpレスポンス型ではない
        /** It is not a Http response type block unless there are two or more. */
        if (blocks.length < 2) {
            return;
        }

        // ブロックのヘッダ部を取得
        /** Get the header portion of the block. */
        HashMap<String, String> boundaryHeaders = parseHeaders(blocks[0]);

        // ブロックヘッダのContent-Typeを取得
        /** Get the Content-Type header of the block. */
        String contentType = boundaryHeaders.get(HttpHeaders.CONTENT_TYPE);

        if (contentType != null) {
            if (contentType.startsWith(CONTENTTYPE_HTTP)) {
                // application/http ならば １つのリクエスト
                /** one request if application / http. */
                StringBuilder responseBody = new StringBuilder();
                // ボディ内に空行がふくまれている場合、２個目以降を連結する
                /** If a blank line is included in the body, connect the second and subsequent lines. */
                for (int i = 2; i < blocks.length; i++) {
                    responseBody.append(blocks[i]);
                }
                resList.add(new ODataResponse(blocks[1], responseBody.toString()));
            } else {
                // multipart/mixed ばらばマルチパート(複数のブロックで構成)
                /** (consist of blocks) multipart / mixed multipart Barabbas. */
                parseBoundary(new StringReader(body), CHARSET_KEY);
            }
        }
    }

    // /**
    // * 複数行の塊となっているレスポンスヘッダーを分解してハッシュマップにセットする.
    // * @param value レスポンスヘッダ文字列
    // * @return １つ１つに分解されたハッシュマップ
    // */
    /**
     * This method parses the headers by setting the hash map by decomposing the response header if it is of more than
     * one line.
     * @param value Response Header String
     * @return Hash map that has been broken down into one single value
     */
    HashMap<String, String> parseHeaders(String value) {
        // 改行コードで分解する
        /** Decompose with a new line code. */
        String[] lines = value.split(CRLF);
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
