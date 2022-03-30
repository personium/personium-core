/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.plugin;

import io.personium.plugin.base.Plugin;

/**
 * plugin Info Class.
 */
public class PluginInfo {
    private Object obj;
    private String type;
    private String name;

    /**
     * crate.
     * @param obj Object
     * @param name String
     */
    public PluginInfo(Object obj, String name) {
        this.obj = obj;
        this.name = name;
        this.type = ((Plugin) obj).getType();
    }

    /**
     * get Object.
     * @return obj Object
     */
    public Object getObj() {
        return this.obj;
    }

    /**
     * get Name.
     * @return name String
     */
    public String getName() {
        return name;
    }

    /**
     * get Type.
     * @return type String
     */
    public String getType() {
        return type;
    }
}
