/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.test.utils;

import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.test.unit.core.UrlUtils;

/**
 * Links setting utils.
 */
public class LinksUtils {

    private LinksUtils() {
    }

    /**
     * Create link with ExtCell.
     * @param cellName Cell name
     * @param url Extcell url
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse createLinksExtCell(String cellName, String url,
            String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "'" + url + "'";
        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }
        String uri = UrlUtils.cellCtlWithoutSingleQuote(cellName, navProp, navKey);

        return createLinks(cellName, ExtCell.EDM_TYPE_NAME, key, navProp, uri, token, statusCode);
    }

    /**
     * Create link with ExtRole.
     * @param cellName Cell name
     * @param extRole Extrole url
     * @param relationName Extrole relation name
     * @param relationBoxName Extrole box name
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse createLinksExtRole(String cellName, String extRole, String relationName,
            String relationBoxName, String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "ExtRole='" + extRole + "'";
        if (relationName != null) {
            key += ",_Relation.Name='" + relationName + "'";
        }
        if (relationBoxName != null) {
            key += ",_Relation._Box.Name='" + relationBoxName + "'";
        }

        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }
        String uri = UrlUtils.cellCtlWithoutSingleQuote(cellName, navProp, navKey);

        return createLinks(cellName, ExtRole.EDM_TYPE_NAME, key, navProp, uri, token, statusCode);
    }

    /**
     * Create link.
     * @param cellName Cell name
     * @param entitySet Entity set
     * @param name Entity set name
     * @param boxName Entity set box name
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse createLinks(String cellName, String entitySet, String name, String boxName,
            String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "Name='" + name + "'";
        if (boxName != null) {
            key += ",_Box.Name='" + boxName + "'";
        }

        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }
        String uri = UrlUtils.cellCtlWithoutSingleQuote(cellName, navProp, navKey);

        return createLinks(cellName, entitySet, key, navProp, uri, token, statusCode);
    }

    /**
     * Create link.
     * @param cellName Cell name
     * @param entitySet Entity set
     * @param key Entity set key
     * @param navProp Navigation property
     * @param uri Navigation property uri
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse createLinks(String cellName, String entitySet, String key,
            String navProp, String uri, String token, int statusCode) {
        return Http.request("links-create.txt")
                .with("cellName", cellName)
                .with("entitySet", entitySet)
                .with("key", key)
                .with("navProp", "_" + navProp)
                .with("token", token)
                .with("uri", uri)
                .returns().statusCode(statusCode).debug();
    }

    /**
     * Delete link with ExtCell.
     * @param cellName Cell name
     * @param url Extcell url
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse deleteLinksExtCell(String cellName, String url,
            String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "'" + url + "'";
        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }
        return deleteLinks(cellName, ExtCell.EDM_TYPE_NAME, key, navProp, navKey, token, statusCode);
    }

    /**
     * Delete link with ExtRole.
     * @param cellName Cell name
     * @param extRole Extrole url
     * @param relationName Extrole relation name
     * @param relationBoxName Extrole box name
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse deleteLinksExtRole(String cellName, String extRole, String relationName,
            String relationBoxName, String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "ExtRole='" + extRole + "'";
        if (relationName != null) {
            key += ",_Relation.Name='" + relationName + "'";
        }
        if (relationBoxName != null) {
            key += ",_Relation._Box.Name='" + relationBoxName + "'";
        }

        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }

        return deleteLinks(cellName, ExtRole.EDM_TYPE_NAME, key, navProp, navKey, token, statusCode);
    }

    /**
     * Delete link.
     * @param cellName Cell name
     * @param entitySet Entity set
     * @param name Entity set name
     * @param boxName Entity set box name
     * @param navProp Navigation property
     * @param navName Navigation property name
     * @param navBoxName Navigation property box name
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse deleteLinks(String cellName, String entitySet, String name, String boxName,
            String navProp, String navName, String navBoxName, String token, int statusCode) {
        String key = "Name='" + name + "'";
        if (boxName != null) {
            key += ",_Box.Name='" + boxName + "'";
        }

        String navKey = "Name='" + navName + "'";
        if (navBoxName != null) {
            navKey += ",_Box.Name='" + navBoxName + "'";
        }
        return deleteLinks(cellName, entitySet, key, navProp, navKey, token, statusCode);
    }

    /**
     * Delete link.
     * @param cellName Cell name
     * @param entitySet Entity set
     * @param key Entity set key
     * @param navProp Navigation property
     * @param navKey Navigation key
     * @param token Token
     * @param statusCode Expecting status code
     * @return API response
     */
    public static TResponse deleteLinks(String cellName, String entitySet, String key,
            String navProp, String navKey, String token, int statusCode) {
        return Http.request("links-delete.txt")
                .with("cellName", cellName)
                .with("entitySet", entitySet)
                .with("key", key)
                .with("navProp", "_" + navProp)
                .with("navKey", navKey)
                .with("token", token)
                .returns().statusCode(statusCode).debug();
    }

}
