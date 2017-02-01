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
package io.personium.core.model.ctl;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * SentMessagePort のEdm 定義体.
 */
public class SentMessagePort extends SentMessage {
    private SentMessagePort() {
        super();
    }

    /**
     * BoxBoundプロパティの定義体.
     */
    public static final EdmProperty.Builder P_BOX_BOUND = EdmProperty.newBuilder("BoxBound")
            .setType(EdmSimpleType.BOOLEAN)
            .setNullable(true);

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(Common.P_ID, P_BOX_BOUND, P_IN_REPLY_TO, P_TO, P_TO_RELATION,
                            P_TYPE, P_TITLE, P_BODY, P_PRIORITY, P_REQUEST_RELATION, P_REQUEST_RELATION_TARGET,
                            P_BOX_NAME, P_RESULT,
                            Common.P_PUBLISHED, Common.P_UPDATED).toList()).addKeys(Common.P_ID.getName());
}
