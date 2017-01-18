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
package com.fujitsu.dc.core.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;

/**
 * JaxRS Resource オブジェクトから処理の委譲を受けてDavのMoveに関する処理を行うクラス.
 */
public class DavMoveResource extends DavRsCmp {

    private String depth = null;
    private String overwrite = null;
    private String destination = null;
    private String ifMatch = null;

    /**
     * コンストラクタ.
     * @param parent 親リソース
     * @param davCmp バックエンド実装に依存する処理を受け持つ部品
     * @param headers リクエストヘッダ情報
     */
    public DavMoveResource(DavRsCmp parent, DavCmp davCmp, HttpHeaders headers) {
        super(parent, davCmp);
        // リクエストヘッダから、移動に必要な情報を取得する
        depth = getFirstHeader(headers, org.apache.http.HttpHeaders.DEPTH, DavCommon.DEPTH_INFINITY);
        overwrite = getFirstHeader(headers, org.apache.http.HttpHeaders.OVERWRITE, DavCommon.OVERWRITE_FALSE);
        destination = getFirstHeader(headers, org.apache.http.HttpHeaders.DESTINATION);
        ifMatch = getFirstHeader(headers, HttpHeaders.IF_MATCH, "*");
    }

    /**
     * MOVEメソッドの処理.
     * @return JAX-RS応答オブジェクト
     */
    public Response doMove() {

        // リクエストヘッダのバリデート
        validateHeaders();

        // 移動先のBox情報を取得する
        BoxRsCmp boxRsCmp = getBoxRsCmp();

        // 移動先の情報を生成
        DavDestination davDestination;
        try {
            davDestination = new DavDestination(destination, this.getAccessContext().getBaseUri(), boxRsCmp);
        } catch (URISyntaxException e) {
            // URI 形式でない
            throw DcCoreException.Dav.INVALID_REQUEST_HEADER.params(org.apache.http.HttpHeaders.DESTINATION,
                    destination);
        }

        // データの更新・削除
        ResponseBuilder response = this.davCmp.move(this.ifMatch, this.overwrite, davDestination);

        return response.build();
    }

    private BoxRsCmp getBoxRsCmp() {
        // 最上位にあるBoxのリソース情報を取得する
        DavRsCmp davRsCmp = this;
        for (int i = 0; i <= DcCoreConfig.getMaxCollectionDepth(); i++) {
            DavRsCmp parent = davRsCmp.getParent();
            if (null == parent) {
                break;
            }
            davRsCmp = parent;
        }

        // 最上位までたどった結果をBoxとして扱う
        BoxRsCmp boxRsCmp = (BoxRsCmp) davRsCmp;
        return boxRsCmp;
    }

    /**
     * Moveメソッドに関するヘッダのバリデートを行う. <br />
     * バリデートに失敗した場合は、例外をスローする.
     * @param headers ヘッダ情報
     */
    void validateHeaders() {
        // Depth ヘッダ
        if (!DavCommon.DEPTH_INFINITY.equalsIgnoreCase(depth)) {
            throw DcCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        // Overwrite ヘッダ
        if (!DavCommon.OVERWRITE_FALSE.equalsIgnoreCase(overwrite)
                && !DavCommon.OVERWRITE_TRUE.equalsIgnoreCase(overwrite)) {
            throw DcCoreException.Dav.INVALID_REQUEST_HEADER.params(org.apache.http.HttpHeaders.OVERWRITE, overwrite);
        }

        // Destination ヘッダ
        if (destination == null || destination.length() <= 0) {
            throw DcCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST
                    .params(org.apache.http.HttpHeaders.DESTINATION);
        }

        if (this.getUrl().equals(destination)) {
            // 移動元と移動先が等しい場合はエラーとする
            throw DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destination);
        }

        DavPath destinationPath = getDestination();
        if (!this.getAccessContext().getBaseUri().equals(destinationPath.getBaseUri())) {
            // スキーマ、ホストが移動元、移動先で異なる場合はエラーとする
            throw DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    org.apache.http.HttpHeaders.DESTINATION, destination);
        }

        if (!this.getCell().getName().equals(destinationPath.getCellName())
                || !this.getBox().getName().equals(destinationPath.getBoxName())) {
            // Cell、Box が移動元、移動先で異なる場合はエラーとする
            throw DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    org.apache.http.HttpHeaders.DESTINATION, destination);
        }
    }

    /**
     * 移動先のオブジェクのパス情報を作成して返却する.
     * @return DavPath 移動先のリソースパスを管理するオブジェクト
     */
    private DavPath getDestination() {
        URI destUri;
        try {
            destUri = new URI(destination);
        } catch (URISyntaxException e) {
            // URI 形式でない
            throw DcCoreException.Dav.INVALID_REQUEST_HEADER.params(org.apache.http.HttpHeaders.DESTINATION,
                    destination);
        }
        return new DavPath(destUri, this.getAccessContext().getBaseUri());
    }

    /**
     * ヘッダ情報から指定されたキーのヘッダを取得する. <br />
     * 存在しない場合はnullを返却する.
     * @param headers ヘッダ情報
     * @param key 取得するヘッダのキー
     * @return 指定されたキーのヘッダ
     */
    private String getFirstHeader(HttpHeaders headers, String key) {
        return this.getFirstHeader(headers, key, null);
    }

    /**
     * ヘッダ情報から指定されたキーのヘッダを取得する. <br />
     * 存在しない場合はnullを返却する.
     * @param headers ヘッダ情報
     * @param key 取得するヘッダのキー
     * @param defaultValue ヘッダが存在しない場合のデフォルト値
     * @return 指定されたキーのヘッダ
     */
    private String getFirstHeader(HttpHeaders headers, String key, String defaultValue) {
        List<String> header = headers.getRequestHeader(key);
        if (header != null && header.size() > 0) {
            return header.get(0);
        }
        return defaultValue;
    }

}
