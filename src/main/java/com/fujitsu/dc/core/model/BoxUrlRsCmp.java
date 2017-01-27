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

import com.fujitsu.dc.core.DcCoreAuthzException;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.auth.AccessContext;
import com.fujitsu.dc.core.auth.OAuth2Helper.AcceptableAuthScheme;
import com.fujitsu.dc.core.auth.Privilege;

/**
 * Box URL取得リソース オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class BoxUrlRsCmp extends BoxRsCmp {

    /**
     * コンストラクタ.
     * @param cellRsCmp CellRsCmp
     * @param davCmp DavCmp
     * @param accessContext AccessContext
     * @param box ボックス
     */
    public BoxUrlRsCmp(final CellRsCmp cellRsCmp, final DavCmp davCmp,
            final AccessContext accessContext, final Box box) {
        super(cellRsCmp, davCmp, accessContext, box);
    }

    /**
     * アクセス制御を行う.
     * @param ac アクセスコンテキスト
     * @param privilege アクセス可能な権限
     */
    @Override
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        AcceptableAuthScheme allowedAuthScheme = getAcceptableAuthScheme();

        // ユニットユーザトークンチェック
        if (ac.isUnitUserToken()) {
            return;
        }

        // スキーマ認証チェック
        ac.checkSchemaAccess(this.getConfidentialLevel(), this.getBox(), allowedAuthScheme);

        // Basic認証できるかチェック
        ac.updateBasicAuthenticationStateForResource(null);

        // アクセス権チェック
        if (!this.hasPrivilege(ac, privilege)) {
            // トークンの有効性チェック
            // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(allowedAuthScheme);
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw DcCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), allowedAuthScheme);
            }
            throw DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return AcceptableAuthScheme.BEARER;
    }

}
