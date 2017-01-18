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
package com.fujitsu.dc.test.jersey.box.acl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Acl;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.complextype.ComplexTypeUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty.ComplexTypePropertyUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.ODataSchemaUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * BOXレベル-スキーマ変更に関するACLのテスト.<br />
 * ※データの存在チェックよりも権限チェックの方が先に行われるため、事前にデータを作成せずに行っている.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AclAlterSchemaTest extends JerseyTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "AclAlterSchemaTestCell";
    private static final String BOX_NAME = "box1";
    private static final String COL_NAME = "setodata";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT_READ = "read-account";
    private static final String ACCOUNT_WRITE = "write-account";
    private static final String ACCOUNT_ALTER_SCHEMA = "alter-schema-account";
    private static final String ACCOUNT_NO_PRIVILEGE = "no-privilege-account";
    private static final String ACCOUNT_ALL_PRIVILEGE = "all-account";
    private static final String ACCOUNT_COMB_PRIVILEGE = "comb-account";

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public AclAlterSchemaTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * すべてのテストで最初に実行する処理.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    @Before
    public void before() throws JAXBException {
        createODataCollection();
    }

    /**
     * すべてのテストで最後に実行する処理.
     */
    @After
    public void after() {
        CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_$metadata.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_$metadata() {
        String token;

        // read権限
        token = getToken(ACCOUNT_READ);
        // 参照系: OK
        // サービスドキュメント取得
        ODataSchemaUtils.getServiceDocument(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);

        // スキーマ取得 （$metadata/$metadata）
        ODataSchemaUtils.getODataSchema(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG
        // サービスドキュメント取得
        ODataSchemaUtils.getServiceDocument(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);

        // スキーマ取得 （$metadata/$metadata）
        ODataSchemaUtils.getODataSchema(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);

        // alterSchemaToken権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG
        // サービスドキュメント取得
        ODataSchemaUtils.getServiceDocument(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);

        // スキーマ取得 （$metadata/$metadata）
        ODataSchemaUtils.getODataSchema(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG
        // サービスドキュメント取得
        ODataSchemaUtils.getServiceDocument(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);

        // スキーマ取得 （$metadata/$metadata）
        ODataSchemaUtils.getODataSchema(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_EntityType.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_EntityType() {
        String token;
        String entityTypeName = "entity";
        String updateBody = String.format("{\"Name\":\"%s\"}", entityTypeName);

        String entitySetPath = String.format("/%s/%s/%s/\\$metadata/EntityType", CELL_NAME, BOX_NAME, COL_NAME);
        String entityPath = String.format("/%s/%s/%s/\\$metadata/EntityType('%s')", CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName);

        // read権限
        token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_NOT_FOUND);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_OK);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_OK);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_NO_CONTENT);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_FORBIDDEN);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_FORBIDDEN);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_AssociationEnd.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_AssociationEnd() {
        String entityTypeName = "entity";
        String associationEndName = "association";

        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        AssociationEndUtils.create(token, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN,
                associationEndName, entityTypeName);
        AssociationEndUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                HttpStatus.SC_NOT_FOUND);
        AssociationEndUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        AssociationEndUtils.update(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, associationEndName, "*", entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, HttpStatus.SC_FORBIDDEN);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.create(token, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN,
                associationEndName, entityTypeName);
        AssociationEndUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.update(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, associationEndName, "*", entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        AssociationEndUtils.create(token, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_CREATED,
                associationEndName, entityTypeName);
        AssociationEndUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.update(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, associationEndName, "*", entityTypeName, HttpStatus.SC_NO_CONTENT);
        AssociationEndUtils.delete(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, HttpStatus.SC_NO_CONTENT);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.create(token, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN,
                associationEndName, entityTypeName);
        AssociationEndUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.update(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, associationEndName, "*", entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(token, CELL_NAME, COL_NAME, entityTypeName, BOX_NAME,
                associationEndName, HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_Property.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_Property() {
        String entityTypeName = "entity";
        String propertyName = "property";

        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        PropertyUtils.create(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, "Edm.Int32", true, null, "None", false, null,
                HttpStatus.SC_FORBIDDEN);
        DcResponse res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
        res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        PropertyUtils.create(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, "Edm.Int32", true, null, "None", false, null,
                HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        PropertyUtils.create(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, "Edm.Int32", true, null, "None", false, null,
                HttpStatus.SC_CREATED);
        res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
        PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, HttpStatus.SC_NO_CONTENT);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        PropertyUtils.create(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, "Edm.Int32", true, null, "None", false, null,
                HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);

    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_ComplexType.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_ComplexType() {
        String complexTypeName = "complex";

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        ComplexTypeUtils.createWithToken(
                token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_NOT_FOUND);
        ComplexTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        ComplexTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, complexTypeName,
                HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.delete(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        ComplexTypeUtils.createWithToken(
                token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, complexTypeName,
                HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.delete(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        ComplexTypeUtils.createWithToken(
                token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_CREATED);
        ComplexTypeUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, complexTypeName,
                HttpStatus.SC_NO_CONTENT);
        ComplexTypeUtils.delete(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_NO_CONTENT);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        ComplexTypeUtils.createWithToken(
                token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, complexTypeName,
                HttpStatus.SC_FORBIDDEN);
        ComplexTypeUtils.delete(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_FORBIDDEN);

    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_ComplexTypeProperty.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_ComplexTypeProperty() {
        String complexTypeName = "complex";
        String complexTypePropertyName = "comp";

        ComplexTypeUtils.createWithToken(
                MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, complexTypeName, HttpStatus.SC_CREATED);

        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", complexTypePropertyName);
        updateBody.put("_ComplexType.Name", complexTypeName);
        updateBody.put("Type", "Edm.Double");

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        ComplexTypePropertyUtils.createWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, "Edm.Int32", HttpStatus.SC_FORBIDDEN);
        DcResponse res = ComplexTypePropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
        ComplexTypePropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        // リクエストボディの組み立て
        res = ComplexTypePropertyUtils.updateWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName, updateBody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.deleteWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, HttpStatus.SC_FORBIDDEN);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        ComplexTypePropertyUtils.createWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, "Edm.Int32", HttpStatus.SC_FORBIDDEN);
        res = ComplexTypePropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        // リクエストボディの組み立て
        res = ComplexTypePropertyUtils.updateWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName, updateBody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.deleteWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        ComplexTypePropertyUtils.createWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, "Edm.Int32", HttpStatus.SC_CREATED);
        res = ComplexTypePropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        // リクエストボディの組み立て
        res = ComplexTypePropertyUtils.updateWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName, updateBody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
        ComplexTypePropertyUtils.deleteWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, HttpStatus.SC_NO_CONTENT);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        ComplexTypePropertyUtils.createWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, "Edm.Int32", HttpStatus.SC_FORBIDDEN);
        res = ComplexTypePropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_FORBIDDEN);
        // リクエストボディの組み立て
        res = ComplexTypePropertyUtils.updateWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                complexTypePropertyName, complexTypeName, updateBody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
        ComplexTypePropertyUtils.deleteWithToken(token, CELL_NAME, BOX_NAME, COL_NAME, complexTypePropertyName,
                complexTypeName, HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_$links.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_$links() {
        String entityTypeName = "entity";
        String associationEndName = "association";
        String entityTypeName2 = "entity2";
        String associationEndName2 = "association2";

        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName2, HttpStatus.SC_CREATED);
        AssociationEndUtils.create(MASTER_TOKEN, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_CREATED,
                associationEndName, entityTypeName);
        AssociationEndUtils.create(MASTER_TOKEN, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_CREATED,
                associationEndName2, entityTypeName2);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        AssociationEndUtils.createLink(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                entityTypeName2, associationEndName, associationEndName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.getAssociationEndLinkList(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                associationEndName, HttpStatus.SC_OK);
        AssociationEndUtils.deleteLinkWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                AssociationEndUtils.getAssociationEndKey(associationEndName, entityTypeName),
                AssociationEndUtils.getAssociationEndKey(associationEndName2, entityTypeName2),
                HttpStatus.SC_FORBIDDEN);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.createLink(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                entityTypeName2, associationEndName, associationEndName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.getAssociationEndLinkList(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                associationEndName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.deleteLinkWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                AssociationEndUtils.getAssociationEndKey(associationEndName, entityTypeName),
                AssociationEndUtils.getAssociationEndKey(associationEndName2, entityTypeName2),
                HttpStatus.SC_FORBIDDEN);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: OK 更新系: NG
        AssociationEndUtils.createLink(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                entityTypeName2, associationEndName, associationEndName2, HttpStatus.SC_NO_CONTENT);
        AssociationEndUtils.getAssociationEndLinkList(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                associationEndName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.deleteLinkWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                AssociationEndUtils.getAssociationEndKey(associationEndName, entityTypeName),
                AssociationEndUtils.getAssociationEndKey(associationEndName2, entityTypeName2),
                HttpStatus.SC_NO_CONTENT);

        // 権限無し
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.createLink(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                entityTypeName2, associationEndName, associationEndName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.getAssociationEndLinkList(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                associationEndName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.deleteLinkWithToken(token, CELL_NAME, BOX_NAME, COL_NAME,
                AssociationEndUtils.getAssociationEndKey(associationEndName, entityTypeName),
                AssociationEndUtils.getAssociationEndKey(associationEndName2, entityTypeName2),
                HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_NP経由.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_NP経由() {
        String entityTypeName = "entity";
        String associationEndName = "association";
        String entityTypeName2 = "entity2";
        String associationEndName2 = "association2";
        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName2, HttpStatus.SC_CREATED);
        AssociationEndUtils.create(MASTER_TOKEN, "*", CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_CREATED,
                associationEndName, entityTypeName);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        AssociationEndUtils.createViaNP(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                associationEndName2, "*", entityTypeName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.listViaAssociationEndNP(token, CELL_NAME, BOX_NAME, COL_NAME, "EntityType",
                entityTypeName, HttpStatus.SC_OK);
        AssociationEndUtils.delete(MASTER_TOKEN, CELL_NAME, COL_NAME, entityTypeName2, BOX_NAME, associationEndName2,
                -1);

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.createViaNP(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                associationEndName2, "*", entityTypeName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.listViaAssociationEndNP(token, CELL_NAME, BOX_NAME, COL_NAME, "EntityType",
                entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(MASTER_TOKEN, CELL_NAME, COL_NAME, entityTypeName2, BOX_NAME, associationEndName2,
                -1);

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        AssociationEndUtils.createViaNP(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                associationEndName2, "*", entityTypeName2, HttpStatus.SC_CREATED);
        AssociationEndUtils.listViaAssociationEndNP(token, CELL_NAME, BOX_NAME, COL_NAME, "EntityType",
                entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(MASTER_TOKEN, CELL_NAME, COL_NAME, entityTypeName2, BOX_NAME, associationEndName2,
                -1);

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        AssociationEndUtils.createViaNP(token, CELL_NAME, BOX_NAME, COL_NAME, associationEndName, entityTypeName,
                associationEndName2, "*", entityTypeName2, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.listViaAssociationEndNP(token, CELL_NAME, BOX_NAME, COL_NAME, "EntityType",
                entityTypeName, HttpStatus.SC_FORBIDDEN);
        AssociationEndUtils.delete(MASTER_TOKEN, CELL_NAME, COL_NAME, entityTypeName2, BOX_NAME, associationEndName2,
                -1);
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_ダイナミックプロパティ.
     * @throws ParseException ボディのパースに失敗
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_ダイナミックプロパティ() throws ParseException {
        String entityTypeName = "entity";
        String id = "0001";
        String propertyName = "property";
        String body = String.format("{\"__id\":\"%s\", \"%s\":1}", id, propertyName);

        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        try {
            UserDataUtils.create(MASTER_TOKEN, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName);
            // ダイナミックプロパティに対する確認
            DcResponse res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                    entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);
        } finally {
            PropertyUtils.delete(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, propertyName, -1);
        }

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: NG
        try {
            UserDataUtils.create(MASTER_TOKEN, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName);
            // ダイナミックプロパティに対する確認
            DcResponse res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                    entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);
        } finally {
            PropertyUtils.delete(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, propertyName, -1);
        }

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: OK
        try {
            UserDataUtils.create(MASTER_TOKEN, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName);
            // ダイナミックプロパティに対する確認
            DcResponse res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                    entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST); // Int32からDouble以外は400
            UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName, propertyName, HttpStatus.SC_NO_CONTENT);
        } finally {
            PropertyUtils.delete(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, propertyName, -1);
        }

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        try {
            UserDataUtils.create(MASTER_TOKEN, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName);
            // ダイナミックプロパティに対する確認
            DcResponse res = PropertyUtils.get(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName, entityTypeName);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            res = PropertyUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, propertyName,
                    entityTypeName, propertyName, entityTypeName, "Edm.Double", true, null, "None", false, null);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            PropertyUtils.delete(OAuth2Helper.Scheme.BEARER + " " + token, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName, propertyName, HttpStatus.SC_FORBIDDEN);
        } finally {
            PropertyUtils.delete(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, propertyName, -1);
        }
    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_ダイナミックプロパティを含むユーザOData.
     * @throws ParseException ボディのパースに失敗
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_ダイナミックプロパティを含むユーザOData() throws ParseException {
        String entityTypeName = "entity";
        String id = "0001";
        String property1 = "prop1";
        String property2 = "prop2";
        String body = String.format("{\"__id\":\"%s\", \"%s\":\"1\"}", id, property1);
        String updateBody = String.format("{\"__id\":\"%s\", \"%s\":\"1\"}", id, property2);

        // read権限
        String token = getToken(ACCOUNT_READ);
        // 参照系: OK 更新系: NG
        try {
            EntityTypeUtils.create(
                    CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

            UserDataUtils.create(token, HttpStatus.SC_FORBIDDEN, body, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName);
            // get/list用データ作成
            UserDataUtils.create(MASTER_TOKEN, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME,
                    entityTypeName);
            UserDataUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, id, HttpStatus.SC_OK);
            UserDataUtils.list(CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, "", token, HttpStatus.SC_OK);
            UserDataUtils.update(token, HttpStatus.SC_FORBIDDEN, (JSONObject) new JSONParser().parse(updateBody),
                    CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id, "*");
            UserDataUtils.delete(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id);
        } finally {
            UserDataUtils.delete(MASTER_TOKEN, -1, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME,
                    CELL_NAME, -1);
        }

        // write権限
        token = getToken(ACCOUNT_WRITE);
        // 参照系: NG 更新系: OK
        try {
            EntityTypeUtils.create(
                    CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

            UserDataUtils.create(token, HttpStatus.SC_CREATED, body, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName);
            UserDataUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, id, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.list(CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, "", token, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.update(token, HttpStatus.SC_NO_CONTENT, (JSONObject) new JSONParser().parse(updateBody),
                    CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id, "*");
            UserDataUtils.delete(token, HttpStatus.SC_NO_CONTENT, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id);
        } finally {
            UserDataUtils.delete(MASTER_TOKEN, -1, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName,
                    id);
            EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME,
                    CELL_NAME, -1);
        }

        // alter-schema権限
        token = getToken(ACCOUNT_ALTER_SCHEMA);
        // 参照系: NG 更新系: NG
        try {
            EntityTypeUtils.create(
                    CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

            UserDataUtils.create(token, HttpStatus.SC_FORBIDDEN, body, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName);
            UserDataUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, id, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.list(CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, "", token, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.update(token, HttpStatus.SC_FORBIDDEN, (JSONObject) new JSONParser().parse(updateBody),
                    CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id, "*");
            UserDataUtils.delete(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id);
        } finally {
            EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME,
                    CELL_NAME, -1);
        }

        // 権限なし
        token = getToken(ACCOUNT_NO_PRIVILEGE);
        // 参照系: NG 更新系: NG
        try {
            EntityTypeUtils.create(
                    CELL_NAME, MASTER_TOKEN, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);

            UserDataUtils.create(token, HttpStatus.SC_FORBIDDEN, body, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName);
            UserDataUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, id, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.list(CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, "", token, HttpStatus.SC_FORBIDDEN);
            UserDataUtils.update(token, HttpStatus.SC_FORBIDDEN, (JSONObject) new JSONParser().parse(updateBody),
                    CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id, "*");
            UserDataUtils.delete(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, id);
        } finally {
            EntityTypeUtils.delete(COL_NAME, MASTER_TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME,
                    CELL_NAME, -1);
        }

    }

    /**
     * ODataコレクション操作の権限チェックが正しく動作すること_正常系.
     */
    @Test
    public void ODataコレクション操作の権限チェックが正しく動作すること_正常系() {
        String token;
        String entityTypeName = "entity";
        String updateBody = String.format("{\"Name\":\"%s\"}", entityTypeName);

        String entitySetPath = String.format("/%s/%s/%s/\\$metadata/EntityType", CELL_NAME, BOX_NAME, COL_NAME);
        String entityPath = String.format("/%s/%s/%s/\\$metadata/EntityType('%s')", CELL_NAME, BOX_NAME, COL_NAME,
                entityTypeName);

        // all権限
        token = getToken(ACCOUNT_ALL_PRIVILEGE);
        // 参照系: OK 更新系: OK
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_OK);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_NO_CONTENT);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_OK);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_OK);

        // read/write/alter-schema権限
        token = getToken(ACCOUNT_COMB_PRIVILEGE);
        // 参照系: OK 更新系: OK
        EntityTypeUtils.create(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.get(CELL_NAME, token, BOX_NAME, COL_NAME, entityTypeName, HttpStatus.SC_OK);
        EntityTypeUtils.list(token, CELL_NAME, BOX_NAME, COL_NAME, HttpStatus.SC_OK);
        EntityTypeUtils.update(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.merge(token, CELL_NAME, BOX_NAME, COL_NAME, entityTypeName, updateBody,
                HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.delete(COL_NAME, token, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                HttpStatus.SC_NO_CONTENT);
        ResourceUtils.options(token, entitySetPath, HttpStatus.SC_OK);
        ResourceUtils.options(token, entityPath, HttpStatus.SC_OK);
    }

    /**
     * Accountの自分セルローカルトークンを取得する.
     * @param account Account名
     * @return トークン
     */
    private String getToken(String account) {
        return ResourceUtils.getMyCellLocalToken(CELL_NAME, account, PASSWORD);
    }

    /**
     * テスト用のODataコレクションを作成し、テストに必要なAccountやACLの設定を作成する.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    private void createODataCollection() throws JAXBException {
        String roleRead = "role-read";
        String roleWrite = "role-write";
        String roleAlterSchema = "role-alter-schema";
        String roleNoPrivilege = "role-no-privilege";
        String roleAllPrivilege = "role-all-privilege";
        String roleCombPrivilege = "role-comb-privilege";

        // Collection作成
        CellUtils.create(CELL_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL_NAME, BOX_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, COL_NAME);

        // Role作成
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleRead, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleWrite, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleAlterSchema, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleNoPrivilege, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleAllPrivilege, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, roleCombPrivilege, HttpStatus.SC_CREATED);

        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(false, roleRead, "read"));
        acl.getAce().add(DavResourceUtils.createAce(false, roleWrite, "write"));
        acl.getAce().add(DavResourceUtils.createAce(false, roleAlterSchema, "alter-schema"));
        acl.getAce().add(DavResourceUtils.createAce(false, roleAllPrivilege, "all"));
        List<String> privileges = new ArrayList<String>();
        privileges.add("read");
        privileges.add("write");
        privileges.add("alter-schema");
        acl.getAce().add(DavResourceUtils.createAce(false, roleCombPrivilege, privileges));
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.DEFAULT_BOX_NAME));

        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, acl, HttpStatus.SC_OK);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_READ, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_WRITE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_ALTER_SCHEMA, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_NO_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_ALL_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_COMB_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_READ, roleRead, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_WRITE, roleWrite, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_ALTER_SCHEMA, roleAlterSchema, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_NO_PRIVILEGE, roleNoPrivilege, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_ALL_PRIVILEGE, roleAllPrivilege, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_COMB_PRIVILEGE, roleCombPrivilege, HttpStatus.SC_NO_CONTENT);
    }

}
