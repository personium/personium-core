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
package io.personium.core.rs.odata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.test.categories.Unit;

/**
 */
@Category({ Unit.class })
public class DecideOutputFormatTest {

    /**
     * format指定なしaccept指定なしでxmlが返却されること.
     */
    @Test
    public final void format指定なしaccept指定なしでxmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(null, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにアスタリスクを指定した場合xmlが返却されること.
     */
    @Test
    public final void format指定なしでacceptにアスタリスクを指定した場合xmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.WILDCARD, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにJSONを指定した場合JSONが返却されること.
     */
    @Test
    public final void format指定なしでacceptにJSONを指定した場合JSONが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.APPLICATION_JSON, null);
        assertEquals(MediaType.APPLICATION_JSON_TYPE, type);
    }

    /**
     * format指定なしでacceptにXMLを指定した場合XMLが返却されること.
     */
    @Test
    public final void format指定なしでacceptにXMLを指定した場合XMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.APPLICATION_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにATOM_XMLを指定した場合XMLが返却されること.
     */
    @Test
    public final void format指定なしでacceptにATOM_XMLを指定した場合XMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにJSONとATOM_XMLを指定した場合XMLが返却されること.
     */
    @Test
    public final void format指定なしでacceptにJSONとATOM_XMLを指定した場合XMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにJSONとATOM_XMLを空白ありで指定した場合XMLが返却されること.
     */
    @Test
    public final void format指定なしでacceptにJSONとATOM_XMLを空白ありで指定した場合XMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_JSON + " , " + MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにJSONとATOM_XMLをタブありで指定した場合XMLが返却されること.
     */
    @Test
    public final void format指定なしでacceptにJSONとATOM_XMLをタブありで指定した場合XMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_JSON + "\t,\t" + MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * acceptのセミコロン以降を無視すること.
     */
    @Test
    public final void acceptのセミコロン以降を無視すること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                "application/xml;q=0.9,*/*;q=0.8", null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * acceptの空白とセミコロン以降を無視すること.
     */
    @Test
    public final void acceptの空白とセミコロン以降を無視すること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                "application/xml ;q=0.9,*/* ;q=0.8", null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * acceptのタブとセミコロン以降を無視すること.
     */
    @Test
    public final void acceptのタブとセミコロン以降を無視すること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                "application/xml\t;q=0.9,*/*\t;q=0.8", null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptに未サポートの値を指定した場合415エラーが返却されること.
     */
    @Test
    public final void format指定なしでacceptに未サポートの値を指定した場合415エラーが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        try {
            odataEntityResource.decideOutputFormat("INVALID_VALUE", null);
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals("PR415-OD-0001", e.getCode());
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * format指定なしでacceptにxmlとatomを指定した場合xmlが返却されること.
     */
    @Test
    public final void format指定なしでacceptにxmlとatomを指定した場合xmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_XML + "," + MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにxmlとJSONを指定した場合xmlが返却されること.
     */
    @Test
    public final void format指定なしでacceptにxmlとJSONを指定した場合xmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_ATOM_XML, null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにxmlと未サポートの値を指定した場合415エラーが返却されること.
     */
    @Test
    public final void format指定なしでacceptにxmlと未サポートの値を指定した場合xmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(
                     MediaType.APPLICATION_ATOM_XML + "," + "INVALID_VALUE", null);
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * format指定なしでacceptにjsonとtext/plainと*を指定した場合jsonが返却されること.
     */
    @Test
    public final void format指定なしでacceptにjsonとtextとアスタリスクを指定した場合jsonが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();
        MediaType type = odataEntityResource.decideOutputFormat(
                MediaType.APPLICATION_JSON + "," + MediaType.TEXT_PLAIN_TYPE +  "," + MediaType.WILDCARD, null);
        assertEquals(MediaType.APPLICATION_JSON_TYPE, type);
    }

    /**
     * formatにatomを指定しaccept指定なしでXMLが返却されること.
     */
    @Test
    public final void formatにatomを指定しaccept指定なしでXMLが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(null, "atom");
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * formatにJSONを指定しaccept指定なしでJSONが返却されること.
     */
    @Test
    public final void formatにJSONを指定しaccept指定なしでJSONが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(null, "json");
        assertEquals(MediaType.APPLICATION_JSON_TYPE, type);
    }

    /**
     * formatにatomを指定しacceptにJSONでxmlが返却されること.
     */
    @Test
    public final void formatにatomを指定しacceptにJSONでxmlが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.APPLICATION_JSON, "atom");
        assertEquals(MediaType.APPLICATION_ATOM_XML_TYPE, type);
    }

    /**
     * formatにJSONを指定しacceptにxml指定でJSONが返却されること.
     */
    @Test
    public final void formatにJSONを指定しacceptにxml指定でJSONが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        MediaType type = odataEntityResource.decideOutputFormat(MediaType.APPLICATION_ATOM_XML, "json");
        assertEquals(MediaType.APPLICATION_JSON_TYPE, type);
    }

    /**
     * formatに未サポートの値を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void formatに未サポートの値を指定した場合に400エラーが返却されること() {
        ODataEntityResource odataEntityResource = new ODataEntityResource();

        try {
            odataEntityResource.decideOutputFormat(null, "INVALID_VALUE");
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals("PR400-OD-0005", e.getCode());
        } catch (Exception e) {
            fail();
        }
    }
}
