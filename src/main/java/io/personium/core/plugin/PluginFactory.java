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
package io.personium.core.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin Factory.
 */
public class PluginFactory {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(PluginFactory.class);

    /** instance. */
    private static PluginFactory instance = new PluginFactory();
    private JarFile jar;
    private URLClassLoader ucl;
    private BufferedReader in;
    private URLClassLoader ucl2;

    /**
     * Constructor (instance prohibited).
     */
    PluginFactory() {
    }

    /**
     * It gets an instance.
     * @return instance
     */
    public static PluginFactory getInstance() {
        return instance;
    }

    /**
     * Get Jar File Plugin.
     * @param cpath root
     * @param fname file name
     * @return object jar
     */
    public Object getJarPlugin(String cpath, String fname) {
        Object plugin = null;
        try {
            File file = new File(cpath + File.separator + fname);
            jar = new JarFile(file);
            // get Manifest info.
            Manifest mf = jar.getManifest();
            Attributes att = mf.getMainAttributes();
            String cname = att.getValue("Plugin-Class");

            try {
                // get class
                URL url = file.getCanonicalFile().toURI().toURL();
                ucl = new URLClassLoader(new URL[] {url}, getClass().getClassLoader());

                Class<?> clazz = ucl.loadClass(cname);
                if (clazz != null) {
                    plugin = (Object) clazz.newInstance();
                }
                log.info("Plugin Factory load jar file...... " + cname);
            } catch (ClassNotFoundException e) {
                log.info("ClassNotFoundException: class name = " + cname, e);
            } catch (NoClassDefFoundError e) {
                log.info("NoClassDefFoundError: class name = " + cname, e);
            } catch (NullPointerException e) {
                log.info("NullPointerExecption: class name = " + cname, e);
            } catch (SecurityException e) {
                log.info("SecurityException: class name = " + cname, e);
            } catch (ExceptionInInitializerError e) {
                log.info("ExceptionInInitializerError: class name = " + cname, e);
            } catch (InstantiationException e) {
                log.info("Plugin Factory InstantiationException : Instance can not be created class name = " + cname,
                        e);
            } catch (IllegalArgumentException e) {
                log.info("IllegalArgumentException: class name = " + cname, e);
            }
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return null;
        }
        return plugin;
    }

    /**
     * Get Directory Plugin.
     * @param cpath root
     * @param dname directory name
     * @return object
     */
    public Object getDirPlugin(String cpath, String dname) {
        Object plugin = null;
        try {
            // MANIFEST.MF
            File mfile = new File(cpath + File.separator + dname + File.separator + "META-INF/MANIFEST.MF");
            in = new BufferedReader(new FileReader(mfile));
            in.readLine();
            String line = in.readLine();
            String cname = line.replace("Plugin-Class: ", "");

            // get class
            String cfile = cname.replace(".", File.separator) + ".class";
            File file = new File("file:/" + cpath + File.separator + dname + File.separator + cfile);
            URL url = new URL(file.toString());
            ucl2 = new URLClassLoader(new URL[] {url});
            Class<?> objFile = ucl2.loadClass(cname);
            if (objFile != null) {
                plugin = (Object) objFile.newInstance();
                log.info("Plugin Factory load directory..... " + cname);
            }

        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
        return plugin;
    }

    /**
     * Loading default auth plugin which is included in personium-plugin.jar.
     * @param name default class name
     * @return obj
     */
    public Object loadDefaultPlugin(String name) {
        Object obj = null;

        try {
            Class<?> clazz;
            clazz = Class.forName(name);
            obj = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            // Class does not exist
            log.info("ClassNotFoundException: class name = " + name, e);
        } catch (InstantiationException e) {
            // Instance can not be created
            log.info("InstantiationException: class name = " + name, e);
        } catch (IllegalAccessException e) {
            // Invocation: Access violation, protected
            log.info("IllegalAccessException: class name = " + name, e);
        }
        return obj;
    }
}
