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
package io.personium.test.unit.core.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumUnitConfig.BinaryData;
import io.personium.core.bar.BarFileInstaller;
import io.personium.core.bar.BarFileReadRunner;
import io.personium.core.bar.jackson.JSONManifest;
import io.personium.core.bar.jackson.JSONMappedObject;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.test.categories.Unit;

/**
 * BarFileのバリデートのユニットテストクラス.
 */
@Category({Unit.class })
public class BarFileValidateTest {

    private static final String RESOURCE_PATH = "requestData/barInstallUnit";

    /**
     * .
     */
    private class TestBarRunner extends BarFileReadRunner {
        TestBarRunner() {
            super(null, null, null, null, null, null, null, null);
        }

        public boolean createMetadata(
                ZipArchiveEntry zae,
                String entryName,
                long maxSize,
                Set<String> keyList,
                List<String> doneKeys
                ) {
            return super.createMetadata(zae, entryName, maxSize, keyList, doneKeys);
        }

        /**
         * .
         * @param jp jp
         * @param mapper mapper
         * @param jsonName jsonName
         * @throws IOException
         */
        public JSONMappedObject barFileJsonValidate(
                JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
            return super.barFileJsonValidate(jp, mapper, jsonName);
        }

        /**
         * .
         * @param jp jp
         * @param mapper mapper
         * @param jsonName jsonName
         * @throws IOException
         */
        public void parseJsonEntityData(JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
            super.registJsonEntityData(jp, mapper, jsonName);
        }

        /**
         * 必須項目のバリデートチェック.
         * @param jp Jsonパーサー
         * @param mapper ObjectMapper
         * @return JSONManifestオブジェクト
         * @throws IOException IOException
         */
        public JSONManifest manifestJsonValidate(JsonParser jp, ObjectMapper mapper) throws IOException {
            return super.manifestJsonValidate(jp, mapper);
        }

        /**
         * XMLのデータを１件処理する.
         * @param rootPropsName 90_rootprops_xmlのファイルパス名
         * @param inputStream 入力ストリーム
         * @param boxUrl boxのURL
         * @return 正常終了した場合はtrue
         */
        public boolean registXmlEntry(String rootPropsName, InputStream inputStream, String boxUrl) {
            return super.registXmlEntry(rootPropsName, inputStream, boxUrl);
        }

        /**
         * 00_$metadata_xmlを解析してユーザスキーマの登録処理を行う.
         * @param entryName エントリ名
         * @param inputStream 入力ストリーム
         * @param davCmp Collection操作用オブジェクト
         * @return 正常終了した場合はtrue
         */
        protected boolean registUserSchema(String entryName, InputStream inputStream, DavCmp davCmp) {
            return super.registUserSchema(entryName, inputStream, davCmp);
        }

        /**
         * 10_odatarelations.jsonのデータを読み込みユーザデータのLink情報を生成する.
         * @param entryName エントリ名
         * @param inputStream 入力ストリーム
         * @return 正常終了した場合はtrue
         */
        public List<JSONMappedObject> registJsonLinksUserdata(String entryName, InputStream inputStream) {
            return super.registJsonLinksUserdata(entryName, inputStream);
        }

    }

