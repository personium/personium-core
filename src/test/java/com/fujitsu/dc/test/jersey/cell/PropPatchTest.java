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
package com.fujitsu.dc.test.jersey.cell;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * CellレベルPROPPATCHのテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class PropPatchTest extends JerseyTest {

    static final String TEST_CELL1 = "testcell1";
    static final String DEPTH = "0";

    /**
     * コンストラクタ.
     */
    public PropPatchTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * CellレベルPROPPATCHのテスト.
     */
    @Test
    public final void CellレベルPROPPATCHのテスト() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String author1 = "Test User1";
        String hoge1 = "Hoge";
        String author2 = "Author1 update";
        String hoge2 = "Fuga";
        String authorKey = "Author";
        String hogeKey = "hoge";
        TResponse tresponseWebDav = null;

        try {

            // PROPPATCH設定実行
            DavResourceUtils.setProppatch(TEST_CELL1, token, HttpStatus.SC_MULTI_STATUS, author1, hoge1);

            // プロパティ変更の確認
            tresponseWebDav = CellUtils.propfind(TEST_CELL1, token, DEPTH, HttpStatus.SC_MULTI_STATUS);
            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(authorKey, author1);
            map.put(hogeKey, hoge1);
            proppatchResponseTest(root, resorce, map);

            // プロパティの変更
            DavResourceUtils.setProppatch(TEST_CELL1, token, HttpStatus.SC_MULTI_STATUS, author2, hoge2);

            // プロパティの変更確認
            tresponseWebDav = CellUtils.propfind(TEST_CELL1, token, DEPTH, HttpStatus.SC_MULTI_STATUS);
            Element root2 = tresponseWebDav.bodyAsXml().getDocumentElement();
            HashMap<String, String> map2 = new HashMap<String, String>();
            map.put(authorKey, author2);
            map.put(hogeKey, hoge2);
            proppatchResponseTest(root2, resorce, map2);

        } finally {
            // プロパティの削除
            DavResourceUtils.resetProppatch(TEST_CELL1, token);
        }
    }

    /**
     * CellレベルPROPPATCHのテスト_Cellの更新あり.
     */
    @Test
    public final void CellレベルPROPPATCHのテスト_Cellの更新あり() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String author1 = "Test User1";
        String hoge1 = "Hoge";
        String authorKey = "Author";
        String hogeKey = "hoge";
        TResponse tresponseWebDav = null;

        try {
            // PROPPATCH設定実行
            DavResourceUtils.setProppatch(TEST_CELL1, token, HttpStatus.SC_MULTI_STATUS, author1, hoge1);

            // Cellの更新
            String updateCellName = TEST_CELL1;
            CellUtils.update(TEST_CELL1, updateCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // プロパティ変更の確認
            tresponseWebDav = CellUtils.propfind(TEST_CELL1, token, DEPTH, HttpStatus.SC_MULTI_STATUS);
            Element root = tresponseWebDav.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(authorKey, author1);
            map.put(hogeKey, hoge1);
            proppatchResponseTest(root, resorce, map);
        } finally {
            // プロパティの削除
            DavResourceUtils.resetProppatch(TEST_CELL1, token);
        }
    }

        /**
     * CellレベルPROPPATCHでpropタグが不正な場合の更新テスト.
     */
    @Test
    public final void CellレベルPROPPATCHでpropタグが不正な場合の更新テスト() {

        String token = AbstractCase.MASTER_TOKEN_NAME;
        String author1 = "Test User1";
        String hoge1 = "Hoge";

        // PROPPATCH設定実行
        DavResourceUtils.setProppatchSetPropKey(TEST_CELL1, token, HttpStatus.SC_BAD_REQUEST, "props", author1, hoge1);

    }

    /**
     * CellレベルPROPPATCHでpropタグが不正な場合の削除テスト.
     */
    @Test
    public final void CellレベルPROPPATCHでpropタグが不正な場合の削除テスト() {

        String token = AbstractCase.MASTER_TOKEN_NAME;

        // PROPPATCH設定実行
        DavResourceUtils.resetProppatchSetPropKey(TEST_CELL1, token, HttpStatus.SC_BAD_REQUEST, "props");

    }

    /**
     * PROPPATCHの返却値のチェック関数.
     * @param doc 解析するXMLオブジェクト
     * @param resorce PROPPATCHを設定したリソースパス
     * @param map チェックするプロパティのKeyValue KeyとValueに値を入れれば値があることのチェック ValueをnullにするとKeyが無いことのチェック（removeの確認に使う）
     */
    private void proppatchResponseTest(Element doc, String resorce, Map<String, String> map) {
        StringBuffer sb = new StringBuffer(resorce);
        sb.deleteCharAt(resorce.length() - 1);

        NodeList response = doc.getElementsByTagName("response");
        assertEquals(1, response.getLength());
        Element node = (Element) response.item(0);
        assertEquals(
                sb.toString(),
                node.getElementsByTagName("href").item(0).getFirstChild().getNodeValue());
        assertEquals(
                "HTTP/1.1 200 OK",
                node.getElementsByTagName("status").item(0).getFirstChild().getNodeValue());

        for (Iterator<String> it = map.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = map.get(key);
            String textContext = null;
            NodeList tmp = node.getElementsByTagName("prop").item(0).getChildNodes();
            for (int i = 0; i < tmp.getLength(); i++) {
                Node child = tmp.item(i);
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    if (childElement.getLocalName().equals(key)) {
                        textContext = childElement.getTextContent();
                        break;
                    }
                }
            }
            assertEquals(value, textContext);
        }
    }

}
