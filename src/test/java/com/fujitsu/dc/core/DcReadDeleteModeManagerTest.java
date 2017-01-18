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
package com.fujitsu.dc.core;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.PathSegment;

import org.apache.wink.common.internal.PathSegmentImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fujitsu.dc.core.model.lock.ReadDeleteModeLockManager;

/**
 * PCSの動作モードを参照するクラスのテスト.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(ReadDeleteModeLockManager.class)
public class DcReadDeleteModeManagerTest {
    /**
     * ReadDeleteOnlyモード時にGETメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にGETメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.GET, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にDELETEメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にDELETEメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.DELETE, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にPROPFINDメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にPROPFINDメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にOPTIONSメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にOPTIONSメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.OPTIONS, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にHEADメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にHEADメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.HEAD, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にREPORTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時にREPORTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode("REPORT", pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時にPOSTメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にPOSTメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);
    }

    /**
     * ReadDeleteOnlyモード時にPUTメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にPUTメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.PUT, pathSegment);
    }

    /**
     * ReadDeleteOnlyモード時にMERGEメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にMERGEメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MERGE,
                pathSegment);
    }

    /**
     * ReadDeleteOnlyモード時にMKCOLメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にMKCOLメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MKCOL,
                pathSegment);
    }

    /**
     * ReadDeleteOnlyモード時にPROPPATCHメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にPROPPATCHメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH,
                pathSegment);
    }

    /**
     * ReadDeleteOnlyモード時にACLメソッドが実行された場合はDcCoreExceptionが発生すること.
     * @throws Exception .
     */
    @Test(expected = DcCoreException.class)
    public void ReadDeleteOnlyモード時にACLメソッドが実行された場合は503が返却されること() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.ACL,
                pathSegment);
    }

    /**
     * ReadDeleteOnlyモードではない状態でPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモードではない状態でPUTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でPUTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.PUT, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモードではない状態でMERGEメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でMERGEメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MERGE,
                    pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモードではない状態でMKCOLメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でMKCOLメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MKCOL,
                    pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモードではない状態でPROPPATCHメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でPROPPATCHメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモードではない状態でACLメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモードではない状態でACLメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(false);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "odata", "entity" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.ACL,
                    pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時に__authに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時に__authに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "__auth" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時に__authzに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時に__authzに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "__authz" });
        try {
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * ReadDeleteOnlyモード時に$batchに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと.
     * @throws Exception .
     */
    @Test
    public void ReadDeleteOnlyモード時に$batchに対するPOSTメソッドが実行された場合はDcCoreExceptionが発生しないこと() throws Exception {
        PowerMockito.spy(ReadDeleteModeLockManager.class);
        PowerMockito.when(ReadDeleteModeLockManager.class, "isReadDeleteOnlyMode").thenReturn(true);
        try {

            List<PathSegment> pathSegment = getPathSegmentList(new String[] {"cell", "box", "col", "odata", "$batch" });
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);

            // 1階層パスを深くしても例外が発生しないことを確認
            pathSegment = getPathSegmentList(new String[] {"cell", "box", "col", "col", "odata", "$batch" });
            DcReadDeleteModeManager.checkReadDeleteOnlyMode(HttpMethod.POST, pathSegment);
        } catch (DcCoreException e) {
            fail(e.getMessage());
        }
    }

    private List<PathSegment> getPathSegmentList(String[] pathSegmentList) {
        List<PathSegment> pathSegment = new ArrayList<PathSegment>();
        for (String path : pathSegmentList) {
            pathSegment.add(new PathSegmentImpl(path));
        }
        return pathSegment;
    }
}
