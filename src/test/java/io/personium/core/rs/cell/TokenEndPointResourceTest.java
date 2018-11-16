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
package io.personium.core.rs.cell;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.CellLocalRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.core.model.Cell;
import io.personium.test.categories.Unit;

/**
 * TokenEndPointResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TokenEndPointResource.class, CellLocalRefreshToken.class, TransCellRefreshToken.class,
    AbstractOAuth2Token.class })
@Category({ Unit.class })
public class TokenEndPointResourceTest {

    /** Target class of unit test. */
    private TokenEndPointResource tokenEndPointResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        tokenEndPointResource = spy(new TokenEndPointResource(null, null));
    }

    /**
     * Test receiveRefresh().
     * RefreshToken is CellLocalToken.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void receiveRefresh_Normal_cell_local_token() throws Exception {
        Cell mockCell = mock(Cell.class);
        tokenEndPointResource = PowerMockito.spy(new TokenEndPointResource(mockCell, null));

        // --------------------
        // Test method args
        // --------------------
        String target = "";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String cellUrl = "https://personium/testcell/";
        String host = "https://personium/";
        String refreshToken = "RA~TEST_REFRESH_TOKEN";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.doReturn(cellUrl).when(tokenEndPointResource, "getIssuerUrl");
        doReturn(host).when(mockCell).getUnitUrl();
        CellLocalRefreshToken mockOldRToken = PowerMockito.mock(CellLocalRefreshToken.class);
        PowerMockito.mockStatic(AbstractOAuth2Token.class);
        PowerMockito.when(AbstractOAuth2Token.class,
                "parse", refreshToken, cellUrl, host).thenReturn(mockOldRToken);

        PowerMockito.doReturn(false).when(mockOldRToken).isRefreshExpired();

        CellLocalRefreshToken mockNewRToken = PowerMockito.mock(CellLocalRefreshToken.class);
        doReturn(mockNewRToken).when(mockOldRToken).refreshRefreshToken(anyLong());

        PowerMockito.doReturn("subject").when(mockNewRToken).getSubject();

        List<Role> roleList = new ArrayList<Role>();
        doReturn(roleList).when(mockCell).getRoleListForAccount("subject");

        CellLocalAccessToken mockNewAToken = mock(CellLocalAccessToken.class);
        PowerMockito.doReturn(mockNewAToken).when(mockNewRToken).refreshAccessToken(
                anyLong(), anyString(), anyString(), anyList(), anyString());

        Response response = Response.ok().build();
        PowerMockito.doReturn(response).when(tokenEndPointResource, "responseAuthSuccess",
                mockNewAToken, mockNewRToken);

        // --------------------
        // Expected result
        // --------------------
        Response expected = Response.ok().build();

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema, host, refreshToken);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actual.getStatus(), is(expected.getStatus()));
    }

    /**
     * Test receiveRefresh().
     * RefreshToken is TransCellAccessToken.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void receiveRefresh_Normal_trans_cell_access_token() throws Exception {
        Cell mockCell = mock(Cell.class);
        tokenEndPointResource = PowerMockito.spy(new TokenEndPointResource(mockCell, null));

        // --------------------
        // Test method args
        // --------------------
        String target = "https://personium/testcell/";
        String owner = "false";
        String schema = "https://personium/appcell/";
        String cellUrl = "https://personium/testcell/";
        String host = "https://personium/";
        String refreshToken = "RA~TEST_REFRESH_TOKEN";

        // --------------------
        // Mock settings
        // --------------------
        PowerMockito.doReturn(cellUrl).when(tokenEndPointResource, "getIssuerUrl");
        doReturn(host).when(mockCell).getUnitUrl();
        TransCellRefreshToken mockOldRToken = PowerMockito.mock(TransCellRefreshToken.class);
        PowerMockito.mockStatic(AbstractOAuth2Token.class);
        PowerMockito.when(AbstractOAuth2Token.class,
                "parse", refreshToken, cellUrl, host).thenReturn(mockOldRToken);

        PowerMockito.doReturn(false).when(mockOldRToken).isRefreshExpired();

        TransCellRefreshToken mockNewRToken = PowerMockito.mock(TransCellRefreshToken.class);
        doReturn(mockNewRToken).when(mockOldRToken).refreshRefreshToken(anyLong());

        List<Role> roleList = new ArrayList<Role>();
        doReturn(roleList).when(mockCell).getRoleListHere(mockNewRToken);

        CellLocalAccessToken mockNewAToken = mock(CellLocalAccessToken.class);
        PowerMockito.doReturn(mockNewAToken).when(mockNewRToken).refreshAccessToken(
                anyLong(), anyString(), anyString(), anyList(), anyString());

        Response response = Response.ok().build();
        PowerMockito.doReturn(response).when(tokenEndPointResource, "responseAuthSuccess",
                mockNewAToken, mockNewRToken);

        // --------------------
        // Expected result
        // --------------------
        Response expected = Response.ok().build();

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema, host, refreshToken);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actual.getStatus(), is(expected.getStatus()));
    }
}
