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
package io.personium.core.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Unit;

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
        final String boxName = "installTargetBox";
        final String baseUrl = "https://baseserver/testcell1/__role/__";
        final String cellUrl = "https://targetserver/installTargetCell/";
        final String master = cellUrl + "__role/" + boxName + "/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl + "/");

            // ACL_URLの末尾に"/"がある場合
            Element res = BarFileUtils.convertToRoleInstanceUrl(element, cellUrl, boxName);
            assertEquals(master, res.getAttribute("xml:base"));

            // ACL_URLの末尾に"/"がない場合
            element.setAttribute("xml:base", baseUrl);
            res = BarFileUtils.convertToRoleInstanceUrl(element, cellUrl, boxName);
            assertEquals(master, res.getAttribute("xml:base"));
            return;
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * rootprops_xmlのACL_URLがURL形式ではない場合に例外がスローされること.
     */
    @Test
    public void rootprops_xmlのACL_URLがURL形式ではない場合に例外がスローされること() {
        final String boxName = "installTargetBox";
        final String baseUrl = "https/baseserver/testcell1/__role/__/col1/";
        final String cellUrl = "https://targetserver/installTargetCell/";
        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder;
            docbuilder = dbfactory.newDocumentBuilder();
            Document document = docbuilder.newDocument();
            Element element = document.createElement("acl");
            element.setAttribute("xml:base", baseUrl + "/");

            BarFileUtils.convertToRoleInstanceUrl(element, cellUrl, boxName);
            fail("Unexpected exception");
        } catch (ParserConfigurationException e) {
            fail("DOM Parsing Error: " + e.getMessage());
        } catch (PersoniumCoreException dce) {
            String code = dce.getCode();
            assertEquals(PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.getCode(), code);
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

}
