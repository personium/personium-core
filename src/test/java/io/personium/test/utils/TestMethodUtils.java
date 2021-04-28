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
package io.personium.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;

/**
 * 共通のテストメソッドを利用するユーティリティ.
 */
public class TestMethodUtils {

    private TestMethodUtils() {
    }

    /**
     * ACL後のPROPFINDの返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce aclを設定したリソースパス
     * @param list 設定したACLのMap
     * @param responseIndex responseタグ要素数
     * @param baseUrl ACLのbase:xml
     * @param requireSchamaAuthz チェックするrequireSchamaAuthzのレベル。nullだとチェック省略
     */
    public static void aclResponseTest(Element doc, String resorce,
            List<Map<String, List<String>>> list, int responseIndex, String baseUrl, String requireSchamaAuthz) {
        aclResponseTest(doc, resorce, list, null, responseIndex, baseUrl, requireSchamaAuthz);
    }

    /**
     * ACL後のPROPFINDの返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce aclを設定したリソースパス
     * @param list 設定したACLのMap
     * @param inheritedHrefList ace inherit href. If it's null it is check omitted.
     * @param responseIndex responseタグ要素数
     * @param baseUrl ACLのbase:xml
     * @param requireSchamaAuthz チェックするrequireSchamaAuthzのレベル。nullだとチェック省略
     */
    public static void aclResponseTest(Element doc, String resorce, List<Map<String, List<String>>> list,
            List<String> inheritedHrefList, int responseIndex, String baseUrl, String requireSchamaAuthz) {
        NodeList response = doc.getElementsByTagName("response");
        assertEquals(responseIndex, response.getLength());
        Element node = (Element) response.item(0);
        assertEquals(
                resorce,
                node.getElementsByTagName("href").item(0).getFirstChild().getNodeValue());
        assertEquals(
                "HTTP/1.1 200 OK",
                node.getElementsByTagName("status").item(0).getFirstChild().getNodeValue());
        NodeList tmp = node.getElementsByTagName("prop").item(0).getChildNodes();
        for (int i = 0; i < tmp.getLength(); i++) {
            Node child = tmp.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element childElement = (Element) child;
            if (!(childElement.getLocalName().equals("acl"))) {
                continue;
            }
            Acl acl = null;
            try {
                acl = ObjectIo.unmarshal(childElement, Acl.class);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            // xml:baseの確認
            assertEquals(baseUrl, acl.getBase());
            // requireSchemaAuthの確認
            if (requireSchamaAuthz != null) {
                assertEquals(requireSchamaAuthz, acl.getRequireSchemaAuthz());
            }
            List<Ace> aceList = acl.getAceList();
            // ACEの格納数を確認
            assertEquals(list.size(), aceList.size());
            // pricepalの確認
            for (int aceIndex = 0; aceIndex < aceList.size(); aceIndex++) {
                Ace ace = aceList.get(aceIndex);
                Map<String, List<String>> map = list.get(aceIndex);
                for (String key : map.keySet()) {
                    assertEquals(key, ace.getPrincipalHref());
                    List<String> grantList = map.get(key);
                    assertEquals(grantList.size(), ace.getGrantedPrivilegeList().size());
                    for (int grantIndex = 0; grantIndex < grantList.size(); grantIndex++) {
                        // grantの確認
                        assertEquals(grantList.get(grantIndex), ace.getGrantedPrivilegeList().get(grantIndex));
                    }
                }
                if (inheritedHrefList != null) {
                    // check inherited href.
                    assertEquals(aceList.size(), inheritedHrefList.size());
                    String inheritedHref = inheritedHrefList.get(aceIndex);
                    if (inheritedHref != null) {
                        assertEquals(inheritedHref, ace.getInheritedHref());
                    } else {
                        assertNull(ace.getInheritedHref());
                    }
                }
            }
        }
    }
}
