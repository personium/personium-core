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
package io.personium.core.rs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import io.personium.common.auth.token.LocalToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreLog;


/**
 * Personium-coreの/_cell_/* 以下URLを担当するJAX-RSのApplication.
 */
public class DcCoreApplication extends Application {
    static {
        try {
            TransCellAccessToken.configureX509(DcCoreConfig.getX509PrivateKey(), DcCoreConfig.getX509Certificate(),
                    DcCoreConfig.getX509RootCertificate());
            LocalToken.setKeyString(DcCoreConfig.getTokenSecretKey());
        } catch (Exception e) {
            DcCoreLog.Server.FAILED_TO_START_SERVER.reason(e).writeLog();
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.ws.rs.core.Application#getClasses()
     */
    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(DcCoreExceptionMapper.class);
        // classes.add(UnitCtlResource.class);
        // classes.add(CellResource.class);
        classes.add(FacadeResource.class);
        // classes.add(CellCtlResource.class);
        // classes.add(BoxResource.class);

        return classes;
    }
}
