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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginLoader;

/**
 * Plugins Loader.
 */
public class PluginsLoader {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(PluginsLoader.class);

    /** Plugin Classname loaded by default. */
    private String defaultLoadClassname = null;

    /**
     * Default constructor The plugin classname loaded by default is set same as PersoniumConfig.
     */
    public PluginsLoader() {
        this(PersoniumUnitConfig.getPluginDefaultLoadClassname());
    }

    /**
     * Constructor to specify plugin classname loaded by default.
     * @param defaultLoadedPluginClassname Plugin classname loaded by default
     */
    public PluginsLoader(String defaultLoadedPluginClassname) {
        if (defaultLoadedPluginClassname == null) {
            this.defaultLoadClassname = "io.personium.plugin.auth.oidc.OIDCPluginLoader";
        } else {
            this.defaultLoadClassname = defaultLoadedPluginClassname;
        }
    }

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
            String className = this.defaultLoadClassname;
            PluginFactory pf = new PluginFactory();
            Object obj = pf.loadDefaultPlugin(className);
            if (obj != null) {
                plugins.addAll(createPluginInfo(obj, className));
            }
        } catch (Exception ex) {
            log.info(ex.getMessage(), ex);
        }

        // Load Local File jar
        try {
            File f = new File(cpath);
            String[] files = f.list();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    ArrayList<PluginInfo> pinfo = null;
                    String fname = files[i];
                    if (!Files.isDirectory(Paths.get(cpath, fname)) && fname.endsWith(".jar")) {
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
                        plugins.addAll(pinfo);
                    }
                }
            }
        } catch (Exception ex) {
            log.info(ex.getMessage(), ex);
        }
        return plugins;
    }

    /**
     * This function wraps object with PluginInfo and returns them. If the arg `plugin` is instanceof AuthPluginLoader,
     * this function tries to load multiple AuthPlugins and wrap them. If other type of Object is passed, this function
     * assumes that `plugin` is AuthPlugin.
     * @param plugin Plugin
     * @param name String
     * @return pi PluginInfo
     */
    public ArrayList<PluginInfo> createPluginInfo(Object plugin, String name) {
        ArrayList<PluginInfo> results = new ArrayList<PluginInfo>();
        if (plugin instanceof AuthPluginLoader) {
            AuthPluginLoader loader = (AuthPluginLoader) plugin;
            for (Object obj : loader.loadInstances()) {
                results.add(new PluginInfo(obj, name));
            }
        } else if (plugin instanceof AuthPlugin) {
            results.add(new PluginInfo(plugin, name));
        } else {
            log.info("Loaded object is not instance of AuthPlugin nor AuthPluginLoader: " + name);
        }
        return results;
    }

}
