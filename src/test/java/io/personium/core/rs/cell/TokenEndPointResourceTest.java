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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.grizzly.utils.Charsets;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;

/**
 * TokenEndPointResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TokenEndPointResource.class, ResidentRefreshToken.class, VisitorRefreshToken.class,
    AbstractOAuth2Token.class })
@Category({ Unit.class })
@PowerMockIgnore({"javax.crypto.*" })
public class TokenEndPointResourceTest {

    /** Target class of unit test. */
    private TokenEndPointResource tokenEndPointResource;
    private Cell mockCell;
    private CellRsCmp mockCellRsCmp;

    @BeforeClass
    public static void beforeClass() {
        PersoniumCoreApplication.loadConfig();
        PersoniumCoreApplication.loadPlugins();

    }
    @AfterClass
    public static void afterClass() {

    }


    /**
     * Before.
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        String unitUrl = "https://personium/";
        String cellUrl = "https://personium/testcell/";

        this.mockCellRsCmp = mock(CellRsCmp.class);
        doReturn(null).when(this.mockCellRsCmp).getAccountsNotRecordingAuthHistory();
        doReturn(false).when(this.mockCellRsCmp).isRecordingAuthHistory(null, "username");

        this.mockCell = Mockito.spy(Cell.class);
        doReturn(unitUrl).when(this.mockCell).getUnitUrl();
        doReturn(cellUrl).when(this.mockCell).getUrl();
//        doReturn(null).when(this.mockCell).
        Map<String, String> o = new HashMap<>();
        o.put(Account.P_IP_ADDRESS_RANGE.getName(), null);
        o.put(Account.P_TYPE.getName(), Account.P_TYPE.getDefaultValue());
        OEntity oe = new OEntity() {
            EdmEntityType edmType = Account.EDM_TYPE_BUILDER.build();

            @Override
            public String getEntitySetName() {
                return Account.EDM_TYPE_NAME;
            }

            @Override
            public OEntityKey getEntityKey() {
                return OEntityKey.create(Account.P_NAME.getName(), "username");
            }

            @Override
            public List<OProperty<?>> getProperties() {
                Account.EDM_TYPE_BUILDER.build().getProperties();
                return null;
            }

            @Override
            public OProperty<?> getProperty(String propName) {
                String value = o.get(propName);
                return OProperties.string(propName, value);
            }

            @Override
            public <T> OProperty<T> getProperty(String propName, Class<T> propClass) {
                return null;
            }

            @Override
            public EdmType getType() {
                return this.edmType;
            }

            @Override
            public <TExtension extends OExtension<OEntity>> TExtension findExtension(Class<TExtension> clazz) {
                return null;
            }

            @Override
            public EdmEntitySet getEntitySet() {
                return EdmEntitySet.newBuilder().setName("Account").build();
            }

            @Override
            public EdmEntityType getEntityType() {
                return Account.EDM_TYPE_BUILDER.build();
            }

            @Override
            public List<OLink> getLinks() {
                // TODO 自動生成されたメソッド・スタブ
                return null;
            }

            @Override
            public <T extends OLink> T getLink(String title, Class<T> linkClass) {
                return null;
            }

        };
        OEntityWrapper oew = new OEntityWrapper(null, oe, "5678etag");
        doReturn(oew).when(this.mockCell).getAccount("username");
        doReturn(true).when(this.mockCell).authenticateAccount(oew, "password");


        this.tokenEndPointResource = PowerMockito.spy(new TokenEndPointResource(mockCell, this.mockCellRsCmp));
    }

    /**
     * Test receiveRefresh().
     * RefreshToken is CellLocalToken.
     * @throws Exception Unexpected error.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void receiveRefresh_Normal_cell_local_token() throws Exception {
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
        ResidentRefreshToken mockOldRToken = PowerMockito.mock(ResidentRefreshToken.class);
        PowerMockito.mockStatic(AbstractOAuth2Token.class);
        PowerMockito.when(AbstractOAuth2Token.class,
                "parse", refreshToken, cellUrl, host).thenReturn(mockOldRToken);

        PowerMockito.doReturn(false).when(mockOldRToken).isRefreshExpired();
        PowerMockito.doReturn(schema).when(mockOldRToken).getSchema();

        ResidentRefreshToken mockNewRToken = PowerMockito.mock(ResidentRefreshToken.class);
        doReturn(mockNewRToken).when(mockOldRToken).refreshRefreshToken(anyLong(), anyLong());

        PowerMockito.doReturn("subject").when(mockNewRToken).getSubject();

        List<Role> roleList = new ArrayList<Role>();
        doReturn(roleList).when(mockCell).getRoleListForAccount("subject");

        VisitorLocalAccessToken mockNewAToken = mock(VisitorLocalAccessToken.class);
        PowerMockito.doReturn(mockNewAToken).when(mockNewRToken).refreshAccessToken(
                anyLong(), anyLong(), anyString(), anyString(), anyList());

        Response response = Response.ok().build();
        PowerMockito.doReturn(response).when(tokenEndPointResource, "responseAuthSuccess",
                mockNewAToken, mockNewRToken, new Date().getTime());

        // --------------------
        // Expected result
        // --------------------
        Response expected = Response.ok().build();

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema, refreshToken,
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);

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
        VisitorRefreshToken mockOldRToken = PowerMockito.mock(VisitorRefreshToken.class);
        PowerMockito.mockStatic(AbstractOAuth2Token.class);
        PowerMockito.when(AbstractOAuth2Token.class,
                "parse", refreshToken, cellUrl, host).thenReturn(mockOldRToken);

        PowerMockito.doReturn(false).when(mockOldRToken).isRefreshExpired();
        PowerMockito.doReturn(schema).when(mockOldRToken).getSchema();

        VisitorRefreshToken mockNewRToken = PowerMockito.mock(VisitorRefreshToken.class);
        doReturn(mockNewRToken).when(mockOldRToken).refreshRefreshToken(anyLong(), anyLong());

        List<Role> roleList = new ArrayList<Role>();
        doReturn(roleList).when(mockCell).getRoleListHere(mockNewRToken);

        VisitorLocalAccessToken mockNewAToken = mock(VisitorLocalAccessToken.class);
        PowerMockito.doReturn(mockNewAToken).when(mockNewRToken).refreshAccessToken(
                anyLong(), anyLong(), anyString(), anyString(), anyList());

        Response response = Response.ok().build();
        PowerMockito.doReturn(response).when(tokenEndPointResource, "responseAuthSuccess",
                mockNewAToken, mockNewRToken, new Date().getTime());

        // --------------------
        // Expected result
        // --------------------
        Response expected = Response.ok().build();

        // --------------------
        // Run method
        // --------------------
        // Load methods for private
        Method method = TokenEndPointResource.class.getDeclaredMethod("receiveRefresh",
                String.class, String.class, String.class, String.class, long.class, long.class);
        method.setAccessible(true);
        // Run method
        Response actual = (Response) method.invoke(tokenEndPointResource, target, owner, schema, refreshToken,
                AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS,
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(actual.getStatus(), is(expected.getStatus()));
    }

    @Test
    public void testToken() throws Exception {
        String cellUrl = "https://personium/testcell/";
        String xForwadedFor = "1.2.3.4";

        //PowerMockito.doReturn(cellUrl).when(tokenEndPointResource, "getIssuerUrl");
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();
        formParams.add("grant_type", "password");
        formParams.add("username", "username");
        formParams.add("password", "password");
        formParams.add("scope", "root https://personium/appcell/");

        UriInfo uriInfo = mock(UriInfo.class);
        doReturn(new URI(cellUrl)).when(uriInfo).getBaseUri();

        Response res = tokenEndPointResource.token(uriInfo, null, formParams, xForwadedFor);
        JsonObject j = Json.createReader(new ByteArrayInputStream(res.getEntity().toString().getBytes(Charsets.UTF8_CHARSET))).readObject();
        System.out.println(j.getString("access_token"));
        assertEquals(200, res.getStatus());


    }
}
