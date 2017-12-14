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
package io.personium.core.bar.jackson;

import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONObject;

/**
 * Mapping class for reading 40_accounts.json file in bar zip file.
 */
public class JSONAccounts implements JSONMappedObject {
    /**
     * Name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Password.
     */
    @JsonProperty("Password")
    private String password;

    /**
     * Get value of Name.
     * @return value of Name
     */
    public String getName() { return name; }

    /**
     * Get value of Password.
     * @return value of Password
     */
    public String getPassword() { return password; }

    /**
     * Set value of Name.
     * @param name name string
     */
    public void setName(String name) { this.name = name; }

    /**
     * Set value of Password.
     * @param password password string
     */
    public void setPassword(String password) { this.password = password; }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("Name", this.name);
        json.put("Password", this.password);
        return json;
    }
}
