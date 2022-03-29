/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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
 * Privilege of cell.
 */
public class CellPrivilege extends Privilege {

    /**
     * Constructor.
     * @param name Name
     * @param accessType Access type
     * @param parent Parent privilege
     */
    CellPrivilege(String name, String accessType, Privilege parent) {
        super(name, accessType, parent);
    }

    /** All authority.*/
    public static final CellPrivilege ROOT = new CellPrivilege("root", ACCESS_TYPE_ALL, null);
    /** Account, Role, extRole group operation authority.*/
    public static final CellPrivilege AUTH = new CellPrivilege("auth", ACCESS_TYPE_WRITE, ROOT);
    /** AUTH group read authority.*/
    public static final CellPrivilege AUTH_READ = new CellPrivilege("auth-read", ACCESS_TYPE_READ, AUTH);
    /** ReceivedMessage, SentMessage group operation authority.*/
    public static final CellPrivilege MESSAGE = new CellPrivilege("message", ACCESS_TYPE_WRITE, ROOT);
    /** MESSAGE group read permission.*/
    public static final CellPrivilege MESSAGE_READ = new CellPrivilege("message-read", ACCESS_TYPE_READ, MESSAGE);
    /** event, log group operation authority.*/
    public static final CellPrivilege EVENT = new CellPrivilege("event", ACCESS_TYPE_WRITE, ROOT);
    /** EVENT group read permission.*/
    public static final CellPrivilege EVENT_READ = new CellPrivilege("event-read", ACCESS_TYPE_READ, EVENT);
    /** log Operation authority.*/
    public static final CellPrivilege LOG = new CellPrivilege("log", ACCESS_TYPE_WRITE, ROOT);
    /** log read authority.*/
    public static final CellPrivilege LOG_READ = new CellPrivilege("log-read", ACCESS_TYPE_READ, LOG);
    /** relation, extCell group operation authority.*/
    public static final CellPrivilege SOCIAL = new CellPrivilege("social", ACCESS_TYPE_WRITE, ROOT);
    /** SOCIAL group read permission.*/
    public static final CellPrivilege SOCIAL_READ = new CellPrivilege("social-read", ACCESS_TYPE_READ, SOCIAL);
    /** Box group operation authority.*/
    public static final CellPrivilege BOX = new CellPrivilege("box", ACCESS_TYPE_WRITE, ROOT);
    /** BOX group read permission.*/
    public static final CellPrivilege BOX_READ = new CellPrivilege("box-read", ACCESS_TYPE_READ, BOX);
    /** BOX group bar - install authority.*/
    public static final CellPrivilege BOX_BAR_INSTALL = new CellPrivilege("box-install", ACCESS_TYPE_WRITE, BOX);
    /** ACL group operation authority.*/
    public static final CellPrivilege ACL = new CellPrivilege("acl", ACCESS_TYPE_WRITE, ROOT);
    /** ACL group read permission.*/
    public static final CellPrivilege ACL_READ = new CellPrivilege("acl-read", ACCESS_TYPE_READ, ACL);
    /** PROPFIND authority.*/
    public static final CellPrivilege PROPFIND = new CellPrivilege("propfind", ACCESS_TYPE_READ, ROOT);
    /** Rule operation privilege. */
    public static final CellPrivilege RULE = new CellPrivilege("rule", ACCESS_TYPE_WRITE, ROOT);
    /** Rule read privilege. */
    public static final CellPrivilege RULE_READ = new CellPrivilege("rule-read", ACCESS_TYPE_READ, RULE);
    /** Sign privilege. */
    public static final CellPrivilege SIGN = new CellPrivilege("sign", ACCESS_TYPE_EXEC, ROOT);

    static Map<String, CellPrivilege> map = new HashMap<String, CellPrivilege>();

    /**
     * Cell Level Get Privilege list.
     * @return Cell level Privilege list
     */
    public static Map<String, CellPrivilege> getPrivilegeMap() {
        return map;
    }

    static {
        register(ROOT);
        register(AUTH);
        register(AUTH_READ);
        register(MESSAGE);
        register(MESSAGE_READ);
        register(EVENT);
        register(EVENT_READ);
        register(LOG);
        register(LOG_READ);
        register(SOCIAL);
        register(SOCIAL_READ);
        register(BOX);
        register(BOX_READ);
        register(BOX_BAR_INSTALL);
        register(ACL);
        register(ACL_READ);
        register(PROPFIND);
        register(RULE);
        register(RULE_READ);
        register(SIGN);
    }

    private static void register(final CellPrivilege p) {
        map.put(p.getName(), p);
    }
}
