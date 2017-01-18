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
package com.fujitsu.dc.test.jersey.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * $metadata/$metadataのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ServiceSchmaTest extends JerseyTest {

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
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ServiceSchmaTest() {
        super(new WebAppDescriptor.Builder(ServiceSchmaTest.INIT_PARAMS).build());
    }

    /**
     * $metadataでサービスドキュメントを取得するテスト.
     */
    @Test
    public final void $metadataでサービスドキュメントを取得するテスト() {

        TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                .with("path", "\\$metadata")
                .with("col", "setodata")
                .with("accept", "application/atomsvc+xml")
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディーのチェック
        String str = res.getBody();

        Service service = JAXB.unmarshal(new StringReader(str), Service.class);

        Service rightService = new Service();
        rightService.workspace = new Workspace("Default",
                new HashSet<Collection>(Arrays.asList(new Collection("ComplexType", "ComplexType"),
                        new Collection("ComplexTypeProperty", "ComplexTypeProperty"),
                        new Collection("AssociationEnd", "AssociationEnd"),
                        new Collection("EntityType", "EntityType"),
                        new Collection("Property", "Property"))));

        assertTrue(rightService.isEqualTo(service));
    }

    /**
     * $metadataでサービスドキュメントを取得するテスト_$format指定.
     */
    @Test
    public final void $metadataでサービスドキュメントを取得するテスト_$format指定() {

        TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                .with("path", "\\$metadata\\?\\$format=atomsvc")
                .with("col", "setodata")
                .with("accept", "application/xml")
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディーのチェック
        String str = res.getBody();

        Service service = JAXB.unmarshal(new StringReader(str), Service.class);

        Service rightService = new Service();
        rightService.workspace = new Workspace("Default",
                new HashSet<Collection>(Arrays.asList(new Collection("ComplexType", "ComplexType"),
                        new Collection("ComplexTypeProperty", "ComplexTypeProperty"),
                        new Collection("AssociationEnd", "AssociationEnd"),
                        new Collection("EntityType", "EntityType"),
                        new Collection("Property", "Property"))));
        assertTrue(rightService.isEqualTo(service));
    }

    /**
     * $metadataでユーザのEDMXを取得するテスト.
     */
    @Test
    public final void $metadataでユーザのEDMXを取得するテスト() {
        try {
            Setup.deleteUserDatas(Setup.TEST_CELL1);
            Setup.deleteTestCollectionSchema(Setup.TEST_CELL1);
            Setup.deleteMaxPropTestCollectionSchema(Setup.TEST_CELL1);

            Setup.createTestCollectionSchema(Setup.TEST_CELL1);

            TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                    .with("path", "\\$metadata")
                    .with("col", "setodata")
                    .with("accept", "application/xml")
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            String str = res.getBody();
            String regex = "Namespace=\"(UserData)\">";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);
            matcher.find();
            String namespace = matcher.group(1);

            Edmx edmx = JAXB.unmarshal(new StringReader(str), Edmx.class);
            Edmx checkBody = getUserSchemaRightEdmx(namespace);
            assertTrue(checkBody.equals(edmx));

        } finally {
            Setup.createMaxPropTestCollectionSchema(Setup.TEST_CELL1);
            Setup.createUserDatas(Setup.TEST_CELL1);
        }
    }

    /**
     * Nameが大文字小文字のみ異なるComplexTypeが存在する場合にユーザODataスキーマ($metadata)のComokexTypeリストが正しく生成されていること.
     * @throws SAXException SAXException
     * @throws ParserConfigurationException ParserConfigurationException
     * @throws IOException IOException
     */
    @Test
    public final void Nameが大文字小文字のみ異なるComplexTypeが存在する場合にユーザODataスキーマのComokexTypeリストが正しく生成されていること()
            throws SAXException, ParserConfigurationException, IOException {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "edmSchemaTestCell";
        String boxName = "TestBox";
        String colName = "TestCol";
        String entityTypeName = "TestEntityType";
        try {
            // Cell作成
            CellUtils.create(cellName, token, HttpStatus.SC_CREATED);
            // Box作成
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);
            // Collection作成
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);
            // EntityType作成
            EntityTypeUtils.create(cellName,
                    token, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            /*
             * Property:propertySmall → ComplexType:complex → ComplexTypeProperty:compro Property:propertyBig →
             * ComplexType:COMPLEX → ComplexTypeProperty:COMPRO
             */
            // ComplexType作成(小文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "complex");
            // ComplexType作成(大文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "COMPLEX");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "compro", "complex", "Edm.String", true, null, "None");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "COMPRO", "COMPLEX", "Edm.String", true, null, "None");
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertySmall",
                    entityTypeName, "complex", true, null, "None", false, null);
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertyBig",
                    entityTypeName, "COMPLEX", true, null, "None", false, null);

            TResponse res = Http.request("box/$metadata-get.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("col", colName)
                    .with("accept", "application/xml")
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            String str = res.getBody();
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docbuilder = dbfactory.newDocumentBuilder();
            Document doc = docbuilder.parse(new ByteArrayInputStream(str.getBytes("UTF-8")));

            // EntityTypeのスキーマ情報のチェック
            // 大文字と小文字の区別がされていることを確認
            NodeList elementsEntityType = doc.getElementsByTagName("EntityType");
            for (int i = 0; i < elementsEntityType.getLength(); i++) {
                Element element = (Element) elementsEntityType.item(i);
                String property = element.getAttribute("Name");
                if (property.equals("TestEntityType")) {
                    NodeList elementsProperty = element.getElementsByTagName("Property");
                    // Propertyのスキーマ情報のチェック
                    for (int j = 0; j < elementsProperty.getLength(); j++) {
                        Element elementProperty = (Element) elementsProperty.item(i);
                        String propName = elementProperty.getAttribute("Name");
                        if (!propName.startsWith("_")) {
                            if (propName.equals("propertyBig")) {
                                String propType = elementProperty.getAttribute("Type");
                                if (!propType.equals("UserData.COMPLEX")) {
                                    fail("Unexpected Property Name:" + propName + " Type:" + propType);
                                }
                            }
                            if (propName.equals("propertySmall")) {
                                String propType = elementProperty.getAttribute("Type");
                                if (!propType.equals("UserData.complex")) {
                                    fail("Unexpected Property Name:" + propName + " Type:" + propType);
                                }
                            } else {
                                fail("Unexpected Property Name:" + propName);
                            }
                        }
                    }

                }
            }
            // ComplexTypeのスキーマ情報のチェック
            // 大文字と小文字の区別がされていることを確認
            NodeList elementsComplexType = doc.getElementsByTagName("ComplexType");
            assertEquals(2, elementsComplexType.getLength());
            for (int i = 0; i < elementsComplexType.getLength(); i++) {
                Element element = (Element) elementsComplexType.item(i);
                String complexTypeName = element.getAttribute("Name");
                if (complexTypeName.equals("COMPLEX")) {
                    NodeList elementsProperty = element.getElementsByTagName("Property");
                    assertEquals(1, elementsProperty.getLength());
                    // ComplexTypePropertyのスキーマ情報のチェック
                    for (int j = 0; j < elementsProperty.getLength(); j++) {
                        Element elementProperty = (Element) elementsProperty.item(i);
                        String propName = elementProperty.getAttribute("Name");
                        if (!propName.equals("COMPRO")) {
                            fail("Unexpected Property:" + propName);
                        }
                    }
                } else if (complexTypeName.equals("complex")) {
                    NodeList elementsProperty = element.getElementsByTagName("Property");
                    assertEquals(1, elementsProperty.getLength());
                    // ComplexTypePropertyのスキーマ情報のチェック
                    for (int j = 0; j < elementsProperty.getLength(); j++) {
                        Element elementProperty = (Element) elementsProperty.item(j);
                        String propName = elementProperty.getAttribute("Name");
                        if (!propName.equals("compro")) {
                            fail("Unexpected Property:" + propName);
                        }
                    }
                } else {
                    fail("Unexpected ComplexType:" + complexTypeName);
                }
            }
        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    // /**
    // * リンク先のAssosiationを削除後にユーザのEDMXを取得した場合_削除されたリンク情報が取得できていないこと.
    // */
    // @Test
    // public final void リンク先のAssosiationを削除後にユーザスキーマ情報を取得した場合_削除されたリンク情報が取得できていないこと() {
    // }
    //
    // /**
    // * リンク先のEntityTypeを削除後ユーザのEDMXを取得した場合_削除されたリンク情報が取得できていないこと.
    // */
    // @Test
    // public final void リンク先のEntityTypeを削除後ユーザのEDMXを取得した場合_削除されたリンク情報が取得できていないこと() {
    // }
    /**
     * AssociationEndとLinkされているEntityTypeを削除するとresponseが409であること. TODO v1.1
     * PropertyとLinkされている、EntityTypeを削除するとresponseが409であること.のテストを追加
     */
    @Test
    public final void AssociationEndとLinkされているEntityTypeを削除するとresponseが409であること() {

        String associationEndName = "linkAssociationEnd";
        String entityTypeName = "linkEntityType";
        String odataSvcName = Setup.TEST_ODATA;
        String multiplicity = "1";
        try {
            // EntityTypeの作成
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                    odataSvcName, entityTypeName, HttpStatus.SC_CREATED);

            // 上のEntityTypeと結びつくAssociationEnd作成
            associationEndPost(Setup.TEST_CELL1, odataSvcName, entityTypeName, associationEndName, multiplicity,
                    HttpStatus.SC_CREATED);

            // EntityTypeの削除(結びつくEntityTypeがあるため、409)
            entityTypeDelete(Setup.TEST_CELL1, odataSvcName, entityTypeName, HttpStatus.SC_CONFLICT);

            // 結びつくEntityTypeの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, odataSvcName, entityTypeName,
                    Setup.TEST_BOX1, associationEndName, HttpStatus.SC_NO_CONTENT);

            // EntityTypeの削除(結びつくEntityTypeが存在しないため、204)
            entityTypeDelete(Setup.TEST_CELL1, odataSvcName, entityTypeName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくEntityTypeの削除
            entityTypeDelete(Setup.TEST_CELL1, odataSvcName, entityTypeName, -1);

            // EntityTypeの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, odataSvcName,
                    entityTypeName, Setup.TEST_BOX1, associationEndName, -1);
        }
    }

    /**
     * EntityTypeのDELETE.
     * @param cellName
     */
    private TResponse entityTypeDelete(final String cellName, final String path,
            final String name, final int code) {

        TResponse tresponse = Http.request("box/entitySet-delete.txt")
                .with("cellPath", cellName)
                .with("boxPath", "box1")
                .with("odataSvcPath", path)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("accept", "application/xml")
                .with("Name", name)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndの登録.
     * @param cellName
     */
    private TResponse associationEndPost(final String cellName, final String path, final String entityTypeName,
            final String name, final String multiplicity, final int code) {
        final String boxName = "box1";
        TResponse tresponse = Http.request("box/associationEnd-post.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("odataSvcPath", path)
                .with("entityTypeName", entityTypeName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("accept", "application/json")
                .with("Name", name)
                .with("Multiplicity", multiplicity)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * スキーマのスキーマ情報を取得するテスト$metadata_$metadata.
     */
    @Test
    public final void スキーマのスキーマ情報を取得するテスト$metadata_$metadata() {

        TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                .with("path", "\\$metadata/\\$metadata")
                .with("col", "setodata")
                .with("accept", "application/xml")
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディーのチェック
        String str = res.getBody();

        Edmx edmx = JAXB.unmarshal(new StringReader(str), Edmx.class);
        Edmx checkBody = getRightEdmx();
        assertTrue(checkBody.equals(edmx));
    }

    private Edmx getRightEdmx() {
        // EntityType
        Map<String, EntityType> entityType = new HashMap<String, EntityType>();
        entityType.put("EntityType",
                new EntityType("EntityType", new Key(Arrays.asList(new PropertyRef("Name"))),
                        Arrays.asList(
                                new Property("Name", "Edm.String", "false"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")),
                        Arrays.asList(
                                new NavigationProperty("_AssociationEnd",
                                        "ODataSvcSchema.EntityType-AssociationEnd-assoc",
                                        "EntityType-AssociationEnd",
                                        "AssociationEnd-EntityType"),
                                new NavigationProperty("_Property",
                                        "ODataSvcSchema.EntityType-Property-assoc",
                                        "EntityType-Property",
                                        "Property-EntityType"))
                ));
        entityType.put("AssociationEnd",
                new EntityType("AssociationEnd", new Key(Arrays.asList(new PropertyRef("Name"), new PropertyRef(
                        "_EntityType.Name"))),
                        Arrays.asList(
                                new Property("Name", "Edm.String", "false"),
                                new Property("Multiplicity", "Edm.String", "false"),
                                new Property("_EntityType.Name", "Edm.String", "false"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")),
                        Arrays.asList(
                                new NavigationProperty("_EntityType",
                                        "ODataSvcSchema.EntityType-AssociationEnd-assoc",
                                        "AssociationEnd-EntityType",
                                        "EntityType-AssociationEnd"),
                                new NavigationProperty("_AssociationEnd",
                                        "ODataSvcSchema.AssociationEnd-AssociationEnd-assoc",
                                        "AssociationEnd-AssociationEnd",
                                        "AssociationEnd-AssociationEnd"))
                ));
        entityType.put("Property",
                new EntityType("Property", new Key(Arrays.asList(new PropertyRef("Name"), new PropertyRef(
                        "_EntityType.Name"))),
                        Arrays.asList(
                                new Property("Name", "Edm.String", "false"),
                                new Property("_EntityType.Name", "Edm.String", "false"),
                                new Property("Type", "Edm.String", "false"),
                                new Property("Nullable", "Edm.Boolean", "false", "true"),
                                new Property("DefaultValue", "Edm.String", "true"),
                                new Property("CollectionKind", "Edm.String", "true", "None"),
                                new Property("IsKey", "Edm.Boolean", "false", "false"),
                                new Property("UniqueKey", "Edm.String", "true"),
                                new Property("IsDeclared", "Edm.Boolean", "true", "true"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")),
                        Arrays.asList(
                                new NavigationProperty("_EntityType",
                                        "ODataSvcSchema.EntityType-Property-assoc",
                                        "Property-EntityType",
                                        "EntityType-Property"))
                ));
        entityType.put("ComplexType",
                new EntityType("ComplexType", new Key(Arrays.asList(new PropertyRef("Name"))),
                        Arrays.asList(
                                new Property("Name", "Edm.String", "false"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")),
                        Arrays.asList(
                                new NavigationProperty("_ComplexTypeProperty",
                                        "ODataSvcSchema.ComplexType-ComplexTypeProperty-assoc",
                                        "ComplexType-ComplexTypeProperty",
                                        "ComplexTypeProperty-ComplexType"))
                ));
        entityType.put("ComplexTypeProperty",
                new EntityType("ComplexTypeProperty", new Key(Arrays.asList(new PropertyRef("Name"), new PropertyRef(
                        "_ComplexType.Name"))),
                        Arrays.asList(
                                new Property("Name", "Edm.String", "false"),
                                new Property("_ComplexType.Name", "Edm.String", "false"),
                                new Property("Type", "Edm.String", "false"),
                                new Property("Nullable", "Edm.Boolean", "false", "true"),
                                new Property("DefaultValue", "Edm.String", "true"),
                                new Property("CollectionKind", "Edm.String", "true", "None"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")),
                        Arrays.asList(
                                new NavigationProperty("_ComplexType",
                                        "ODataSvcSchema.ComplexType-ComplexTypeProperty-assoc",
                                        "ComplexTypeProperty-ComplexType",
                                        "ComplexType-ComplexTypeProperty"))
                ));

        // Association
        List<Association> associations = Arrays.asList(
                new Association(
                        "EntityType-AssociationEnd-assoc",
                        Arrays.asList(
                                new End("EntityType-AssociationEnd", "ODataSvcSchema.EntityType", "1"),
                                new End("AssociationEnd-EntityType", "ODataSvcSchema.AssociationEnd", "*"))),
                new Association(
                        "AssociationEnd-AssociationEnd-assoc",
                        Arrays.asList(
                                new End("AssociationEnd-AssociationEnd", "ODataSvcSchema.AssociationEnd", "0..1"),
                                new End("AssociationEnd-AssociationEnd", "ODataSvcSchema.AssociationEnd", "0..1"))),
                new Association(
                        "EntityType-Property-assoc",
                        Arrays.asList(
                                new End("EntityType-Property", "ODataSvcSchema.EntityType", "1"),
                                new End("Property-EntityType", "ODataSvcSchema.Property", "*"))),
                new Association(
                        "ComplexType-ComplexTypeProperty-assoc",
                        Arrays.asList(
                                new End("ComplexType-ComplexTypeProperty", "ODataSvcSchema.ComplexType", "1"),
                                new End("ComplexTypeProperty-ComplexType", "ODataSvcSchema.ComplexTypeProperty", "*")))
                );

        // EntityContainer
        EntityContainer entityContainer = new EntityContainer("ODataSvcSchema",
                "true",
                new HashSet<EntitySet>(Arrays.asList(
                        new EntitySet("ComplexType", "ODataSvcSchema.ComplexType"),
                        new EntitySet("ComplexTypeProperty", "ODataSvcSchema.ComplexTypeProperty"),
                        new EntitySet("AssociationEnd", "ODataSvcSchema.AssociationEnd"),
                        new EntitySet("EntityType", "ODataSvcSchema.EntityType"),
                        new EntitySet("Property", "ODataSvcSchema.Property"))),
                Arrays.asList(
                        new AssociationSet("EntityType-AssociationEnd-assoc",
                                "ODataSvcSchema.EntityType-AssociationEnd-assoc",
                                Arrays.asList(new EndAssociationSet("EntityType-AssociationEnd", "EntityType"),
                                        new EndAssociationSet("AssociationEnd-EntityType", "AssociationEnd"))),
                        new AssociationSet("AssociationEnd-AssociationEnd-assoc",
                                "ODataSvcSchema.AssociationEnd-AssociationEnd-assoc",
                                Arrays.asList(new EndAssociationSet("AssociationEnd-AssociationEnd", "AssociationEnd"),
                                        new EndAssociationSet("AssociationEnd-AssociationEnd", "AssociationEnd"))),
                        new AssociationSet("EntityType-Property-assoc",
                                "ODataSvcSchema.EntityType-Property-assoc",
                                Arrays.asList(new EndAssociationSet("EntityType-Property", "EntityType"),
                                        new EndAssociationSet("Property-EntityType", "Property"))),
                        new AssociationSet(
                                "ComplexType-ComplexTypeProperty-assoc",
                                "ODataSvcSchema.ComplexType-ComplexTypeProperty-assoc",
                                Arrays.asList(new EndAssociationSet("ComplexType-ComplexTypeProperty", "ComplexType"),
                                        new EndAssociationSet("ComplexTypeProperty-ComplexType",
                                                "ComplexTypeProperty")))));
        return new Edmx("1.0", new DataSerevices("1.0", new Schema("ODataSvcSchema",
                entityType,
                associations,
                entityContainer)));
    }

    private Edmx getUserSchemaRightEdmx(String namespace) {
        // EntityType
        Map<String, EntityType> entityType = getEntityUserSchemaRightEdmx(namespace);

        // Association
        List<Association> associations = Arrays.asList(
                new Association(
                        "Price-Sales-assoc",
                        Arrays.asList(
                                new End("Price:price2sales", namespace + ".Price", "0..1"),
                                new End("Sales:sales2price", namespace + ".Sales", "0..1")
                                )),
                new Association(
                        "Product-Sales-assoc",
                        Arrays.asList(
                                new End("Product:product2Sales", namespace + ".Product", "*"),
                                new End("Sales:sales2product", namespace + ".Sales", "*")
                                )),
                new Association(
                        "Product-Supplier-assoc",
                        Arrays.asList(
                                new End("Product:product2supplier", namespace + ".Product", "1"),
                                new End("Supplier:supplier2product", namespace + ".Supplier", "0..1")
                                )),
                new Association(
                        "Sales-SalesDetail-assoc",
                        Arrays.asList(
                                new End("Sales:sales2salesDetail", namespace + ".Sales", "1"),
                                new End("SalesDetail:salesDetail2sales", namespace + ".SalesDetail", "*")
                                )),
                new Association(
                        "Sales-Supplier-assoc",
                        Arrays.asList(
                                new End("Sales:sales2supplier", namespace + ".Sales", "0..1"),
                                new End("Supplier:supplier2sales", namespace + ".Supplier", "*")
                                ))
                );

        // EntityContainer
        EntityContainer entityContainer = new EntityContainer(namespace,
                "true",
                new HashSet<EntitySet>(Arrays.asList(
                        new EntitySet("Sales", namespace + ".Sales"),
                        new EntitySet("Category", namespace + ".Category"),
                        new EntitySet("Supplier", namespace + ".Supplier"),
                        new EntitySet("Product", namespace + ".Product"),
                        // new EntitySet("Testlink", namespace + ".Testlink"),
                        new EntitySet("Price", namespace + ".Price"),
                        new EntitySet("SalesDetail", namespace + ".SalesDetail")
                        )),
                Arrays.asList(
                        new AssociationSet("Price-Sales-assoc",
                                namespace + ".Price-Sales-assoc",
                                Arrays.asList(new EndAssociationSet("Price:price2sales", "Price"),
                                        new EndAssociationSet("Sales:sales2price", "Sales"))),
                        new AssociationSet("Product-Sales-assoc",
                                namespace + ".Product-Sales-assoc",
                                Arrays.asList(new EndAssociationSet("Product:product2Sales", "Product"),
                                        new EndAssociationSet("Sales:sales2product", "Sales"))),
                        new AssociationSet("Product-Supplier-assoc",
                                namespace + ".Product-Supplier-assoc",
                                Arrays.asList(new EndAssociationSet("Product:product2supplier", "Product"),
                                        new EndAssociationSet("Supplier:supplier2product", "Supplier"))),
                        new AssociationSet("Sales-SalesDetail-assoc",
                                namespace + ".Sales-SalesDetail-assoc",
                                Arrays.asList(new EndAssociationSet("Sales:sales2salesDetail", "Sales"),
                                        new EndAssociationSet("SalesDetail:salesDetail2sales", "SalesDetail"))),
                        new AssociationSet("Sales-Supplier-assoc",
                                namespace + ".Sales-Supplier-assoc",
                                Arrays.asList(new EndAssociationSet("Sales:sales2supplier", "Sales"),
                                        new EndAssociationSet("Supplier:supplier2sales", "Supplier")))
                        ));
        return new Edmx("1.0", new DataSerevices("1.0", new Schema(namespace,
                entityType,
                associations,
                entityContainer)));
    }

    private Map<String, EntityType> getEntityUserSchemaRightEdmx(String namespace) {
        Map<String, EntityType> entityType = new HashMap<String, EntityType>();
        entityType.put("Category",
                new EntityType(
                        "Category",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                )
                )
                );
        entityType.put("Price",
                new EntityType(
                        "Price",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                ),
                        Arrays.asList(
                                new NavigationProperty("_Sales",
                                        namespace + ".Price-Sales-assoc",
                                        "Price:price2sales",
                                        "Sales:sales2price")
                                )
                )
                );
        entityType.put("Product",
                new EntityType(
                        "Product",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                ),
                        Arrays.asList(
                                new NavigationProperty("_Sales",
                                        namespace + ".Product-Sales-assoc",
                                        "Product:product2Sales",
                                        "Sales:sales2product"),
                                new NavigationProperty("_Supplier",
                                        namespace + ".Product-Supplier-assoc",
                                        "Product:product2supplier",
                                        "Supplier:supplier2product")
                                )
                )
                );
        entityType.put("Sales",
                new EntityType(
                        "Sales",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                ),
                        Arrays.asList(
                                new NavigationProperty("_Price",
                                        namespace + ".Price-Sales-assoc",
                                        "Sales:sales2price",
                                        "Price:price2sales"),
                                new NavigationProperty("_Product",
                                        namespace + ".Product-Sales-assoc",
                                        "Sales:sales2product",
                                        "Product:product2Sales"),
                                new NavigationProperty("_SalesDetail",
                                        namespace + ".Sales-SalesDetail-assoc",
                                        "Sales:sales2salesDetail",
                                        "SalesDetail:salesDetail2sales"),
                                new NavigationProperty("_Supplier",
                                        namespace + ".Sales-Supplier-assoc",
                                        "Sales:sales2supplier",
                                        "Supplier:supplier2sales"))
                )
                );
        entityType.put("SalesDetail",
                new EntityType(
                        "SalesDetail",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                ),
                        Arrays.asList(
                                new NavigationProperty("_Sales",
                                        namespace + ".Sales-SalesDetail-assoc",
                                        "SalesDetail:salesDetail2sales",
                                        "Sales:sales2salesDetail")
                                )
                )
                );
        entityType.put("Supplier",
                new EntityType(
                        "Supplier",
                        "true",
                        new Key(Arrays.asList(new PropertyRef("__id"))),
                        Arrays.asList(
                                new Property("__id", "Edm.String", "false", "UUID()"),
                                new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
                                new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
                                ),
                        Arrays.asList(
                                new NavigationProperty("_Product",
                                        namespace + ".Product-Supplier-assoc",
                                        "Supplier:supplier2product",
                                        "Product:product2supplier"),
                                new NavigationProperty("_Sales",
                                        namespace + ".Sales-Supplier-assoc",
                                        "Supplier:supplier2sales",
                                        "Sales:sales2supplier")
                                )
                )
                );
        // entityType.put("Testlink",
        // new EntityType(
        // "Testlink",
        // new Key(Arrays.asList(new PropertyRef("__id"))),
        // Arrays.asList(
        // new Property("__id", "Edm.String", "false", "UUID()"),
        // new Property("__published", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3"),
        // new Property("__updated", "Edm.DateTime", "false", "SYSUTCDATETIME()", "3")
        // )
        // )
        // );
        return entityType;
    }

    // /**
    // * ODataCollectionの作成.
    // */
    // private void createODataCollection() {
    // Http.request("box/mkcol-odata.txt")
    // .with("cellPath", "testcell1")
    // .with("path", "odatacol")
    // .with("token", AbstractCase.MASTER_TOKEN_NAME)
    // .returns()
    // .statusCode(HttpStatus.SC_CREATED);
    // }
    //
    // /**
    // * CollectionDELETEの実行.
    // */
    // private void deleteCollection(final String path) {
    // // boxの削除
    // Http.request("box/delete-col.txt")
    // .with("cellPath", "testcell1")
    // .with("path", path)
    // .with("token", AbstractCase.MASTER_TOKEN_NAME)
    // .returns()
    // .statusCode(HttpStatus.SC_OK);
    // }
}
