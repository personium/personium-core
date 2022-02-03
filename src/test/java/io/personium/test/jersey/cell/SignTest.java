package io.personium.test.jersey.cell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.setup.Setup;
import io.personium.test.utils.SignUtils;
import io.personium.test.utils.TResponse;

/**
 * Unit Test for SignResource with JerseyTestFramework
 */
@Category({Unit.class, Integration.class, Regression.class })
public class SignTest extends AbstractCase {

    /**
     * Constructor
     */
    public SignTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Function for checking JWS payload is match
     */
    private void checkContent(String strJWS, byte[] rawPayload) {
        String[] strParts = strJWS.split("\\.");
        if (strParts.length < 3) {
            fail("Not a JWS: " + strJWS);
        }
        try {
            assertArrayEquals(rawPayload, Base64UrlUtility.decode(strParts[1]));
        } catch (Base64Exception e) {
            e.printStackTrace();
            fail("An exception is happend in decoding base64: " + strParts[1]);
        }
    }

    /**
     * Test that cell can sign a byte array
     */
    @Test
    public void Test_that_cell_can_sign_bytes() {
        byte[] testBody = "example_text".getBytes(StandardCharsets.UTF_8);
        TResponse res = SignUtils.post(Setup.TEST_CELL1, MASTER_TOKEN_NAME, testBody, HttpStatus.SC_OK);
        checkContent(res.getBody(), testBody);
        System.out.println(res.getBody());
    }

    /**
     * Test that cell can sign a zero-length string
     */
    @Test
    public void Test_that_celL_can_sign_content_length_0() {
        byte[] testBody = {};
        TResponse res = SignUtils.post(Setup.TEST_CELL1, MASTER_TOKEN_NAME, testBody, HttpStatus.SC_OK);
        checkContent(res.getBody(), testBody);
    }

    /**
     * Test that cell reject request which accept header is not jose
     */
    @Test
    public void Test_that_cell_reject_accept_is_not_jose() {
        byte[] testBody = "example_text".getBytes(StandardCharsets.UTF_8);
        SignUtils.post(Setup.TEST_CELL1, MASTER_TOKEN_NAME, "application/json", testBody, HttpStatus.SC_NOT_ACCEPTABLE);
    }

    /**
     * Test that cell accept request which accept header is not specified
     */
    @Test
    public void Test_that_cell_can_sign_accept_is_not_specified() {
        byte[] testBody = "example_text".getBytes(StandardCharsets.UTF_8);
        TResponse res = SignUtils.post(Setup.TEST_CELL1, MASTER_TOKEN_NAME, "", testBody, HttpStatus.SC_OK);
        checkContent(res.getBody(), testBody);
    }

    /**
     * Test that cell accept request which accept header is specified by wildcard
     */
    @Test
    public void Test_that_cell_can_sign_accept_is_wildcard() {
        byte[] testBody = "example_text".getBytes(StandardCharsets.UTF_8);
        TResponse res = SignUtils.post(Setup.TEST_CELL1, MASTER_TOKEN_NAME, "*/*", testBody, HttpStatus.SC_OK);
        checkContent(res.getBody(), testBody);
    }
}
