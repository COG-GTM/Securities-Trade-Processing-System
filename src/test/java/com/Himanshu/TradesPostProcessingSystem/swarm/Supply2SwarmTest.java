package com.Himanshu.TradesPostProcessingSystem.swarm;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUPPLY-2: CVE-2025-48988 (unbounded multipart part count / part headers, Tomcat 10.1.0-M1 through 10.1.41).
 * Spring Boot 3.4.2 enables multipart by default and DispatcherServlet#checkMultipart parses any
 * multipart/form-data body before handler lookup, so every HTTP endpoint is a parse trigger.
 * The fix added Connector maxPartCount / maxPartHeaderSize; the shipped Connector must expose them.
 * Report-only: owner-driven Boot/Tomcat upgrade.
 */
@Tag("swarm")
class Supply2SwarmTest {

    static boolean atLeast(String version, int major, int minor, int patch) {
        String[] p = version.split("[.-]");
        int[] v = {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        if (v[0] != major) return v[0] > major;
        if (v[1] != minor) return v[1] > minor;
        return v[2] >= patch;
    }

    static boolean connectorHas(String method) {
        return Arrays.stream(Connector.class.getMethods()).anyMatch(m -> m.getName().equals(method));
    }

    @Test
    void shippedTomcatBoundsMultipartPartsForCve2025_48988() {
        String shipped = ServerInfo.getServerNumber();
        assertAll(
                () -> assertTrue(atLeast(shipped, 10, 1, 42),
                        "tomcat-embed-core " + shipped + " is affected by CVE-2025-48988; fixed in 10.1.42"),
                () -> assertTrue(connectorHas("setMaxPartCount"),
                        "Connector in " + shipped + " has no maxPartCount limit (CVE-2025-48988 mitigation absent)"),
                () -> assertTrue(connectorHas("setMaxPartHeaderSize"),
                        "Connector in " + shipped + " has no maxPartHeaderSize limit (CVE-2025-48988 mitigation absent)"));
    }
}
