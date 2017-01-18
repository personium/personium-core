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
package com.fujitsu.dc.test.unit.core.bar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

import org.apache.wink.webdav.model.Multistatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.bar.BarFileReadRunner;
import com.fujitsu.dc.test.categories.Unit;

/**
 * BarFileのバリデートのユニットテストクラス.
 */
@Category({Unit.class })
public class RootpropsValidateTest {

    /**
     * .
     */
    private class TestBarRunner extends BarFileReadRunner {
        TestBarRunner() {
            super(null, null, null, null, null, null, null, null);
        }

        /**
         * 90_rootprops.xmlに定義されたpathの階層構造に矛盾がないことを検証する.
         * @param multiStatus 90_rootprops.xmlとして定義された文字列
         * @return 矛盾がない場合はtrueを、矛盾がある場合はfalseを返す。
         * @throws IOException XMLパースに失敗した場合にスローされる。
         */
        protected boolean validateCollectionDefinitions(String multiStatusInputStr) throws IOException {
            StringReader reader = new StringReader(multiStatusInputStr);
            Multistatus multiStatus = Multistatus.unmarshal(reader);
            return super.validateCollectionDefinitions(multiStatus, "bar/00_meta/90_rootprops.xml");
        }

    }

    /**
     * 共通_hrefタグがない場合にfalseが返却されること.
     */
    @Test
    public void 共通_hrefタグがない場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_href属性値が空文字の場合にfalseが返却されること.
     */
    @Test
    public void 共通_href属性値が空文字の場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href/>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_href属性値がdcboxコロンで始まらない場合にfalseが返却されること.
     */
    @Test
    public void 共通_href属性値がdcboxコロンで始まらない場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("dcbox", "box");
            assertFalse(runner.validateCollectionDefinitions(input2));
            String input3 = multiStatusInputStr.replace("dcbox", "http://localhost:8080/dc1-core/cell/box/col");
            assertFalse(runner.validateCollectionDefinitions(input3));
            String input4 = multiStatusInputStr.replace("dcbox", "dcbox://");
            assertFalse(runner.validateCollectionDefinitions(input4));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_Boxルートパスの定義がない場合にfalseが返却されること.
     */
    @Test
    public void 共通_Boxルートパスの定義がない場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_Boxルートのみ定義した場合にtrueが返却されること.
     */
    @Test
    public void 共通_Boxルートのみ定義した場合にtrueが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertTrue(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("dcbox:", "dcbox:/");
            assertTrue(runner.validateCollectionDefinitions(input2));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * WebDAV_href属性値に存在しないパスを指定した場合にfalseが返却されること.
     */
    @Test
    public void WebDAV_href属性値に存在しないパスを指定した場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
               + "    <response>"
                + "        <href>dcbox:/notexists/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * OData_href属性値に存在しないパスを指定した場合にfalseが返却されること.
     */
    @Test
    public void OData_href属性値に存在しないパスを指定した場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/notexists/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に存在しないパスを指定した場合にfalseが返却されること.
     */
    @Test
    public void Service_href属性値に存在しないパスを指定した場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/notexists/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "                <acl xml:base=\"http://localhost:9998/testcell1/__role/__/\""
                + "                 xmlns:dc=\"urn:x-dc1:xmlns\" />"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * WebDAVFile_href属性値に存在しないパスを指定した場合にfalseが返却されること.
     */
    @Test
    public void WebDAVFile_href属性値に存在しないパスを指定した場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/notexists/collection/webdav.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * OData_href属性値に指定されたパスの配下に別のパス定義が存在する場合にfalseが返却されること.
     */
    @Test
    public void OData_href属性値に指定されたパスの配下に別のパス定義が存在する場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/odatacol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:odata xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/odatacol/webdav.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に指定されたパスの配下に__src以外のパス定義が存在する場合にfalseが返却されること.
     */
    @Test
    public void Service_href属性値に指定されたパスの配下に__src以外のパス定義が存在する場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol/_src</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に指定されたパスの配下にWebDAVコレクション以外の__srcパス定義が存在する場合にfalseが返却されること.
     */
    @Test
    public void Service_href属性値に指定されたパスの配下にWebDAVコレクション以外の__srcパス定義が存在する場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol/__src</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:odata xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("dc:odata", "dc:service");
            assertFalse(runner.validateCollectionDefinitions(input2));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に指定されたパスの配下にWebDAVコレクションの__srcパス定義が存在する場合にtrueが返却されること.
     */
    @Test
    public void Service_href属性値に指定されたパスの配下にWebDAVコレクションの__srcパス定義が存在する場合にtrueが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol/__src</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertTrue(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に__srcパスが定義されずにサービスソースの定義が存在する場合にfalseが返却されること.
     */
    @Test
    public void Service_href属性値に__srcパスが定義されずにサービスソースの定義が存在する場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol/__src/test.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Service_href属性値に指定されたパスの配下に__srcパスの定義が存在しない場合にfalseが返却されること.
     */
    @Test
    public void Service_href属性値に指定されたパスの配下に__srcパスの定義が存在しない場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/servicecol</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * WebDAVFile_href属性値に指定されたパスの配下にパスの定義が存在する場合にfalseが返却されること.
     */
    @Test
    public void WebDAVFile_href属性値に指定されたパスの配下にパスの定義が存在する場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/webdavfile-1.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/webdavfile-1.js/webdavfile-2.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * WebDAVFile_href属性値に指定されたリソース名に不正な場合にfalseが返却されること.
     */
    @Test
    public void WebDAVFile_href属性値に指定されたリソース名に不正な場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/webdavcollection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/webdavcollection/test:test.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("test:test.js", "test test.js");
            assertFalse(runner.validateCollectionDefinitions(input2));
            String input3 = multiStatusInputStr.replace("test:test.js", "");
            assertFalse(runner.validateCollectionDefinitions(input3));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_href属性値に指定されたコレクション名が不正な場合にfalseが返却されること.
     */
    @Test
    public void 共通_href属性値に指定されたコレクション名が不正な場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collec tion</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                    <dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("dc:service", "dc:odata");
            assertFalse(runner.validateCollectionDefinitions(input2));
            String input3 = multiStatusInputStr.replace("<dc:service xmlns:dc=\"urn:x-dc1:xmlns\"/>", "");
            assertFalse(runner.validateCollectionDefinitions(input3));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_href属性値に指定されたコレクションパスが重複している場合にfalseが返却されること.
     */
    @Test
    public void 共通_href属性値に指定されたコレクションパスが重複している場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
            String input2 = multiStatusInputStr.replace("<href>dcbox:/collection</href>",
                    "<href>dcbox:/collection/</href>");
            assertFalse(runner.validateCollectionDefinitions(input2));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * 共通_href属性値に指定されたファイルパスが重複している場合にfalseが返却されること.
     */
    @Test
    public void 共通_href属性値に指定されたファイルパスが重複している場合にfalseが返却されること() {
        String multiStatusInputStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<multistatus xmlns=\"DAV:\">"
                + "    <response>"
                + "        <href>dcbox:/</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collection</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <resourcetype>"
                + "                    <collection/>"
                + "                </resourcetype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collection/conflict.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "    <response>"
                + "        <href>dcbox:/collection/conflict.js</href>"
                + "        <propstat>"
                + "            <prop>"
                + "                <getcontenttype>text/javascript</getcontenttype>"
                + "            </prop>"
                + "        </propstat>"
                + "    </response>"
                + "</multistatus>";
        TestBarRunner runner = new TestBarRunner();
        try {
            assertFalse(runner.validateCollectionDefinitions(multiStatusInputStr));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
