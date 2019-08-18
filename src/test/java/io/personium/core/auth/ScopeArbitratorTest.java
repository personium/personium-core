package io.personium.core.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.personium.core.model.Box;
import io.personium.core.model.Cell;

public class ScopeArbitratorTest {
    Cell mockCell = mock(Cell.class);
    Box mockBox = mock(Box.class);

    @Before
    public void setUp() throws Exception {
        doReturn("https://personium.example/").when(mockCell).getUnitUrl();

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
        ScopeArbitrator sa  = new ScopeArbitrator(this.mockCell, this.mockBox, true);
        sa.request("openid root root message foo https://personium.example/__role/__/someRole");
        String[] res = sa.getResults();
        System.out.println(StringUtils.join(sa.requestedScopes, " "));
        System.out.println(StringUtils.join(res, " "));
        assertEquals(3, res.length);
    }
    /**
     * When constructed with non-ROPC option, then any Cell level priviledge can not be allowed.
     */
    @Test
    public void When_NotROPC_Then_CellLevelPrivileges_CanNotBeAllowed () {
        ScopeArbitrator sa  = new ScopeArbitrator(this.mockCell, this.mockBox, false);
        sa.request("root message-read");
        String[] res = sa.getResults();
        assertEquals(0, res.length);
    }
}
