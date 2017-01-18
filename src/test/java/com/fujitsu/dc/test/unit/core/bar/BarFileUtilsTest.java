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
package com.fujitsu.dc.test.unit.core.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.bar.BarFileUtils;
import com.fujitsu.dc.test.categories.Unit;

/**
 * BarFileUtilsクラス用のユニットテストクラス.
 */
@Category({Unit.class })
public class BarFileUtilsTest {

    /**
     * rootprops_xmlのACL_URLがロールインスタンスURLに変換されること.
     */
    @Test
    public void rootprops_xmlのACL_URLがロールインスタンスURLに変換されること() {
        final String cellName = "installTargetCell";
        final String boxName = "installTargetBox";
        final String baseUrl = "https://baseserver/testcell1/__role/__/col1";
        final String targetUrl = "https://targetserver/";
        final String master = targetUrl + cellName + "/__role/" + boxName + "/col1/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl + "/");

            // ACL_URLの末尾に"/"がある場合
            Element res = BarFileUtils.convertToRoleInstanceUrl(element, targetUrl, cellName, boxName);
            assertEquals(master, res.getAttribute("xml:base"));

            // ACL_URLの末尾に"/"がない場合
            element.setAttribute("xml:base", baseUrl);
            res = BarFileUtils.convertToRoleInstanceUrl(element, targetUrl, cellName, boxName);
            assertEquals(master, res.getAttribute("xml:base"));
            return;
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

    /**
     * rootprops_xmlのACL_URLがURL形式ではない場合に例外がスローされること.
     */
    @Test
    public void rootprops_xmlのACL_URLがURL形式ではない場合に例外がスローされること() {
        final String cellName = "installTargetCell";
        final String boxName = "installTargetBox";
        final String baseUrl = "https/baseserver/testcell1/__role/__/col1/";
        final String targetUrl = "https://targetserver/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl + "/");

            BarFileUtils.convertToRoleInstanceUrl(element, targetUrl, cellName, boxName);
            fail("Unexpected exception");
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            String code = dce.getCode();
            assertEquals(DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

    /**
     * スキーマなしBoxへのインストールでACL_URLが正しくない場合にバリデートがfalseで返却されること.
     */
    @Test
    @Ignore
    public void スキーマなしBoxへのインストールでACL_URLが正しくない場合にバリデートがfalseで返却されること() {
        // URL形式ではない
        final String baseUrl = "https//baseserver/testcell1/__role/_a_/col1/";
        final String schemaUrl = null;
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl);

            assertFalse(BarFileUtils.aclNameSpaceValidate("90_rootprops.xml", element, schemaUrl));
            fail("Unexpected exception");
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            String code = dce.getCode();
            assertEquals(DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

    /**
     * スキーマなしBoxへのインストールでACL_URLのBoxが正しくない場合にバリデートがfalseで返却されること.
     */
    @Test
    @Ignore
    public void スキーマなしBoxへのインストールでACL_URLのBoxが正しくない場合にバリデートがfalseで返却されること() {
        // デフォルトBoxではない
        final String baseUrl = "https://baseserver/testcell1/__role/_a_/col1/";
        final String schemaUrl = null;
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl);

            assertFalse(BarFileUtils.aclNameSpaceValidate("90_rootprops.xml", element, schemaUrl));
            return;
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            String code = dce.getCode();
            assertEquals(DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

    /**
     * スキーマありBoxへのインストールでACL_URL中のCellがアプリセルではない場合にバリデートがfalseで返却されること.
     */
    @Test
    @Ignore
    public void スキーマありBoxへのインストールでACL_URL中のCellがアプリセルではない場合にバリデートがfalseで返却されること() {
        // アプリセルではない
        final String baseUrl = "https://baseserver/testcell1/__role/__/col1/";
        final String schemaUrl = "https://targetserver/testcellx/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl);

            assertFalse(BarFileUtils.aclNameSpaceValidate("90_rootprops.xml", element, schemaUrl));
            return;
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            String code = dce.getCode();
            assertEquals(DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

    /**
     * スキーマありBoxへのインストールでACL_URL中のBoxが不正の場合にバリデートがfalseで返却されること.
     */
    @Test
    @Ignore
    public void スキーマありBoxへのインストールでACL_URL中のBoxが不正の場合にバリデートがfalseで返却されること() {
        // アプリセルではない
        final String baseUrl = "https://targetserver/testcell1/__role/_x_/col1/";
        final String schemaUrl = "https://targetserver/testcell1/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl);

            assertFalse(BarFileUtils.aclNameSpaceValidate("90_rootprops.xml", element, schemaUrl));
            return;
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (DcCoreException dce) {
            String code = dce.getCode();
            assertEquals(DcCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("DcCoreExceptionが返却されない");
    }

}
