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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.assocend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.ctl.AssociationEnd;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AssociationEnd登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndListLinkTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public AssociationEndListLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * AssociationEndのlinkを一覧取得してレスポンスコードが200であること.
     */
    @Test
    public final void AssociationEndのlinkを一覧取得してレスポンスコードが200であること() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            TResponse resp = createAssociationEnd(linkName, linkEntityTypeName);

            createLink(entityTypeName, linkEntityTypeName, name, linkName);

            // $links一覧取得
            TResponse response = AssociationEndUtils.getAssociationEndLinkList(token, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName, name, HttpStatus.SC_OK);
            ODataCommon.checkCommonResponseHeader(response);
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(resp.getLocationHeader());
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), uri);

            // NP経由一覧取得
            response = AssociationEndUtils.getAssociationEndNpLinkList(token, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName, name, HttpStatus.SC_OK);
            ODataCommon.checkCommonResponseHeader(response);
            Map<String, String> uriMap = new HashMap<String, String>();
            uriMap.put("id", UrlUtils.associationEnd(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, linkName, entityTypeName));
            String nameSpace = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + AssociationEnd.EDM_TYPE_NAME;

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(linkName, additionalprop);
            additionalprop.put("Name", linkName);
            ODataCommon
            .checkResponseBodyList(response.bodyAsJson(), uriMap, nameSpace, additional, "__id", null, null);

        } finally {
            // AssociationEndのlink解除
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, key, navKey, -1);
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndと紐付くAssociationEndが存在しない場合AssociationEndのlinkを一覧取得してレスポンスコードが200であること.
     */
    @Test
    public final void AssociationEndと紐付くAssociationEndが存在しない場合AssociationEndのlinkを一覧取得してレスポンスコードが200であること() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            TResponse response = AssociationEndUtils.getAssociationEndLinkList(token, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName, name, HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), uri);

        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
        }
    }

    /**
     * AssociationEndに存在しないEntityType名を指定してlinkを一覧取得してレスポンスコードが404であること.
     */
    @Test
    public final void AssociationEndに存在しないEntityType名を指定してlinkを一覧取得してレスポンスコードが404であること() {
        String entityTypeName = "dummy";
        String name = "AssoEnd";

        // link作成
        try {
            // AssociationEndの作成
            createAssociationEnd(name, "Product");

            Http.request("box/associationEnd-listLink.txt")
                    .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("entityTypeName", entityTypeName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", "application/json")
                    .with("name", name)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .debug();

        } finally {
            // AssociationEndの削除
            deleteAssociationEnd(name, "Product");
        }
    }

    private TResponse createLink(String entityTypeName, String linkEntityTypeName, String name, String linkName) {
        return Http.request("box/associationEnd-createLink.txt")
                .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                .with("cell", Setup.TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("odataSvcPath", Setup.TEST_ODATA)
                .with("entityTypeName", entityTypeName)
                .with("linkEntityTypeName", linkEntityTypeName)
                .with("token", MASTER_TOKEN_NAME)
                .with("accept", "application/json")
                .with("name", name)
                .with("linkName", linkName)
                .returns()
                .debug();
    }

    /**
     * AssociationEndを新規作成.
     */
    private TResponse createAssociationEnd(String name, String entityTypeName) {
        return Http.request("box/odatacol/schema/assocend/create.txt").with("cell", "testcell1").with("box", "box1")
                .with("collection", "setodata").with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON).with("token", DcCoreConfig.getMasterToken())
                .with("name", name).with("multiplicity", EdmMultiplicity.MANY.getSymbolString())
                .with("entityTypeName", entityTypeName).returns().statusCode(HttpStatus.SC_CREATED).debug();
    }

    /**
     * AssociationEndを削除する.
     */
    private void deleteAssociationEnd(String name, String entityTypeName) {
        Http.request("box/odatacol/schema/assocend/delete.txt").with("cell", "testcell1").with("box", "box1")
                .with("collection", "setodata").with("token", DcCoreConfig.getMasterToken()).with("name", name)
                .with("entityTypeName", entityTypeName).with("ifMatch", "*").returns()
                .statusCode(HttpStatus.SC_NO_CONTENT).debug();
    }

}
