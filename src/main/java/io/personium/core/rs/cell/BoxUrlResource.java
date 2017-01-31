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
package io.personium.core.rs.cell;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

import io.personium.core.DcCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.BoxUrlRsCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.utils.ODataUtils;

/**
 * BoxURL取得用JAX-RS Resource.
 */
public class BoxUrlResource {

    private AccessContext accessContext = null;
    private CellRsCmp cellRsCmp;

    /**
     * constructor.
     * @param cellRsCmp DavRsCmp
     */
    public BoxUrlResource(final CellRsCmp cellRsCmp) {
        this.cellRsCmp = cellRsCmp;
        this.accessContext = this.cellRsCmp.getAccessContext();
    }

    /**
     * BoxURL取得のエンドポイント .
     * @param querySchema 取得対象のBoxのスキーマURL
     * @return BoxUrlResourceオブジェクト
     */
    @GET
    public final Response boxUrl(@QueryParam("schema") final String querySchema) {

        String schema = querySchema;
        if (schema == null) {
            // スキーマパラメタが存在しない場合は、認証トークンからスキーマ情報を取得する
            schema = this.accessContext.getSchema();

            // トークンのスキーマがConfidentialClientの場合は、#cを削除してボックスを取得する
            if (schema != null && schema.endsWith(OAuth2Helper.Key.CONFIDENTIAL_MARKER)) {
                schema = schema.replaceAll(OAuth2Helper.Key.CONFIDENTIAL_MARKER, "");
            }
        } else {
            // クエリ指定がある場合は、schemaのチェックをおこなう
            if (!ODataUtils.isValidSchemaUri(querySchema)) {
                throw DcCoreException.OData.QUERY_INVALID_ERROR.params("schema", querySchema);
            }
        }

        Box box = null;
        if (schema == null || schema.length() == 0) {
            box = this.cellRsCmp.getBox();
        } else {
            // スキーマ情報からBoxを取得する
            box = this.cellRsCmp.getCell().getBoxForSchema(schema);
        }

        // Boxが存在しない場合も権限エラーを返却する
        if (box == null) {
            // Basic認証が許可されているかのチェック
            this.accessContext.updateBasicAuthenticationStateForResource(null);
            if (AccessContext.TYPE_INVALID.equals(accessContext.getType())) {
                accessContext.throwInvalidTokenException(this.cellRsCmp.getAcceptableAuthScheme());
            }
            throw DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

        // 認証トークンの有効性チェック（有効期限の切れているトークンなど）
        DavCmp davCmp = ModelFactory.boxCmp(box);
        DavRsCmp boxUrlRsCmp = new BoxUrlRsCmp(this.cellRsCmp, davCmp, this.accessContext, box);
        boxUrlRsCmp.checkAccessContext(this.accessContext, BoxPrivilege.READ);

        // レスポンスを返却する
        return Response.status(HttpStatus.SC_OK)
                .header(HttpHeaders.LOCATION, box.getCell().getUrl() + box.getName())
                .build();
    }

}
