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
import javax.xml.bind.annotation.XmlType;

import com.fujitsu.dc.common.utils.DcCoreUtils;

/**
 * Propertyを扱うオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class Property {
    /**
     * コンストラクタ.
     */
    public Property() {
    }

    Property(String name, String type, String nullable, String defaultValue, String precision) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.precision = precision;
    }

    Property(String name, String type, String nullable, String defaultValue) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
    }

    Property(String name, String type, String nullable) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    Property setDefaultValue(String defValue) {
        this.defaultValue = defValue;
        return this;
    }

    Property setUnique(String uniq) {
        this.unique = uniq;
        return this;
    }

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(name = "Type")
    String type;

    @XmlAttribute(name = "Nullable")
    String nullable;

    @XmlAttribute(name = "DefaultValue")
    String defaultValue;

    @XmlAttribute(name = "Precision")
    String precision;

    @XmlAttribute(namespace = DcCoreUtils.XmlConst.NS_DC1, name = "Unique")
    String unique;

    @Override
    public boolean equals(Object obj) {
        Property property = (Property) obj;
        if (!name.equals(property.name)) {
            Edmx.printResult(this, "Name", property.name, name);
            return false;
        }
        if (!type.equals(property.type)) {
            Edmx.printResult(this, "Type", property.type, type);
            return false;
        }
        if (!nullable.equals(property.nullable)) {
            Edmx.printResult(this, "Nullable", property.nullable, nullable);
            return false;
        }
        if ((defaultValue == null && property.defaultValue != null)
                || (defaultValue != null && !defaultValue.equals(property.defaultValue))) {
            Edmx.printResult(this, "DefaultValue", property.defaultValue, defaultValue);
            return false;
        }
        if ((precision == null && property.precision != null)
                || (precision != null && !precision.equals(property.precision))) {
            Edmx.printResult(this, "Precision", property.precision, precision);
            return false;
        }
        if ((unique == null && property.unique != null)
                || (unique != null && !unique.equals(property.unique))) {
            Edmx.printResult(this, "Unique", property.unique, unique);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityType => Name:").append(name).append(",Type:").append(type);
        builder.append(",Nullable:").append(nullable).append(",DefaultValue:").append(defaultValue);
        builder.append(",Precision:").append(precision).append(",Unique:").append(unique).append("\n");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
