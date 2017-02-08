/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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

import io.personium.core.PersoniumUnitConfig;

import java.util.ArrayList;

/**
 * Plugin Load.
 * @author coe
 */
public class PluginList {
    private ArrayList<PluginInfo> plugins;

    /**
     * Constructor.
     */
    public PluginList() {
        load();
    }

    /**
     * load.
     */
    public void load() {
        PluginsLoader pl = new PluginsLoader();
        // get Config plugin path
        String path = PersoniumUnitConfig.getPluginPath();
        // get Plugins
        this.plugins = pl.getPlugins(path);
    }

    /**
     * getPlugins.
     * @return plugins
     */
    public ArrayList<PluginInfo> getPlugins() {
        return this.plugins;
    }

    /**
     * setPlugins.
     * @param plugins ArrayList
     */
    public void setPlugins(ArrayList<PluginInfo> plugins) {
        this.plugins = plugins;
    }

}
