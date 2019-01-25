/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
package io.personium.core.rule.action;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.UriBuilder;

import io.personium.core.model.ctl.Common;
import io.personium.core.utils.ResourceUtils;

/**
 * Utilities for action of rule.
 */
public class ActionUtils {
    /**
     * Constructor.
     */
    private ActionUtils() {
    }

    /**
     * Get cell name from URI.
     * @param uri URI object
     * @return cell name
     */
    private static Optional<String> getCellName(URI uri) {
        // cell name is root of the path,
        String path = uri.getPath();
        return Stream.of(path.split("/"))
                     .filter(s -> !s.isEmpty())
                     .findFirst();
    }

    /**
     * Remove fragment.
     * @param url url string with fragment
     * @return url string without fragment
     */
    static String getUrl(String url) {
        try {
            return UriBuilder.fromUri(url)
                             .fragment(null)
                             .build()
                             .toString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Convert to cell url.
     * @param url url string with fragment
     * @return cell url string
     */
    static String getCellUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            String fragment = uri.getFragment();

            if (fragment == null) {
                // for compatibility
                Optional<String> cellName = getCellName(uri);
                path = cellName.map(cell -> "/" + cell + "/").orElse("/");
            } else {
                path = path.substring(0, path.length() - fragment.length());
            }

            return UriBuilder.fromUri(uri)
                             .replacePath(path)
                             .replaceQuery(null)
                             .fragment(null)
                             .build()
                             .toString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Get Entity as Map from response body.
     * @param body response body of entity
     * @return Map object
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> getEntityAsMap(String body) {
        Map<String, Object> map = ResourceUtils.convertToMap(body);
        Map<String, Object> d = (Map) map.get("d");
        Map<String, Object> results = (Map) d.get("results");
        // remove reserved key
        results.remove("__metadata");
        results.remove(Common.P_PUBLISHED.getName());
        results.remove(Common.P_UPDATED.getName());
        return results;
    }

}
