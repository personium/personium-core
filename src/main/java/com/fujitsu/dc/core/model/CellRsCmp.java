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
 * JaxRS Resource オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class CellRsCmp extends DavRsCmp {

    DavCmp davCmp;
    Cell cell;
    AccessContext accessContext;

    /**
     * コンストラクタ.
     * @param davCmp DavCmp
     * @param cell Cell
     * @param accessContext AccessContext
     */
    public CellRsCmp(final DavCmp davCmp, final Cell cell, final AccessContext accessContext) {
        super(null, davCmp);
        this.cell = cell;
        this.accessContext = accessContext;
        this.davCmp = davCmp;
    }

    /**
     * このリソースのURLを返します.
     * @return URL文字列
     */
    public String getUrl() {
        return this.cell.getUrl();
    }

    /**
     * リソースが所属するCellを返す.
     * @return Cellオブジェクト
     */
    public Cell getCell() {
        return this.cell;
    }

    /**
     * リソースが所属するBoxを返す.
     * @return Boxオブジェクト
     */
    public Box getBox() {
        return null;
    }

    /**
     * このリソースのdavCmpを返します.
     * @return davCmp
     */
    public DavCmp getDavCmp() {
        return this.davCmp;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * ACL情報を確認し、アクセス可能か判断する.
     * @param ac アクセスコンテキスト
     * @param privilege ACLのプリビレッジ（readとかwrite）
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {

        // davCmpが無い（存在しないリソースが指定された）場合はそのリソースのACLチェック飛ばす
        if (this.davCmp != null
                && this.getAccessContext().requirePrivilege(this.davCmp.getAcl(), privilege, this.getCell().getUrl())) {
            return true;
        }
        return false;
    }

    /**
     * アクセス制御を行う.
     * @param ac アクセスコンテキスト
     * @param privilege アクセス可能な権限
     */
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        // ユニットユーザトークンチェック
        if (ac.isUnitUserToken()) {
            return;
        }

        // Basic認証できるリソースかをチェック
        this.accessContext.updateBasicAuthenticationStateForResource(null);

        // アクセス権チェック
        if (!this.hasPrivilege(ac, privilege)) {
            // トークンの有効性チェック
            // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(getAcceptableAuthScheme());
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw DcCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), getAcceptableAuthScheme());
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
