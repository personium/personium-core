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
package io.personium.core.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class managing Dav's path information.
 */
public class DavPath {

    private static final int RESOURCE_INDEX = 3;
    private static final int BOX_INDEX = 2;
    private static final int CELL_INDEX = 1;

    private URI uri;
    private String baseUri = null;
    private String cellName = null;
    private String boxName = null;
    private List<String> resourcePath = new ArrayList<String>();

    /**
     * constructor.
     * @param uri Dav URI
     * @param baseUri BaseURI
     */
    public DavPath(URI uri, String baseUri) {
        this.uri = uri;

        String path = this.uri.toString();
        Pattern pattern = Pattern.compile(baseUri + "([^/]+)/([^/]+)/(.+)");
        Matcher matcher = pattern.matcher(path);
        if (!matcher.find()) {
            return;
        }
        this.baseUri = baseUri;
        this.cellName = matcher.group(CELL_INDEX);
        this.boxName = matcher.group(BOX_INDEX);
        if (matcher.group(RESOURCE_INDEX) == null) {
            return;
        }
        String[] resourcePaths = matcher.group(RESOURCE_INDEX).split("/");
        for (int i = 0; i < resourcePaths.length; i++) {
            this.resourcePath.add(resourcePaths[i]);
        }
    }

    /**
     * @return baseURI string
     */
    public String getBaseUri() {
        return this.baseUri;
    }

    /**
     * @return Cell Name
     */
    public String getCellName() {
        return this.cellName;
    }

    /**
     * @return Box name
     */
    public String getBoxName() {
        return this.boxName;
    }

    /**
     * Path of resource under.
     * @return Box
     */
    public List<String> getResourcePath() {
        return this.resourcePath;
    }
}
