package com.ardencm.treasury.fxfunding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CiSettingsCredentialTest {

    private static final Pattern PASSWORD = Pattern.compile("<password>(.*?)</password>", Pattern.DOTALL);

    @Test
    void ciSettingsReadsCredentialsFromEnvironmentOnly() throws Exception {
        String settings = Files.readString(Path.of("ci/settings.xml"));
        Matcher m = PASSWORD.matcher(settings);
        boolean found = false;
        while (m.find()) {
            found = true;
            String value = m.group(1).trim();
            assertTrue(value.matches("\\$\\{env\\.[A-Z0-9_]+}"),
                    "ci/settings.xml <password> must be an ${env.*} reference, not a literal");
        }
        assertTrue(found, "expected the ardencm-artifactory server entry in ci/settings.xml");
        assertFalse(settings.contains("AKCp"), "ci/settings.xml must not contain a JFrog identity token");
    }
}
