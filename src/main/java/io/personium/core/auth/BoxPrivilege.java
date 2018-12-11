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

    /** All authority.*/
    public static final BoxPrivilege ALL = new BoxPrivilege("all", ACCESS_TYPE_ALL, CellPrivilege.ROOT);
    /** Read authority.*/
    public static final BoxPrivilege READ = new BoxPrivilege("read", ACCESS_TYPE_READ, ALL);
    /** Attribute read authority, included in READ authority.*/
    public static final BoxPrivilege READ_PROPERTIES = new BoxPrivilege("read-properties", ACCESS_TYPE_READ, READ);
    /** ACL read authority, not included in READ authority, it is included only in ALL authority.*/
    public static final BoxPrivilege READ_ACL = new BoxPrivilege("read-acl", ACCESS_TYPE_READ, ALL);
    /** Write permission, included in ALL authority.*/
    public static final BoxPrivilege WRITE = new BoxPrivilege("write", ACCESS_TYPE_WRITE, ALL);
    /** ACL write authority. Not included in WRITE authority, it is included only in ALL.*/
    public static final BoxPrivilege WRITE_ACL = new BoxPrivilege("write-acl", ACCESS_TYPE_WRITE, ALL);
    /** BIND authority, included in WRITE authority.*/
    public static final BoxPrivilege BIND = new BoxPrivilege("bind", ACCESS_TYPE_WRITE, WRITE);
    /** UNBIND authority. Included in WRITE authority.*/
    public static final BoxPrivilege UNBIND = new BoxPrivilege("unbind", ACCESS_TYPE_WRITE, WRITE);
    /** Contents Write permission, included in WRITE authority.*/
    public static final BoxPrivilege WRITE_CONTENT = new BoxPrivilege("write-content", ACCESS_TYPE_WRITE, WRITE);
    /** Attribute write authority, included in WRITE authority.*/
    public static final BoxPrivilege WRITE_PROPERTIES = new BoxPrivilege("write-properties", ACCESS_TYPE_WRITE, WRITE);
    /** Execute service authority, included in ALL authority.*/
    public static final BoxPrivilege EXEC = new BoxPrivilege("exec", ACCESS_TYPE_EXEC, ALL);
    /** Schema change authority. Included in ALL authority.*/
    public static final BoxPrivilege ALTER_SCHEMA = new BoxPrivilege("alter-schema", ACCESS_TYPE_WRITE, ALL);
    /** SEND authority, included in ALL authority.*/
    public static final BoxPrivilege SEND = new BoxPrivilege("send", ACCESS_TYPE_SEND, ALL);
    /** RECEIVE authority, included in ALL authority.*/
    public static final BoxPrivilege RECEIVE = new BoxPrivilege("receive", ACCESS_TYPE_RECEIVE, ALL);

    static Map<String, BoxPrivilege> map = new HashMap<String, BoxPrivilege>();

    /**
     * Box Level Get Privilege List.
     * @return Box level Privilege list
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
        register(SEND);
        register(RECEIVE);
    }

    private static void register(final BoxPrivilege p) {
        map.put(p.getName(), p);
    }
}
