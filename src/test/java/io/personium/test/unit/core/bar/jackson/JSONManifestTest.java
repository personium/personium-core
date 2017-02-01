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
package io.personium.test.unit.core.bar.jackson;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.bar.jackson.JSONManifest;
import io.personium.test.categories.Unit;

/**
 * BarFileのバリデートのユニットテストクラス.
 */
@Category({Unit.class })
public class JSONManifestTest {

    /**
     * manifest.jsonのschema値がURL形式である場合trueが返却されること.
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void manifest_jsonのschema値がURL形式である場合trueが返却されること() throws IOException {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", "http://app1.example.com/");
        JsonParser jp = f.createJsonParser(json.toJSONString());
        ObjectMapper mapper = new ObjectMapper();
        jp.nextToken();

        JSONManifest manifest = mapper.readValue(jp, JSONManifest.class);

        assertTrue(manifest.checkSchema());
    }

    /**
     * manifest_jsonのschema値がURL形式でない場合falseが返却されること.
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void manifest_jsonのschema値がURL形式でない場合falseが返却されること() throws IOException {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", "test");
        JsonParser jp = f.createJsonParser(json.toJSONString());
        ObjectMapper mapper = new ObjectMapper();
        jp.nextToken();

        JSONManifest manifest = mapper.readValue(jp, JSONManifest.class);

        assertFalse(manifest.checkSchema());
    }

    /**
     * manifest_jsonのschema値がnull場合falseが返却されること.
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void manifest_jsonのschema値がnull場合falseが返却されること() throws IOException {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        json.put("schema", null);
        JsonParser jp = f.createJsonParser(json.toJSONString());
        ObjectMapper mapper = new ObjectMapper();
        jp.nextToken();

        JSONManifest manifest = mapper.readValue(jp, JSONManifest.class);

        assertFalse(manifest.checkSchema());
    }

    /**
     * manifest_jsonのschemaの指定がない場合falseが返却されること.
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void manifest_jsonのschemaの指定がない場合falseが返却されること() throws IOException {
        JsonFactory f = new JsonFactory();
        JSONObject json = new JSONObject();
        json.put("bar_version", "1");
        json.put("box_version", "1");
        json.put("DefaultPath", "boxName");
        JsonParser jp = f.createJsonParser(json.toJSONString());
        ObjectMapper mapper = new ObjectMapper();
        jp.nextToken();

        JSONManifest manifest = mapper.readValue(jp, JSONManifest.class);

        assertFalse(manifest.checkSchema());
    }
}
