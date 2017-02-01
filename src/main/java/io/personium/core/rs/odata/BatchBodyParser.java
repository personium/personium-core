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
package io.personium.core.rs.odata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import com.sun.jersey.api.uri.UriComponent;

/**
 * BatchBodyParserクラス.
 */
public class BatchBodyParser {

    private String collectionUri = null;

    // BoundaryParserクラスでリクエストを解析した際に有効な$topの指定値を加算する
    private int bulkTopCount = 0;

    /**
     * $batchのリクエストボディをパースする.
     * @param boundary バウンダリ文字列
     * @param reader リクエストボディ
     * @param requestUriParam baseUri
     * @return BatchBodyPartのリスト
     */
    public List<BatchBodyPart> parse(String boundary, Reader reader, String requestUriParam) {
        // TODO リクエストが１万件を超える場合エラーとする

        this.collectionUri = requestUriParam.split("/\\$batch")[0];

        BufferedReader br = new BufferedReader(reader);
        // ボディを取得する
        StringBuilder body;
        try {
            body = getBatchBody(br);
        } catch (IOException e) {
            // IOExceptionは重大障害
            throw PersoniumCoreException.Server.UNKNOWN_ERROR.reason(e);
        }

        if (!body.toString().startsWith("--" + boundary + "\n")) {
            // リクエストボディの先頭が「--バウンダリー文字列」で始まっていなければエラーとする
            throw PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }
        if (!body.toString().trim().endsWith("--" + boundary + "--")) {
            // リクエストボディの最後が「--バウンダリー文字列--」で終わっていなければエラーとする
            throw PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }

        // 個々のリクエストを取得する
        List<BatchBodyPart> requests = getRequests(body.toString(), boundary);
        return requests;
    }

