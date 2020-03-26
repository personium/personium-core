package io.personium.core.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.model.Box;
import io.personium.core.model.Cell;

public class ScopeArbitratorTest {
    static Logger log = LoggerFactory.getLogger(ScopeArbitratorTest.class);
    Cell mockCell = mock(Cell.class);
    Box mockBox = mock(Box.class);
    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        doReturn("https://personium.example/").when(mockCell).getUnitUrl();
//        PersoniumUnitConfig mock = PowerMockito.spy(PersoniumUnitConfig.class);
//        PowerMockito.when(PersoniumUnitConfig.class, "getTokenDefaultScopeRopc").thenReturn("root");
//        PowerMockito.doReturn("").when(PersoniumUnitConfig.class, "getTokenDefaultScopeCode");
//        PowerMockito.doReturn("root").when(PersoniumUnitConfig.class, "getTokenDefaultScopeAssertion");

    }

    @After
    public void tearDown() throws Exception {
        this.mockCell = null;
        this.mockBox = null;
    }

    /**
     * When constructed with ROPC option, then any Cell level priviledge can be allowed.
     */
    @Test
    public void When_ROPC_Then_CellLevelPrivileges_CanBeAllowed () {
        ScopeArbitrator sa  = new ScopeArbitrator(this.mockCell, this.mockBox, OAuth2Helper.GrantType.PASSWORD);
        sa.unitMaxScopePrivilege = Privilege.get(CellPrivilege.class, "root");
        sa.requestString("openid root root message foo https://personium.example/__role/__/someRole");
        String[] res = sa.getResults();
        log.info(StringUtils.join(sa.requestedScopes, " "));
        log.info(StringUtils.join(res, " "));
        assertEquals(3, res.length);
    }
    /**
     * When constructed with ROPC option and no scope requested, then root is granted.
     */
    @Test
    public void When_ROPC_noScopeRequest_Then_RootGranted () {
        ScopeArbitrator sa  = new ScopeArbitrator(this.mockCell, this.mockBox, OAuth2Helper.GrantType.PASSWORD);
        sa.unitMaxScopePrivilege = Privilege.get(CellPrivilege.class, "root");
        sa.requestString(null);
        String[] res = sa.getResults();
        assertEquals("root", res[0]);
    }

    /**
     * When constructed with non-ROPC option, then any Cell level priviledge can not be allowed.
     */
    @Test
    public void When_NotROPC_Then_CellLevelPrivileges_CanNotBeAllowed () {

        ScopeArbitrator sa  = new ScopeArbitrator(this.mockCell, this.mockBox, OAuth2Helper.GrantType.AUTHORIZATION_CODE);
        sa.unitMaxScopePrivilege = Privilege.get(CellPrivilege.class, "");
        sa.requestString("root message-read");
        String[] res = sa.getResults();
        assertEquals(0, res.length);
    }
}
