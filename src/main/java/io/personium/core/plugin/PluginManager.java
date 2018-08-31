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
package io.personium.core.plugin;

import java.util.ArrayList;
import java.util.List;

import io.personium.plugin.base.auth.AuthPlugin;

/**
 * Plugin Load.
 */
public class PluginManager {

    private PluginList pl;

    /**
     * Constructor.
     */
    public PluginManager() {
        this.pl = new PluginList();
    }

    /**
     * getPluginList.
     * @return pl PluginList
     */
    public PluginList getPluginList() {
        return pl;
    }

    /**
     * getTypePluginInfo.
     *Get target plug-in.
     * @param type String
     * @return pi PluginInfo
     */
    public List<PluginInfo> getPluginsByType(String type) {
        List<PluginInfo> typeList = new ArrayList<PluginInfo>();
        for (int i = 0; i < pl.getPlugins().size(); i++) {
            PluginInfo pi = pl.getPlugins().get(i);
            if (type.endsWith(pi.getType())) {
                typeList.add(pi);
            }
        }
        return typeList;
    }

    /**
     * getGrantTypePluginInfo.
     *Get target plug-in.
     * @param grant String
     * @return pi PluginInfo
     */
    public PluginInfo getPluginsByGrantType(String grant) {
        PluginInfo pi = null;
        for (int i = 0; i < pl.getPlugins().size(); i++) {
            pi = pl.getPlugins().get(i);
            if (pi != null) {
                String gtype = getGrantType(pi);
                //Perfect matching
                if (gtype.equals(grant)) {
                    return pi;
                }
            }
        }
        return null;
    }

    /**
     * getGrantType.
     * @param pi PluginInfo
     * @return type String
     */
    public String getGrantType(PluginInfo pi) {
        AuthPlugin ap = (AuthPlugin) pi.getObj();
        return ap.getGrantType();
    }

    /**
     * size.
     * @return size
     */
    public int size() {
        return this.pl.getPlugins().size();
    }
}
