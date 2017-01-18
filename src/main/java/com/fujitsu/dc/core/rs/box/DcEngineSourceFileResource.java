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
package com.fujitsu.dc.core.rs.box;

import java.io.Reader;

import javax.ws.rs.core.Response;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.annotations.ACL;
import com.fujitsu.dc.core.auth.BoxPrivilege;
import com.fujitsu.dc.core.model.DavCmp;
import com.fujitsu.dc.core.model.DavRsCmp;

/**
 * DcEngineのソースファイルリソースに対応するJAX-RS Resource クラス.
 */
public final class DcEngineSourceFileResource extends DavFileResource {

    /**
     * constructor.
     * @param parent 親
     * @param davCmp 部品
     */
    public DcEngineSourceFileResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent, davCmp);
    }

    /**
     * ACLメソッドの処理. ACLの設定を行う.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    @ACL
    public Response acl(final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        throw DcCoreException.Dav.METHOD_NOT_ALLOWED;
    }
}
