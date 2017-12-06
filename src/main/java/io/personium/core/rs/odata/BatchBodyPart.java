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

import java.util.Map;

/**
 * OdataBatchクラス.
 */
public class BatchBodyPart {
    private String httpMethod = null;
    private Map<String, String> httpHeaders = null;
    private String body = null;
    private String uri = null;
    private String changesetStr = null;
    private Boolean bChangesetStart = false;
    private Boolean bChangesetEnd = false;
    private String uriLast = null;

    private Boolean isLinksRequest = false;

    private String sourceEntitySetName;
    private String sourceEntitySetKey;
    private String targetEntitySetName;
    private String targetEntitySetKey;

    private String targetNavigationProperty;

    private String requestQuery = null;

    BatchBodyPart(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    /**
     * HttpHeadersのゲッター.
     * @return HttpHeaders
     */
    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * リクエストボディのゲッター.
     * @return リクエストボディ
     */
    public String getEntity() {
        return this.body;
    }

    /**
     * リクエストボディのセッター.
     * @param bodyParam リクエストボディ
     */
    public void setEntity(String bodyParam) {
        this.body = bodyParam;
    }

    /**
     * uriのゲッター.
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * uriのセッター.
     * @param uri uri
     */
    public void setUri(String uri) {
        this.uri = uri;

        if (this.uri.endsWith("/")) {
            // URLが"/"で終わっている場合は、終端の"/"を削除する
            this.uri = this.uri.substring(0, this.uri.length() - 2);
        }

        // URLの最後のパスを取得
        int index = this.uri.lastIndexOf('/');
        this.uriLast = this.uri.substring(index + 1);
    }

    /**
     * HttpMethodのゲッター.
     * @return HttpMethod
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * HttpMethodのセッター.
     * @param httpMethod HttpMethod
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * エンティティセット名を取得する.
     * @return エンティティセット名
     */
    public String getEntitySetName() {
        if (hasNavigationProperty() || isLinksRequest) {
            return this.sourceEntitySetName;
        } else {
            int i = this.uriLast.indexOf('(');
            if (i != -1) {
                return this.uriLast.substring(0, i);
            } else {
                return this.uriLast;
            }
        }
    }

    /**
     * エンティティセット名を設定する.
     * @param entitySetName エンティティセット名
     */
    public void setSourceEntitySetName(String entitySetName) {
        this.sourceEntitySetName = entitySetName;
    }

    /**
     * エンティティキーを取得する.
     * @return エンティティキー
     */
    public String getEntityKeyWithParences() {
        if (hasNavigationProperty() || isLinksRequest) {
            return "('" + this.sourceEntitySetKey + "')";
        } else {
            int i = this.uriLast.indexOf('(');
            if (i > -1 && i < this.uriLast.length()) {
                return this.uriLast.substring(i);
            } else {
                return null;
            }
        }
    }

    /**
     * エンティティキーを取得する.
     * @return エンティティキー
     */
    public String getEntityKey() {
        if (hasNavigationProperty()) {
            return "'" + this.sourceEntitySetKey + "'";
        } else {
            int i = this.uriLast.indexOf('(');
            if (i > -1 && i < this.uriLast.length()) {
                return this.uriLast.substring(i + 1, this.uriLast.length() - 1);
            } else {
                return null;
            }
        }
    }

    /**
     * エンティティキーを設定する.
     * @param entityKey エンティティキー
     */
    public void setSourceEntityKey(String entityKey) {
        this.sourceEntitySetKey = entityKey;
    }

    /**
     * changesetの始端かどうかを返却する.
     * @return true: changesetの始端
     */
    public Boolean isChangesetStart() {
        return bChangesetStart;
    }

    /**
     * changesetの始端フラグのセッター.
     * @param flg true: changesetの始端
     */
    public void setbChangesetStart(Boolean flg) {
        this.bChangesetStart = flg;
    }

    /**
     * changesetの終端かどうかを返却する.
     * @return true: changesetの終端
     */
    public Boolean isChangesetEnd() {
        return bChangesetEnd;
    }

    /**
     * changesetの終端フラグのセッター.
     * @param flg true: changesetの終端
     */
    public void setChangesetEnd(Boolean flg) {
        this.bChangesetEnd = flg;
    }

    /**
     * changeset文字列のゲッター.
     * @return changeset文字列
     */
    public String getChangesetStr() {
        return changesetStr;
    }

    /**
     * changeset文字列のセッター.
     * @param changesetStr changeset文字列
     */
    public void setChangesetStr(String changesetStr) {
        this.changesetStr = changesetStr;
    }

    /**
     * NavigationProperty経由の登録時に指定されたパスをもとにソース/ターゲット情報を設定する.
     * @param requestPath requestPath
     */
    public void setNavigationProperty(String requestPath) {
        int sourceKeyStartIndex = requestPath.indexOf("(") + 2;
        int sourceKeyEndIndex = requestPath.indexOf("'", sourceKeyStartIndex);
        this.sourceEntitySetName = requestPath.substring(0, sourceKeyStartIndex - 2);
        this.sourceEntitySetKey = requestPath.substring(sourceKeyStartIndex, sourceKeyEndIndex);
        int lastPathNameIndex = requestPath.indexOf("/") + 1;
        this.targetNavigationProperty = requestPath.substring(lastPathNameIndex, requestPath.length());
    }

    /**
     * NavigationProperty経由での登録時に使用するターゲットEntitySet名を取得する.
     * @return the targetEntitySetName
     */
    public String getTargetNavigationProperty() {
        return this.targetNavigationProperty;
    }

    /**
     * このバルクリクエストがNavigationProperty経由の登録を行おうとしているかどうかを返却する.
     * @return NavigationProperty経由の登録時はtrueを、それ以外はfalseを返す
     */
    public boolean hasNavigationProperty() {
        return this.targetNavigationProperty != null;
    }

    /**
     * CollectionまでのURIを返却する.
     * @return CollectionまでのURI
     */
    public String getCollectionUri() {
        int index = this.uri.lastIndexOf('/');
        return this.uri.substring(0, index);
    }

    /**
     * このバルクリクエストがLinks登録を行おうとしているかどうかを返却する.
     * @return Links登録時はtrueを、それ以外はfalseを返す
     */
    public Boolean isLinksRequest() {
        return isLinksRequest;
    }

    /**
     * このバルクリクエストがLinks登録を行おうとしているかどうかを設定する.
     * @param isLinksRequest Links登録時はtrueを、それ以外はfalse
     */
    public void setIsLinksRequest(Boolean isLinksRequest) {
        this.isLinksRequest = isLinksRequest;
    }

    /**
     * $links先のEntitySet名を取得する.
     * @return $links先のEntitySet名
     */
    public String getTargetEntitySetName() {
        return targetEntitySetName;
    }

    /**
     * $links先のEntitySet名を設定する.
     * @param entitySetName $links先のEntitySet名
     */
    public void setTargetEntitySetName(String entitySetName) {
        this.targetEntitySetName = entitySetName;
    }

    /**
     * $links先のEntityキーを取得する.
     * @return $links先のEntityキー
     */
    public String getTargetEntityKey() {
        return targetEntitySetKey;
    }

    /**
     * $links先のEntityキーを設定する.
     * @param entityKey $links先のEntityキー
     */
    public void setTargetEntityKey(String entityKey) {
        this.targetEntitySetKey = entityKey;
    }

    /**
     * バルクリクエストのクエリを取得する.
     * @return バルクリクエストのクエリ
     */
    public String getRequestQuery() {
        return this.requestQuery;
    }

    /**
     * バルクリクエストのクエリを設定する.
     * @param query クエリ
     */
    public void setRequestQuery(String query) {
        this.requestQuery = query;
    }

}
