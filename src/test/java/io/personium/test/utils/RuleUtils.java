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

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.test.jersey.AbstractCase;

/**
 * Utility class for Rule.
 */
public class RuleUtils {
    private RuleUtils() {
    }

    /**
     * Return complex key of rule without box name.
     * @param ruleName rule name
     * @return string of the rule's complex key
     */
    public static String keyString(String ruleName) {
        return "Name='" + ruleName + "'";
    }

    /**
     * Return complex key of rule.
     * @param ruleName rule name
     * @param boxName box name
     * @return string of the rule's complex key
     */
    public static String keyString(String ruleName, String boxName) {
        return "Name='" + ruleName + "',_Box.Name='" + boxName + "'";
    }

    /**
     * Get a rule in a cell.
     * @param cellName cell name
     * @param token token string
     * @param ruleName rule name
     * @param boxName box name
     * @param sc status code
     * @return response
     */
    public static TResponse get(String cellName, String token, String ruleName,
            String boxName, int sc) {
        TResponse res = Http.request("rule-retrieve.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rulename", ruleName)
                .with("boxname", "'" + boxName + "'")
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Get list of rules in a cell.
     * @param token token string
     * @param cellName cell name
     * @param sc status code
     * @return response
     */
    public static TResponse list(String token, String cellName, int sc) {
        TResponse res = Http.request("rule-list.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Get list of rules in a cell with query.
     * @param token token string
     * @param cellName cell name
     * @param query query string
     * @param sc status code
     * @return response
     */
    public static TResponse list(String token, String cellName, String query, int sc) {
        TResponse res = Http.request("rule-list-with-query.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("query", query)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Get list of rules.
     * List is internal detail information.
     * @param token token string
     * @param cellName cell name
     * @param sc status code
     * @return response
     */
    public static TResponse listDetail(String token, String cellName, int sc) {
        TResponse res = Http.request("rule-list-detail.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Create a rule.
     * @param cellName cell name
     * @param token token
     * @param body body
     * @param code response code
     * @return response
     */
    public static TResponse create(final String cellName, final String token,
            final JSONObject body, final int code) {
        return Http.request("rule-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
    }


    /**
     * Update rule.
     * @param token token string
     * @param cellName cell name
     * @param ruleName rule name
     * @param boxName box name
     * @param body body
     * @param sc status code
     * @return response
     */
    public static TResponse update(String token, String cellName, String ruleName,
            String boxName, final JSONObject body, final int sc) {
        String boxNameStr = null;
        if (boxName != null) {
            boxNameStr = "'" + boxName + "'";
        } else {
            boxNameStr = "null";
        }
        TResponse res = Http.request("cell/rule-update.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("rulename", ruleName)
                .with("boxname", boxNameStr)
                .with("body", body.toString())
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Delete rule.
     * @param cellName cell name
     * @param token token string
     * @param ruleName rule name
     * @param boxName box name
     */
    public static void delete(final String cellName, final String token,
            final String ruleName, final String boxName) {
        String keyBoxName = null;
        if (boxName == null) {
            keyBoxName = "null";
        } else {
            keyBoxName = "'" + boxName + "'";
        }
        Http.request("rule-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("rulename", ruleName)
                .with("boxname", keyBoxName)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Delete rule.
     * @param cellName cell name
     * @param token token string
     * @param ruleName rule name
     * @param boxName box name
     * @param code response code
     */
    public static void delete(final String cellName, final String token,
            final String ruleName, final String boxName, final int code) {
        String keyBoxName = null;
        if (boxName == null) {
            keyBoxName = "null";
        } else {
            keyBoxName = "'" + boxName + "'";
        }

        Http.request("rule-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("rulename", ruleName)
                .with("boxname", keyBoxName)
                .returns()
                .statusCode(code);
    }

}
