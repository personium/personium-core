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
package io.personium.core.rs.box;

import java.io.Reader;

import javax.ws.rs.core.Response;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;

/**
 * JAX-RS Resource class corresponding to Personium Engine source file resources.
 */
public final class PersoniumEngineSourceFileResource extends DavFileResource {

    /**
     * constructor.
     * @param parent parent
     * @param davCmp parts
     */
    public PersoniumEngineSourceFileResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent, davCmp);
    }

    /**
     * Processing of ACL method Set ACL.
     * @param reader configuration XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
    }
}