    /**
     * 10_relations.jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_10_relations_jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること() {

        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/10_relations_too_many_field.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "10_relations.json";

            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (JsonMappingException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("JsonMappingExceptionが返却されない");
    }

    /**
     * 20_role.jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_20_role_jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること() {
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/20_roles_too_many_field.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "20_roles.json";

            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (JsonMappingException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("JsonMappingExceptionが返却されない");
    }

    /**
     * 30_extRole.jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_30_extRole_jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること() {
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/30_extroles_too_many_field.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            String jsonName = "30_extroles.json";
            ObjectMapper mapper = new ObjectMapper();

            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (JsonMappingException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("JsonMappingExceptionが返却されない");
    }

    /**
     * 30_extRole.jsonにExtRoleUrlがない場合_JsonMappingExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_30_extRole_jsonにExtRoleUrlがない場合_JsonMappingExceptionが返却されること() {
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/30_extroles_no_extrole.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            String jsonName = "30_extroles.json";
            ObjectMapper mapper = new ObjectMapper();

            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (PersoniumCoreException e) {
            return;
        } catch (Exception e) {
            fail(e.getMessage());
        }
        fail("JsonMappingExceptionが返却されない");
    }

    /**
     * 30_extroles.jsonに_relation.Nameがない場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void バリデートテスト_30_extRole_jsonに_relation_Nameがない場合_PersoniumCoreExceptionが返却されること() {
        JSONObject json = new JSONObject();
        json.put("ExtRole", "https://fqdn/cellName/__role/__/role2");

        JsonFactory f = new JsonFactory();
        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            String jsonName = "30_extroles.json";
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (PersoniumCoreException e) {
            return;
        } catch (Exception e) {
            fail(e.getMessage());
        }
        fail("PersoniumCoreException not throw");
    }

    /**
     * 10_relations.jsonが配列形式でない場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_10_relations_jsonが配列形式でない場合_PersoniumCoreExceptionが返却されること() {

        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/10_relations_not_array.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "10_relations.json";

            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.parseJsonEntityData(jp, mapper, jsonName);
        } catch (PersoniumCoreException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * 10_relations.jsonの内容が異なるファイルである場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_10_relations_jsonの内容が異なるファイルである場合_PersoniumCoreExceptionが返却されること() {
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/10_relations_different_json.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "10_relations.json";

            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.parseJsonEntityData(jp, mapper, jsonName);
        } catch (PersoniumCoreException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * 10_relations.jsonの配列内がオブジェクトでない場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_10_relations_jsonの配列内がオブジェクトでない場合_PersoniumCoreExceptionが返却されること() {
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/10_relations_not_object.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "10_relations.json";

            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.parseJsonEntityData(jp, mapper, jsonName);
        } catch (PersoniumCoreException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * バリデートテスト_70_link_jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_70_link_jsonに必須項目以外がある場合_JsonMappingExceptionが返却されること() {

        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + "/70_$links_too_many_field.json");
        File file = new File(fileUrl.getPath());
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        try {
            jp = f.createJsonParser(file);
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "70_$links.json";

            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            jp.nextToken();
            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
        } catch (JsonMappingException e) {
            return;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        fail("JsonMappingExceptionが返却されない");
    }

    /**
     * バリデートテスト_70_link_jsonに必須項目がない場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_70_link_jsonに必須項目がない場合_PersoniumCoreExceptionが返却されること() {

        String[] files = {"/70_$links_no_fromtype.json",
                "/70_$links_no_fromname.json",
                "/70_$links_no_totype.json",
                "/70_$links_no_toname.json"};
        for (String filename : files) {
            URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
            File file = new File(fileUrl.getPath());
            JsonFactory f = new JsonFactory();
            JsonParser jp;
            try {
                jp = f.createJsonParser(file);
                ObjectMapper mapper = new ObjectMapper();
                String jsonName = "70_$links.json";

                jp.nextToken();
                jp.nextToken();
                jp.nextToken();
                jp.nextToken();
                TestBarRunner testBarRunner = new TestBarRunner();
                testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
            } catch (PersoniumCoreException e) {
                continue;
            } catch (IOException e) {
                fail(e.getMessage());
            }
            fail("JsonMappingExceptionが返却されない");
        }
    }

    /**
     * バリデートテスト_70_link_jsonのToTypeにAccount_ExtCell_Boxを指定した場合_PersoniumCoreExceptionが返却されること.
     */
    @Test
    public void バリデートテスト_70_link_jsonのToTypeにAccount_ExtCell_Boxを指定した場合_PersoniumCoreExceptionが返却されること() {

        String[] files = {"/70_$links_totype_Account.json",
               "/70_$links_totype_ExtCell.json",
               "/70_$links_totype_Box.json"};
        for (String filename : files) {
            URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
            File file = new File(fileUrl.getPath());
            JsonFactory f = new JsonFactory();
            JsonParser jp;
            try {
                jp = f.createJsonParser(file);
                ObjectMapper mapper = new ObjectMapper();
                String jsonName = "70_$links.json";

                jp.nextToken();
                jp.nextToken();
                jp.nextToken();
                jp.nextToken();
                TestBarRunner testBarRunner = new TestBarRunner();
                testBarRunner.barFileJsonValidate(jp, mapper, jsonName);
            } catch (PersoniumCoreException e) {
                continue;
            } catch (IOException e) {
                fail(e.getMessage());
            }
            fail("JsonMappingExceptionが返却されない");
        }
    }

