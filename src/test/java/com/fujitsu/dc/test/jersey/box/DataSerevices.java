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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * DataSerevices.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSerevices {
    /**
     * コンストラクタ.
     */
    public DataSerevices() {
    }

    DataSerevices(String dataServiceVersion, Schema schema) {
        this.dataServiceVersion = dataServiceVersion;
        this.schema = schema;
    }

    @XmlElement(namespace = "http://schemas.microsoft.com/ado/2006/04/edm", name = "Schema", type = Schema.class)
    Schema schema;

    @XmlAttribute(namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata",
            name = "DataServiceVersion")
    String dataServiceVersion;

    @Override
    public boolean equals(Object obj) {
        DataSerevices dServices = (DataSerevices) obj;
        if (!dataServiceVersion.equals(dServices.dataServiceVersion)) {
            Edmx.printResult(this, "DataServiceVersion", dServices.dataServiceVersion, dataServiceVersion);
            return false;
        }
        if (!schema.equals(dServices.schema)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DataSerevices => DataServiceVersion:").append(dataServiceVersion).append("\n");
        builder.append(schema.toString());
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