    /**
     * $batchのリクエストボディを取得する.
     * @param br リクエストボディ
     * @return リクエストボディ（StringBuilder）
     * @throws IOException
     */
    private StringBuilder getBatchBody(BufferedReader br) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            body.append(line + "\n");
        }
        return body;
    }

    /**
     * $batchのリクエストボディから個々のリクエストを取得し、リストで返却する.
     * @param body $batchのリクエストボディ
     * @param boundaryStr バウンダリ文字列
     * @return BatchBodyPartのリスト
     */
    private List<BatchBodyPart> getRequests(String body, String boundaryStr) {
        List<BatchBodyPart> requests = new ArrayList<BatchBodyPart>();
        // ボディをboundaryで分割する
        String[] arrBoundary = splitBoundary(body, boundaryStr);
        for (String boundaryBody : arrBoundary) {
            if (!boundaryBody.equals("")) {
                BoundaryParser boundary = new BoundaryParser(null, boundaryStr);
                requests.addAll(boundary.parse(boundaryBody));
            }
        }
        return requests;
    }

    private String[] splitBoundary(String input, String boundaryStr) {
        return input.split("--" + boundaryStr + "\n");
    }

    /**
     * バウンダリの中身のパーサ.
     */
    class BoundaryParser {

        private Map<String, String> headers = null;
        private BoundaryParser parent = null;
        private String boundaryStr = null;

        /**
         * コンストラクタ.
         * @param boundary バウンダリ文字列
         */
        BoundaryParser(BoundaryParser boundary, String boundaryStr) {
            this.parent = boundary;
            this.boundaryStr = boundaryStr;
        }

        /**
         * バウンダリの中を解析する.
         * @param boundaryBody バウンダリのボディパート
         * @return BatchBodyPartのリスト
         */
        List<BatchBodyPart> parse(String boundaryBody) {

            List<BatchBodyPart> requests = new ArrayList<BatchBodyPart>();

            // 行で分割
            List<String> bodyLines = Arrays.asList(boundaryBody.split("\n"));

            // コンテントタイプを取得
            String type = getContentType(bodyLines);
            if (type == null) {
                // Content-Typeの指定がない
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }

            // ボディを取得
            String boundaryBodyPart = getBoundaryBody(bodyLines, this.headers.size());

            if (type.equals("application/http")) {
                // リクエストの処理
                requests.add(getRequest(boundaryBodyPart));
            } else if (type.startsWith("multipart/mixed")) {
                // changesetの処理

                // changesetのネスト指定
                if (this.parent != null) {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR;
                }

                // changesetのバウンダリ文字列を取得
                String changeset = getBoundaryStr(type);

                // ボディをboundaryで分割する
                List<BatchBodyPart> changesetRequests = new ArrayList<BatchBodyPart>();
                String[] arrChangeset = splitBoundary(boundaryBodyPart, changeset);
                for (String changesetBody : arrChangeset) {
                    if (!changesetBody.equals("")) {
                        BoundaryParser changesetBoundary = new BoundaryParser(this, changeset);
                        changesetRequests.addAll(changesetBoundary.parse(changesetBody));
                    }
                }

                // changeset始端フラグの設定
                changesetRequests.get(0).setbChangesetStart(true);
                // changeset終端フラグの設定
                changesetRequests.get(changesetRequests.size() - 1).setChangesetEnd(true);

                requests.addAll(changesetRequests);
            } else {
                // Content-Typeが不正
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }

            return requests;

        }

        /**
         * バウンダリの中からリクエストを取得しBatchBodyPart型で返却する.
         * @param bodyPart バウンダリのボディパート
         * @return BatchBodyPart
         */
        private BatchBodyPart getRequest(String bodyPart) {

            // リクエストの形
            // ---------
            // HTTPヘッダ
            // ：
            // 空行
            // {メソッド} {リクエストの相対パス}
            // {リクエストヘッダ}
            // 空行
            // {リクエストボディ}
            // ---------

            List<String> lines = Arrays.asList(bodyPart.split("\n"));
            List<String> partHeaders = lines.subList(1, lines.size());
            Map<String, String> requestHeaders = getHeaders(partHeaders);

            BatchBodyPart batchBodyPart = new BatchBodyPart(requestHeaders);
            // requestLineは、「{メソッド} {リクエストの相対パス}」
            String requestLine = lines.get(0);
            String method = getMethod(requestLine);
            batchBodyPart.setHttpMethod(method);

            String requestUri = getUri(requestLine);
            String requestPath = getPath(requestUri);
            String requestQuery = getQuery(requestUri);

            if (requestPath.indexOf("/") > 0) {
                // $links、NP経由系のリクエストはクエリが未サポートのため指定があった場合はエラーとする
                // TODO 取得のリクエストの制限が解除された場合はクエリを許可すること
                if (!requestQuery.equals("")) {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
                Pattern linkPathPattern = Pattern
                        .compile("^([^\\(]*)\\('([^']*)'\\)\\/\\$links\\/([^\\(]*)(\\('([^')]*)'\\))?$");
                Matcher linkMatcher = linkPathPattern.matcher(requestPath);
                Pattern npPathPattern = Pattern
                        .compile("^([^\\(]*)\\('([^']*)'\\)\\/_([^\\(]*)(\\('([^')]*)'\\))?$");
                Matcher npMatcher = npPathPattern.matcher(requestPath);
                if (linkMatcher.find()) {
                    // $linksリクエスト
                    batchBodyPart.setIsLinksRequest(true);
                    batchBodyPart.setSourceEntitySetName(linkMatcher.replaceAll("$1"));
                    batchBodyPart.setSourceEntityKey(linkMatcher.replaceAll("$2"));
                    batchBodyPart.setTargetEntitySetName(linkMatcher.replaceAll("$3"));
                    batchBodyPart.setTargetEntityKey(linkMatcher.replaceAll("$5"));
                    batchBodyPart.setUri(collectionUri + "/" + requestUri);
                } else if (npMatcher.find()) {
                    // NavigationPropertyあり
                    batchBodyPart.setNavigationProperty(requestPath);
                    String targetNvProName = batchBodyPart.getTargetNavigationProperty();
                    batchBodyPart.setTargetEntitySetName(npMatcher.replaceAll("$3"));
                    batchBodyPart.setUri(collectionUri + "/" + targetNvProName);
                } else {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
            } else {
                Pattern reqPattern = Pattern.compile("^([^\\(]*)(\\('([^']*)'\\))?$");
                Matcher reqMatcher = reqPattern.matcher(requestPath);
                if (reqMatcher.find()) {
                    String targetID = reqMatcher.replaceAll("$2");
                    // メソッドとパスの整合性をチェックする
                    if (HttpMethod.POST.equals(method)) {
                        // POSTの場合_ターゲットのIDが指定されていればエラーとする
                        if (null != targetID && !"".equals(targetID)) {
                            throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                        }
                    } else if (!HttpMethod.GET.equals(method)
                            && (null == targetID || "".equals(targetID))) {
                        // POST,GET以外の場合_ターゲットのIDが指定されていなければエラーとする
                        throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                    }
                    // GETの場合_一件取得・一覧取得の両方があるため、ターゲットのIDの有無はチェックしない
                } else {
                    throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params(requestLine);
                }
                batchBodyPart.setUri(collectionUri + "/" + requestPath);
            }
            batchBodyPart.setEntity(getBoundaryBody(lines, requestHeaders.size() + 1));
            batchBodyPart.setChangesetStr(getChangesetStr());

            // GETメソッド以外でクエリが指定された場合は指定を無視する
            if (HttpMethod.GET.equals(method)) {
                batchBodyPart.setRequestQuery(requestQuery);
                Integer top = null;
                // $batchリクエスト全体の$top指定値の合計をチェックするために、$topの値だけを取得している
                // 各クエリの値のエラーはリクエスト送信時にチェックするため、ここでは例外は無視している
                try {
                    top = QueryParser.parseTopQuery(UriComponent.decodeQuery(requestQuery, true).getFirst("$top"));
                } catch (PersoniumCoreException e) {
                    top = null;
                }
                if (top != null) {
                    checkTopCount(top);
                }
            }

            return batchBodyPart;
        }

        private String getChangesetStr() {
            String changesetStr = null;

            if (this.parent != null) {
                // changesetの処理の場合
                return this.boundaryStr;
            }

            return changesetStr;
        }

        /**
         * ヘッダをセットし、コンテントタイプを返却する.
         * @param bodyLines バウンダリの中身
         * @return コンテントタイプ
         */
        private String getContentType(List<String> bodyLines) {
            Map<String, String> map = getHeaders(bodyLines);
            this.headers = map;
            return this.headers.get(HttpHeaders.CONTENT_TYPE);
        }

        /**
         * ボディパートの中からヘッダを取得する.
         * @param bodyLines ボディパート
         * @return ヘッダ
         */
        private Map<String, String> getHeaders(List<String> bodyLines) {
            Map<String, String> map = new HashMap<String, String>();
            for (String bodyLine : bodyLines) {
                if (bodyLine.equals("")) {
                    break;
                }
                String[] types = bodyLine.split("[\\s]*:[\\s]*");
                if (types.length > 1) {
                    map.put(types[0], types[1]);
                }
            }
            return map;
        }

        /**
         * HTTPメソッドを取得する.
         * @param line {メソッド} {パス} *
         * @return HTTPメソッドの文字列
         */
        private String getMethod(String line) {
            Pattern pattern = Pattern.compile("(^[A-Z]+)[\\s]+([^\\s]+).*");
            Matcher m = pattern.matcher(line);

            String method = m.replaceAll("$1");

            // 受付可能なメソッドかどうかチェックする
            if (!HttpMethod.POST.equals(method) && !HttpMethod.GET.equals(method) && !HttpMethod.PUT.equals(method)
                    && !HttpMethod.DELETE.equals(method)) {
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_METHOD_ERROR.params(method);
            }
            return method;
        }

        /**
         * リクエストのURIパスを取得する.
         * @param line {メソッド} {URIパス} *
         * @return リクエストのURIパス
         */
        private String getUri(String line) {
            Pattern pattern = Pattern.compile("(^[A-Z]+)[\\s]+([^\\s]+).*");
            Matcher m = pattern.matcher(line);

            return m.replaceAll("$2");
        }

        /**
         * リクエストの相対パスを取得する.
         * @param line {メソッド} {パス} *
         * @return リクエストの相対パス
         */
        private String getPath(String line) {
            String[] path = line.split("\\?");
            return path[0];
        }

        /**
         * リクエストの相対パスを取得する.
         * @param line {メソッド} {パス} *
         * @return リクエストの相対パス
         */
        private String getQuery(String line) {
            String[] path = line.split("\\?", 2);
            if (path.length != 2) {
                return "";
            }
            return path[1];
        }

        /**
         * バウンダリのボディパートからボディを取得する.
         * @param bodyLines ボディパート
         * @param headersize ヘッダのサイズ
         * @return ボディ
         */
        private String getBoundaryBody(List<String> bodyLines, int headersize) {
            StringBuilder boundaryBody = new StringBuilder();
            for (int i = headersize + 1; i < bodyLines.size(); i++) {
                if (bodyLines.get(i).contains("--" + this.boundaryStr + "--")) {
                    break;
                }
                boundaryBody.append(bodyLines.get(i));
                boundaryBody.append("\n");
            }
            return boundaryBody.toString();
        }

        /**
         * バウンダリ文字列を取得する.
         * @param contentType Content-Typeの値
         * @return バウンダリ文字列
         */
        private String getBoundaryStr(String contentType) {
            String boundary = null;

            Pattern pattern = Pattern.compile(".+boundary=(.+)");
            Matcher m = pattern.matcher(contentType);
            // バウンダリの指定が無い
            if (!m.matches()) {
                throw PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(HttpHeaders.CONTENT_TYPE);
            }
            boundary = m.replaceAll("$1");

            return boundary;
        }

        /**
         * バルクリクエスト全体で指定された$top数をチェックする.
         * バルクリクエスト全体で$topの合計値が1万以上の場合は400エラーを返却する
         * @param query
         */
        private void checkTopCount(int query) {
            bulkTopCount += query;
            if (bulkTopCount > PersoniumUnitConfig.getTopQueryMaxSize()) {
                throw PersoniumCoreException.OData.BATCH_TOTAL_TOP_COUNT_LIMITATION_EXCEEDED;
            }

        }
    }
}
