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
package io.personium.core.auth;


/**
 * WebDAVACLのPrivilege.
 */
public abstract class Privilege {
    private String name;
    /**
     * @return 権限名
     */
    public String getName() {
        return name;
    }

    /**
     * @return 親権限
     */
    public Privilege getParent() {
        return parent;
    }

    private Privilege parent;

    /**
     * コンストラクタ.
     * @param name Privilege名
     */
    Privilege(final String name) {
        this.name = name;
    }

    /**
     * コンストラクタ.
     * @param name Privilege名
     * @param parent 親Privilege
     */
    Privilege(final String name, final Privilege parent) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * このPrivilegeが引数で指定されたPrivilegeを含むかどうかを返す.
     * @param priv 対象Privilege
     * @return 引数で指定されたPrivilegeを含む場合真
     */
    public boolean includes(final Privilege priv) {
        if (this == priv) {
            return true;
        }
        if (priv.parent == null) {
            return false;
        }
        return this.includes(priv.parent);
    }

    /**
     * 対応するPrivilegeクラスのPrivilegeを返却.
     * @param <T> 戻り値のクラス
     * @param clazz Privilegeが属するクラス
     * @param privilegeName 取得するPrivilegeの名前
     * @return .
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz, String privilegeName) {
        if (clazz == CellPrivilege.class) {
            return (T) CellPrivilege.map.get(privilegeName);
        } else {
            return (T) BoxPrivilege.map.get(privilegeName);
        }
    }
}
