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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Plugin Factory.
 * @author fjqs
 */
public class PluginFactory {
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
            File file = new File(cpath + "/" + fname);
            jar = new JarFile(file);
            // get Manifest info.
            Manifest mf = jar.getManifest();
            Attributes att = mf.getMainAttributes();
            String cname = att.getValue("Plugin-Class");

            try {
	            // get class
	            URL url = file.getCanonicalFile().toURI().toURL();
	            ucl = new URLClassLoader(new URL[] {url});

	            System.out.println(ucl.getURLs().toString());
                Class<?> clazz = ucl.loadClass(cname);
                if (clazz != null) {
                  	plugin = (Object) clazz.newInstance();
                }
                System.out.println("Plugin Factory load jar file...... " + cname);
            } catch (ClassNotFoundException e) {
                System.out.println("Plugin Factory ClassNotFoundException : class name = " + cname);
                e.printStackTrace();
            } catch (NoClassDefFoundError e) {
                System.out.println("Plugin Factory NoClassDefFoundError : class name = " + cname);
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (ExceptionInInitializerError e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                System.out.println("Plugin Factory InstantiationException : Instance can not be created class name = " + cname);
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            File mfile = new File(cpath + File.separator + dname +  File.separator + "META-INF/MANIFEST.MF");
            in = new BufferedReader(new FileReader(mfile));
            in.readLine();
            String line = in.readLine();
            String cname = line.replace("Plugin-Class: ", "");

            // get class
            String cfile = cname.replace(".", "/") + ".class";
            File file = new File("file:/" + cpath + File.separator + dname + File.separator + cfile);
            URL url = new URL(file.toString());
            ucl2 = new URLClassLoader(new URL[] {url});
            Class<?> objFile = ucl2.loadClass(cname);
            if (objFile != null) {
                plugin = (Object) objFile.newInstance();
                System.out.println("Plugin Factory load directory..... " + cname);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return plugin;
    }
}
