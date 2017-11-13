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
package io.personium.core.jersey.filter;

import java.util.Collections;
import java.util.List;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

import io.personium.core.annotations.WriteAPI;

/**
 * Set filter processing for request/response of specific condition.
 */
public class PersoniumCoreResourceFilterFactory implements ResourceFilterFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        if (am.getAnnotation(WriteAPI.class) != null) {
            // Filter for @WriteAPI annotation.
            return Collections.<ResourceFilter>singletonList(new WriteMethodFilter());
        }
        return null;
    }
}