    /**
     * 管理情報のリンク定義をパースして正常終了.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void 管理情報のリンク定義をパースして正常終了() {

        JSONObject links = new JSONObject();

        links.put("FromType", "Relation");
        JSONObject fromName = new JSONObject();
        fromName.put("Name", "relation1");
        links.put("FromName", fromName);

        links.put("ToType", "Role");
        JSONObject toName = new JSONObject();
        toName.put("Name", "role1");
        links.put("ToName", toName);

        JsonFactory f = new JsonFactory();
        try {
            JsonParser jp = f.createJsonParser(links.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "70_$links.json";

            TestBarRunner testBarRunner = new TestBarRunner();
            JSONMappedObject result = testBarRunner.barFileJsonValidate(jp, mapper, jsonName);

            assertEquals("Relation", result.getJson().get("FromType"));
            assertTrue(result.getJson().get("FromName") instanceof JSONObject);
            JSONObject fromResult = (JSONObject) result.getJson().get("FromName");
            assertEquals("relation1", fromResult.get("Name"));
            assertEquals("Role", result.getJson().get("ToType"));
            assertTrue(result.getJson().get("ToName") instanceof JSONObject);
            JSONObject toResult = (JSONObject) result.getJson().get("ToName");
            assertEquals("role1", toResult.get("Name"));
        } catch (PersoniumCoreException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ユーザデータのリンク定義をパースして正常終了.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void ユーザデータのリンク定義をパースして正常終了() {

        JSONObject links = new JSONObject();

        links.put("FromType", "Relation");
        JSONObject fromId = new JSONObject();
        fromId.put("__id", "id_relation1");
        links.put("FromId", fromId);

        links.put("ToType", "Role");
        JSONObject toId = new JSONObject();
        toId.put("__id", "id_role1");
        links.put("ToId", toId);

        JsonFactory f = new JsonFactory();
        try {
            JsonParser jp = f.createJsonParser(links.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            String jsonName = "10_odatarelations.json";

            TestBarRunner testBarRunner = new TestBarRunner();
            JSONMappedObject result = testBarRunner.barFileJsonValidate(jp, mapper, jsonName);

            assertEquals("Relation", result.getJson().get("FromType"));

            assertTrue(result.getJson().get("FromId") instanceof JSONObject);
            JSONObject fromResult = (JSONObject) result.getJson().get("FromId");
            assertEquals("id_relation1", fromResult.get("__id"));

            assertEquals("Role", result.getJson().get("ToType"));

            assertTrue(result.getJson().get("ToId") instanceof JSONObject);
            JSONObject toResult = (JSONObject) result.getJson().get("ToId");
            assertEquals("id_role1", toResult.get("__id"));
        } catch (PersoniumCoreException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 正しいJSONデータを与えてJSONmanifestオブジェクトが返却される.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void 正しいJSONデータを与えてJSONManifestオブジェクトが返却される() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", "http://app1.example.com");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            JSONManifest manifest = testBarRunner.manifestJsonValidate(jp, mapper);

            assertNotNull(manifest);
            assertEquals("1", manifest.getBarVersion());
            assertEquals("1", manifest.getBoxVersion());
            assertEquals("boxName", manifest.getDefaultPath());
            assertEquals("http://app1.example.com", manifest.getSchema());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * bar_versionを指定しない場合に例外がスローされる.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void bar_versionを指定しない場合に例外がスローされる() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", "http://app1.example.com");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.manifestJsonValidate(jp, mapper);
        } catch (PersoniumCoreException dce) {
            assertEquals(400, dce.getStatus());
            assertEquals("PR400-BI-0006", dce.getCode());
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * box_versionを指定しない場合に例外がスローされる.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void box_versionを指定しない場合に例外がスローされる() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", "http://app1.example.com");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.manifestJsonValidate(jp, mapper);
        } catch (PersoniumCoreException dce) {
            assertEquals(400, dce.getStatus());
            assertEquals("PR400-BI-0006", dce.getCode());
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * DefaultPathを指定しない場合に例外がスローされる.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void DefaultPathを指定しない場合に例外がスローされる() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("schema", "http://app1.example.com");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.manifestJsonValidate(jp, mapper);
        } catch (PersoniumCoreException dce) {
            assertEquals(400, dce.getStatus());
            assertEquals("PR400-BI-0006", dce.getCode());
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * DefaultPathにnullを指定した場合に例外がスローされる.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void DefaultPathにnullを指定した場合に例外がスローされる() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", null);
        json.put("schema", "http://app1.example.com");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.manifestJsonValidate(jp, mapper);
        } catch (PersoniumCoreException dce) {
            assertEquals(400, dce.getStatus());
            assertEquals("PR400-BI-0006", dce.getCode());
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * schemaを指定しない場合にJSONManifestオブジェクトが返却される.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void schemaを指定しない場合にJSONManifestオブジェクトが返却される() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            JSONManifest manifest = testBarRunner.manifestJsonValidate(jp, mapper);

            assertNotNull(manifest);
            assertEquals("1", manifest.getBarVersion());
            assertEquals("1", manifest.getBoxVersion());
            assertEquals("boxName", manifest.getDefaultPath());
            assertNull(manifest.getSchema());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 不正なキーの存在するJSONデータを与えた場合に例外がスローされる.
     */
    @Test
    @SuppressWarnings({"unchecked" })
    public void 不正なキーの存在するJSONデータを与えた場合に例外がスローされる() {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", null);
        json.put("schema", "http://app1.example.com");
        json.put("InvalidKey", "SomeValue");

        try {
            JsonParser jp = f.createJsonParser(json.toJSONString());
            ObjectMapper mapper = new ObjectMapper();
            jp.nextToken();

            TestBarRunner testBarRunner = new TestBarRunner();
            testBarRunner.manifestJsonValidate(jp, mapper);
        } catch (PersoniumCoreException dce) {
            assertEquals(400, dce.getStatus());
            assertEquals("PR400-BI-0006", dce.getCode());
            return;
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * 正しいRootPropsを与えた場合に復帰値trueで正常終了する.
     */
    @Test
    public void 正しいRootPropsを与えた場合に復帰値trueで正常終了する() {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<multistatus xmlns=\"DAV:\">");
            buf.append("<response>");
            buf.append("<href>dcbox:/</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:41:54.323+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:42:04 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/__/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("<response>");
            buf.append("<href>dcbox:/col1</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:41:54.337+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:41:54 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("<p:odata xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/__/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\">");
            buf.append("<ace>");
            buf.append("<principal>");
            buf.append("<href>../__/role</href>");
            buf.append("</principal>");
            buf.append("<grant>");
            buf.append("<privilege>");
            buf.append("<read/>");
            buf.append("</privilege>");
            buf.append("<privilege>");
            buf.append("<write/>");
            buf.append("</privilege>");
            buf.append("</grant>");
            buf.append("</ace>");
            buf.append("</acl>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("<response>");
            buf.append("<href>dcbox:/col2</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:42:04.863+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:42:04 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("<p:service xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/__/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("<response>");
            buf.append("<href>dcbox:/col2/__src</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("</resourcetype>");
            buf.append("</prop>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("</multistatus>");

            String boxUrl = "http://localhost:9998/testcell1/boxInstall";
            InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes());
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean ret = testBarRunner.registXmlEntry("bar/00_meta/90_rootprops.xml", inputStream, boxUrl);
            assertTrue(ret);
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * 不正なRootPropsを与えた場合にfalseが返される.
     */
    @Test
    public void 不正なRootPropsを与えた場合にfalseが返される() {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<multistatus xmlns=\"DAV:\">");
            buf.append("<response>");
            buf.append("<href>dcbox:/</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:41:54.323+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:42:04 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/box/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            // responseタグが閉じていない
            // buf.append("</response>");
            buf.append("</multistatus>");

            String boxUrl = "http://localhost:9998/testcell1/boxInstall";
            InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes());
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean ret = testBarRunner.registXmlEntry("bar/00_meta/90_rootprops.xml", inputStream, boxUrl);
            assertFalse(ret);
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * HREFタグが複数存在する場合にfalseが返される.
     */
    @Test
    public void HREFタグが複数存在する場合にfalseが返される() {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<multistatus xmlns=\"DAV:\">");
            buf.append("<response>");
            buf.append("<href>dcbox:/</href>");
            buf.append("<href>dcbox:/</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:41:54.323+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:42:04 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/box/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("</multistatus>");

            String boxUrl = "http://localhost:9998/testcell1/boxInstall";
            InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes());
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean ret = testBarRunner.registXmlEntry("bar/00_meta/90_rootprops.xml", inputStream, boxUrl);
            assertFalse(ret);
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * 不正なCollection種別が存在する場合にfalseが返される.
     */
    @Test
    public void 不正なCollection種別が存在する場合にfalseが返される() {
        try {
            StringBuffer buf = new StringBuffer();
            buf.append("<multistatus xmlns=\"DAV:\">");
            buf.append("<response>");
            buf.append("<href>dcbox:/col1</href>");
            buf.append("<propstat>");
            buf.append("<prop>");
            buf.append("<creationdate>2013-01-21T08:41:54.323+0900</creationdate>");
            buf.append("<getlastmodified>Sun, 20 Jan 2013 23:42:04 GMT</getlastmodified>");
            buf.append("<resourcetype>");
            buf.append("<collection/>");
            //
            buf.append("<p:invalidtype xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</resourcetype>");
            buf.append("<acl xml:base=\"https://tsunashima.c3.fla.fujitsu.com/test0121/__role/box/\""
                    + " xmlns:p=\"urn:x-personium:xmlns\"/>");
            buf.append("</prop>");
            buf.append("<status>HTTP/1.1 200 OK</status>");
            buf.append("</propstat>");
            buf.append("</response>");
            buf.append("</multistatus>");

            String boxUrl = "http://localhost:9998/testcell1/boxInstall";
            InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes());
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean ret = testBarRunner.registXmlEntry("bar/00_meta/90_rootprops.xml", inputStream, boxUrl);
            assertFalse(ret);
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * metadata_xmlに不正な名前のタグがある場合にtrueが返却される.
     */
    @Test
    public void metadata_xmlに不正な名前のタグがある場合にtrueが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_invalid_tag.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertTrue(res); // EntityTypeが1件もないとみなされるため
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのEntityTypeにName属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのEntityTypeにName属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_entity_name_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのEntityTypeにName属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのEntityTypeにName属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_entity_name_attr_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlにEntityTypeの定義がない場合にtrueが返却される.
     */
    @Test
    public void metadata_xmlにEntityTypeの定義がない場合にtrueが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_entity_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertTrue(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexTypeにName属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexTypeにName属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_complex_name_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexTypeにName属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexTypeにName属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_complex_name_attr_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationにName属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationにName属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_name_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationにName属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationにName属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_name_attr_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにRole属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにRole属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_role_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにRole属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにRole属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_role_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにRole属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにRole属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_role_attr_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにType属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにType属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_type_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにType属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにType属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_type_attr_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにType属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにType属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_type_attr_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにMultiplicity属性がない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにMultiplicity属性がない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_multi_attr_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにMultiplicity属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにMultiplicity属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_multi_attr_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのAssociationEndにMultiplicity属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのAssociationEndにMultiplicity属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_associaton_multi_attr_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのPropertyにType属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのPropertyにNullable属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_property_nullable_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのPropertyにType属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのPropertyにNullable属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_property_nullable_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのPropertyにType属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのPropertyにType属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_property_type_attr_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのPropertyのComplexTypeが存在しない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのPropertyのComplexTypeが存在しない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_property_type_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexPropertyにNullable属性値が空文字の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexPropertyにNullable属性値が空文字の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_compprop_nullable_empty.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexPropertyにNullable属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexPropertyにNullable属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_compprop_nullable_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexPropertyにType属性値が不正の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexPropertyにType属性値が不正の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_compprop_type_attr_invalid.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexTypePropertyのComplexTypeが存在しない場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexTypePropertyのComplexTypeが存在しない場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_compprop_type_notexist.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * metadata_xmlのComplexTypePropertyのComplexTypeが循環参照の場合にfalseが返却される.
     */
    @Test
    public void metadata_xmlのComplexTypePropertyのComplexTypeが循環参照の場合にfalseが返却される() {
        final String entryName = "bar/90_contents/odatacol1/00_$metadata.xml";
        final String filename = "/00_$metadata_compprop_type_circular_ref.xml";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            boolean res = testBarRunner.registUserSchema(entryName, fis, null);
            assertFalse(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * odatarelations_jsonのJSONパースエラーの場合にnullが返却される.
     */
    @Test
    public void odatarelations_jsonのJSONパースエラーの場合にnullが返却される() {
        final String entryName = "bar/90_contents/odatacol1/10_odatarelations.json";
        final String filename = "/10_odatarelations_parse_error.json";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            List<JSONMappedObject> res = testBarRunner.registJsonLinksUserdata(entryName, fis);
            assertNull(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * odatarelations_jsonのFromTypeの値がnullの場合にnullが返却される.
     */
    @Test
    public void odatarelations_jsonのFromTypeの値がnullの場合にnullが返却される() {
        final String entryName = "bar/90_contents/odatacol1/10_odatarelations.json";
        final String filename = "/10_odatarelations_fromtype_null.json";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            List<JSONMappedObject> res = testBarRunner.registJsonLinksUserdata(entryName, fis);
            assertNull(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }

    /**
     * odatarelations_jsonのFromTypeの項目指定なしの場合にnullが返却される.
     */
    @Test
    public void odatarelations_jsonのFromTypeの項目指定なしの場合にnullが返却される() {
        final String entryName = "bar/90_contents/odatacol1/10_odatarelations.json";
        final String filename = "/10_odatarelations_fromtype_notexist.json";
        URL fileUrl = ClassLoader.getSystemResource(RESOURCE_PATH + filename);
        File file = new File(fileUrl.getPath());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            TestBarRunner testBarRunner = new TestBarRunner();
            List<JSONMappedObject> res = testBarRunner.registJsonLinksUserdata(entryName, fis);
            assertNull(res);
            return;
        } catch (PersoniumCoreException dce) {
            fail("Unexpected exception");
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
        fail("PersoniumCoreExceptionが返却されない");
    }


    /**
     * BarFileInstallerのテスト用クラス.
     * @author Administrator
     */
    private class TestBarInstaller extends BarFileInstaller {
        TestBarInstaller() {
            super(null, null, null, null);
        }

        protected long getMaxBarFileSize() {
            return 0;
        }
        protected void checkBarFileSize(File barFile) {
            super.checkBarFileSize(barFile);
        }
        protected void checkBarFileEntrySize(ZipArchiveEntry zae, String entryName,
                long maxBarEntryFileSize) {
            super.checkBarFileEntrySize(zae, entryName, maxBarEntryFileSize);
        }
    }

    /**
     * barファイルのファイルサイズ上限値を超えた場合に例外が発生すること.
     */
    @Test
    public void barファイルのファイルサイズ上限値を超えた場合に例外が発生すること() {
        TestBarInstaller testBarInstaller = new TestBarInstaller();
        URL fileUrl = ClassLoader.getSystemResource("requestData/barInstall/V1_1_2_bar_minimum.bar");
        File file = new File(fileUrl.getPath());

        try {
            testBarInstaller.checkBarFileSize(file);
            fail("Unexpected exception");
        } catch (PersoniumCoreException dce) {
            String code = PersoniumCoreException.BarInstall.BAR_FILE_SIZE_TOO_LARGE.getCode();
            assertEquals(code, dce.getCode());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * barファイル内エントリのファイルサイズ上限値を超えた場合に例外が発生すること.
     */
    @Test
    public void barファイル内エントリのファイルサイズ上限値を超えた場合に例外が発生すること() {
        TestBarInstaller testBarInstaller = new TestBarInstaller();
        URL fileUrl = ClassLoader.getSystemResource("requestData/barInstall/V1_1_2_bar_minimum.bar");
        File file = new File(fileUrl.getPath());

        try {
            ZipFile zipFile = new ZipFile(file, "UTF-8");
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            long maxBarEntryFileSize = 0;
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zae = entries.nextElement();
                if (zae.isDirectory()) {
                    continue;
                }
                testBarInstaller.checkBarFileEntrySize(zae, zae.getName(), maxBarEntryFileSize);
            }
            fail("Unexpected exception");
        } catch (PersoniumCoreException dce) {
            String code = PersoniumCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE.getCode();
            assertEquals(code, dce.getCode());
        } catch (Exception ex) {
            fail("Unexpected exception");
        }
    }

    /**
     * fsync ON.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testStoreTemporaryBarFile() throws Exception {
        boolean fsyncEnabled = PersoniumUnitConfig.getFsyncEnabled();
        PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, "true");
        try {
            CellEsImpl cell = new CellEsImpl();
            cell.setId("hogeCell");
            BarFileInstaller bfi = Mockito.spy(new BarFileInstaller(cell, "hogeBox", null, null));
            Method method = BarFileInstaller.class.getDeclaredMethod(
                    "storeTemporaryBarFile", new Class<?>[] {InputStream.class});
            method.setAccessible(true);
            //any file
            method.invoke(bfi, new FileInputStream("pom.xml"));
            Mockito.verify(bfi, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.anyObject());
        } finally {
            PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, String.valueOf(fsyncEnabled));
        }
    }

    /**
     * fsync OFF.
     * FileDescriptor#sync() should never be called.
     * @throws Exception .
     */
    @Test
    public void testStoreTemporaryBarFile2() throws Exception {
        boolean fsyncEnabled = PersoniumUnitConfig.getFsyncEnabled();
        PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, "false");
        try {
            CellEsImpl cell = new CellEsImpl();
            cell.setId("hogeCell");
            BarFileInstaller bfi = Mockito.spy(new BarFileInstaller(cell, "hogeBox", null, null));
            Method method = BarFileInstaller.class.getDeclaredMethod(
                    "storeTemporaryBarFile", new Class<?>[] {InputStream.class});
            method.setAccessible(true);
            //any file
            method.invoke(bfi, new FileInputStream("pom.xml"));
            Mockito.verify(bfi, Mockito.never()).sync((FileDescriptor) Mockito.anyObject());
        } finally {
            PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, String.valueOf(fsyncEnabled));
        }
    }
}
