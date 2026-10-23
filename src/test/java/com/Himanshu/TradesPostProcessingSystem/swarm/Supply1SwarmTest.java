package com.Himanshu.TradesPostProcessingSystem.swarm;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUPPLY-1: the embedded Tomcat shipped by the Spring Boot 3.4.2 BOM must not be in the
 * CVE-2025-24813 range (CISA KEV, partial-PUT path equivalence, 10.1.0-M1 through 10.1.34).
 * Report-only: the fix is an owner-driven Boot/Tomcat upgrade, not a code change here.
 */
@Tag("swarm")
class Supply1SwarmTest {

    static int[] parse(String v) {
        String[] p = v.split("[.-]");
        return new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    static boolean atLeast(String version, int major, int minor, int patch) {
        int[] v = parse(version);
        if (v[0] != major) return v[0] > major;
        if (v[1] != minor) return v[1] > minor;
        return v[2] >= patch;
    }

    @Test
    void shippedTomcatIsPatchedForKevCve2025_24813() {
        String shipped = ServerInfo.getServerNumber();
        assertTrue(atLeast(shipped, 10, 1, 35),
                "tomcat-embed-core " + shipped + " on the runtime classpath is affected by CVE-2025-24813 "
                        + "(CISA KEV, added 2025-04-01); fixed in 10.1.35");
    }
}
