/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import java.io.File;
import java.util.ArrayList;

/**
 * Plugins Loader.
 */
public class PluginsLoader {

    static final String PLUGIN_AUTH_GOOGLE =
           "io.personium.plugin.auth.oidc.GoogleIdTokenAuthPlugin";

    /**
     * Return together an instance of the plug-in class to an ArrayList.
     * @param cpath String
     * @return ArrayList
     */
    public ArrayList<PluginInfo> loadPlugins(String cpath) {
        ArrayList<PluginInfo> plugins = new ArrayList<PluginInfo>();

        // Load jar(maven) specified in pom.xml
        try {
            // personium-plugins original
            String className = PLUGIN_AUTH_GOOGLE;
            PluginFactory pf = new PluginFactory();
            Object obj = pf.loadDefaultPlugin(className);
            if (obj != null) {
                plugins.add(createPluginInfo(obj, className));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        // Load Local File jar
        try {
            File f = new File(cpath);
            String[] files = f.list();
            for (int i = 0; i < files.length; i++) {
                PluginInfo pinfo = null;
                String fname = files[i];
                if (fname.endsWith(".jar")) {
                    // File *.jar
                    File file = new File(cpath + File.separator + files[i]);
                    if (file.isFile()) {
                        PluginFactory pf = new PluginFactory();
                        Object obj = pf.getJarPlugin(cpath, fname);
                        if (obj != null) {
                            pinfo = createPluginInfo(obj, obj.getClass().getName());
                        }
                    }
                } else {
                    // Directory
                    PluginFactory pf = new PluginFactory();
                    Object obj = pf.getDirPlugin(cpath, fname);
                    pinfo = createPluginInfo(obj, obj.getClass().getName());
                }
                if (pinfo != null) {
                    plugins.add(pinfo);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return plugins;
    }

    /**
     * createPluginInfo.
     * @param plugin Plugin
     * @param name String
     * @return pi PluginInfo
     */
    public PluginInfo createPluginInfo(Object plugin, String name) {
        return new PluginInfo(plugin, name);
    }

}
