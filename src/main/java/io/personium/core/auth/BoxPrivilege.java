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

import java.util.HashMap;
import java.util.Map;

/**
 * Privilege of box.
 */
public final class BoxPrivilege extends Privilege {

    /**
     * Constructor.
     * @param name Name
     * @param accessType Access type
     * @param parent Parent privilege
     */
    BoxPrivilege(String name, String accessType, Privilege parent) {
        super(name, accessType, parent);
    }

    /** すべての権限. */
    public static final BoxPrivilege ALL = new BoxPrivilege("all", ACCESS_TYPE_ALL, CellPrivilege.ROOT);
    /** リード権限. */
    public static final BoxPrivilege READ = new BoxPrivilege("read", ACCESS_TYPE_READ, ALL);
    /** 属性リード権限.READ権限に含まれます. */
    public static final BoxPrivilege READ_PROPERTIES = new BoxPrivilege("read-properties", ACCESS_TYPE_READ, READ);
    /** ACLリード権限. READ権限に含まれず、ALL権限にのみ含まれます. */
    public static final BoxPrivilege READ_ACL = new BoxPrivilege("read-acl", ACCESS_TYPE_READ, ALL);
    /** ライト権限.ALL権限に含まれます. */
    public static final BoxPrivilege WRITE = new BoxPrivilege("write", ACCESS_TYPE_WRITE, ALL);
    /** ACLライト権限. WRITE権限に含まれまれず、ALLにのみ含まれます。 */
    public static final BoxPrivilege WRITE_ACL = new BoxPrivilege("write-acl", ACCESS_TYPE_WRITE, ALL);
    /** BIND権限. WRITE権限に含まれます. */
    public static final BoxPrivilege BIND = new BoxPrivilege("bind", ACCESS_TYPE_WRITE, WRITE);
    /** UNBIND権限. WRITE権限に含まれます. */
    public static final BoxPrivilege UNBIND = new BoxPrivilege("unbind", ACCESS_TYPE_WRITE, WRITE);
    /** 内容ライト権限. WRITE権限に含まれます. */
    public static final BoxPrivilege WRITE_CONTENT = new BoxPrivilege("write-content", ACCESS_TYPE_WRITE, WRITE);
    /** 属性ライト権限. WRITE権限に含まれます. */
    public static final BoxPrivilege WRITE_PROPERTIES = new BoxPrivilege("write-properties", ACCESS_TYPE_WRITE, WRITE);
    /** サービス実行権限.ALL権限に含まれます. */
    public static final BoxPrivilege EXEC = new BoxPrivilege("exec", ACCESS_TYPE_EXEC, ALL);
    /** スキーマ変更権限.ALL権限に含まれます. */
    public static final BoxPrivilege ALTER_SCHEMA = new BoxPrivilege("alter-schema", ACCESS_TYPE_WRITE, ALL);

    static Map<String, BoxPrivilege> map = new HashMap<String, BoxPrivilege>();

    /**
     * BoxレベルPrivilege一覧を取得する.
     * @return BoxレベルPrivilege一覧
     */
    public static Map<String, BoxPrivilege> getPrivilegeMap() {
        return map;
    }

    static {
        register(ALL);
        register(READ);
        register(READ_PROPERTIES);
        register(READ_ACL);
        register(WRITE);
        register(WRITE_ACL);
        register(BIND);
        register(UNBIND);
        register(WRITE_CONTENT);
        register(WRITE_PROPERTIES);
        register(EXEC);
        register(ALTER_SCHEMA);
    }

    private static void register(final BoxPrivilege p) {
        map.put(p.getName(), p);
    }
}
