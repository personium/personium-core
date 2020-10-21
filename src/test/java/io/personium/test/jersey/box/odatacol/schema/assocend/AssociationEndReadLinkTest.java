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
package io.personium.test.jersey.box.odatacol.schema.assocend;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmMultiplicity;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * AssociationEndの$ink削除のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AssociationEndReadLinkTest extends AbstractCase {

    /**
     * コンストラクタ.
     */
    public AssociationEndReadLinkTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * AssociationEndのlinkを取得して400エラーとなること.
     */
    @Test
    public final void AssociationEndのlinkを取得して400エラーとなること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";

        try {
            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            createLink(entityTypeName, linkEntityTypeName, name, linkName);

            Http.request("box/associationEnd-readLink.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("odataSvcPath", Setup.TEST_ODATA)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("key", key)
                    .with("navKey", navKey)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

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
     * AssociationEndのlink取得でパースに失敗した場合400となること.
     */
    @Test
    public final void AssociationEndのlink取得でパースに失敗した場合400となること() {
        String entityTypeName = "Product";
        String linkEntityTypeName = "Category";
        String name = "AssoEnd";
        String linkName = "LinkAssoEnd";
        String key = "Name='" + name + "',_EntityType.Name='" + entityTypeName + "'";
        String navKey = "Name='" + linkName + "',_EntityType.Name='" + linkEntityTypeName + "'";

        try {

            // AssociationEndの作成
            createAssociationEnd(name, entityTypeName);
            createAssociationEnd(linkName, linkEntityTypeName);

            createLink(entityTypeName, linkEntityTypeName, name, linkName);

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            String linksNavKey = URLEncoder.encode("'"
                    + UrlUtils.associationEnd(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, linkName,
                            linkEntityTypeName) + "'", "utf-8");
            String requestUrl = UrlUtils.associationEnd(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, name,
                    entityTypeName) + "/$links/_AssociationEnd(" + linksNavKey + ")";

            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        } catch (PersoniumException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            // AssociationEndのlink解除
            AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                    Setup.TEST_BOX1, key, navKey, -1);
            // AssociationEndの削除
            deleteAssociationEnd(name, entityTypeName);
            deleteAssociationEnd(linkName, linkEntityTypeName);
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
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .debug();
    }

    /**
     * AssociationEndを新規作成.
     */
    private void createAssociationEnd(String name, String entityTypeName) {
        AssociationEndUtils.create(PersoniumUnitConfig.getMasterToken(),
                EdmMultiplicity.MANY.getSymbolString(),
                Setup.TEST_CELL1,
                Setup.TEST_BOX1,
                Setup.TEST_ODATA,
                HttpStatus.SC_CREATED,
                name,
                entityTypeName);
    }

    /**
     * AssociationEndを削除する.
     */
    private void deleteAssociationEnd(String name, String entityTypeName) {
        AssociationEndUtils.delete(
                AbstractCase.MASTER_TOKEN_NAME,
                Setup.TEST_CELL1,
                Setup.TEST_ODATA,
                entityTypeName,
                Setup.TEST_BOX1,
                name, HttpStatus.SC_NO_CONTENT);
    }

}
