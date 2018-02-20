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
 * Privilege of WebDAV ACL.
 */
public abstract class Privilege {

    // Access type is provisional.
    // Only READ is used in the current(core-1.6.4) process.
    /** Access type : READ. */
    public static final String ACCESS_TYPE_READ = "read";
    /** Access type : WRITE. */
    public static final String ACCESS_TYPE_WRITE = "write";
    /** Access type : EXEC. */
    public static final String ACCESS_TYPE_EXEC = "exec";
    /** Access type : ALL. */
    public static final String ACCESS_TYPE_ALL = "all";

    /** Name. */
    private String name;
    /** Access type. */
    private String accessType;
    /** Parent privilege. */
    private Privilege parent;

    /**
     * Constructor.
     * @param name Name
     * @param accessType Access type
     * @param parent Parent privilege
     */
    Privilege(String name, String accessType, Privilege parent) {
        this.name = name;
        this.accessType = accessType;
        this.parent = parent;
    }

    /**
     * Get name.
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get access type.
     * @return Access type
     */
    public String getAccessType() {
        return accessType;
    }

    /**
     * Get parent.
     * @return Parent privilege
     */
    public Privilege getParent() {
        return parent;
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
