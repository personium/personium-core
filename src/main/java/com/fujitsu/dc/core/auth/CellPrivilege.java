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
package com.fujitsu.dc.core.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * WebDAVACLのPrivilege.
 */
public final class CellPrivilege extends Privilege {
    /**
     * コンストラクタ.
     * @param name Privilege名
     */
    CellPrivilege(final String name) {
        super(name);
    }

    /**
     * コンストラクタ.
     * @param name Privilege名
     * @param parent 親Privilege
     */
    CellPrivilege(final String name, final CellPrivilege parent) {
        super(name, parent);
    }

    /**
     * すべての権限.
     */
    public static final CellPrivilege ROOT = new CellPrivilege("root");
    /**
     * Account, Role, extRole グループ操作権限.
     */
    public static final CellPrivilege AUTH = new CellPrivilege("auth", ROOT);
    /**
     * AUTHグループ read権限.
     */
    public static final CellPrivilege AUTH_READ = new CellPrivilege("auth-read", AUTH);
    /**
     * ReceivedMessage, SentMessage グループ操作権限.
     */
    public static final CellPrivilege MESSAGE = new CellPrivilege("message", ROOT);
    /**
     * MESSAGEグループ read権限.
     */
    public static final CellPrivilege MESSAGE_READ = new CellPrivilege("message-read", MESSAGE);
    /**
     * event, log グループ操作権限.
     */
    public static final CellPrivilege EVENT = new CellPrivilege("event", ROOT);
    /**
     * EVENTグループ read権限.
     */
    public static final CellPrivilege EVENT_READ = new CellPrivilege("event-read", EVENT);
    /**
     * log 操作権限.
     */
    public static final CellPrivilege LOG = new CellPrivilege("log", ROOT);
    /**
     * log read権限.
     */
    public static final CellPrivilege LOG_READ = new CellPrivilege("log-read", LOG);
    /**
     * relation, extCell グループ操作権限.
     */
    public static final CellPrivilege SOCIAL = new CellPrivilege("social", ROOT);
    /**
     * SOCIALグループ read権限.
     */
    public static final CellPrivilege SOCIAL_READ = new CellPrivilege("social-read", SOCIAL);
    /**
     * Box グループ操作権限.
     */
    public static final CellPrivilege BOX = new CellPrivilege("box", ROOT);
    /**
     * BOXグループ read権限.
     */
    public static final CellPrivilege BOX_READ = new CellPrivilege("box-read", BOX);
    /**
     * BOXグループ bar-install権限.
     */
    public static final CellPrivilege BOX_BAR_INSTALL = new CellPrivilege("box-install", BOX);
    /**
     * ACL グループ操作権限.
     */
    public static final CellPrivilege ACL = new CellPrivilege("acl", ROOT);
    /**
     * ACLグループ read権限.
     */
    public static final CellPrivilege ACL_READ = new CellPrivilege("acl-read", ACL);
    /**
     * PROPFIND権限.
     */
    public static final CellPrivilege PROPFIND = new CellPrivilege("propfind", ROOT);

    static Map<String, CellPrivilege> map = new HashMap<String, CellPrivilege>();

    /**
     * CellレベルPrivilege一覧を取得する.
     * @return CellレベルPrivilege一覧
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
    }

    private static void register(final CellPrivilege p) {
        map.put(p.getName(), p);
    }
}
