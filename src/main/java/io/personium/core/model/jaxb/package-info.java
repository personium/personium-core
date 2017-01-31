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
/**
 * package to store JAXB objects.
 */
// Package level annotation
// このアノテーションで以下の名前空間のプレフィックスのマッピング設定する
@javax.xml.bind.annotation.XmlSchema (
        xmlns = {
                // "DAV:"はプレフィックス指定なしの場合、marshall実行時にJAXBが"ns1"のように機械的なプレフィックスを付けるため、空白文字を指定
                @javax.xml.bind.annotation.XmlNs(prefix = "", namespaceURI = "DAV:"),
                // "urn:x-personium:xmlns"は"dc"で固定
                @javax.xml.bind.annotation.XmlNs(prefix = "dc", namespaceURI = "urn:x-personium:xmlns")
        }
)
package io.personium.core.model.jaxb;

