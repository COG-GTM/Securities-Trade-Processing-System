package com.ardencm.treasury.fxfunding;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CiSettingsCredentialTest {
    private static final Path SETTINGS = Path.of("ci/settings.xml");

    @Test
    void passwordsAreEnvReferencesNotLiterals() throws Exception {
        String xml = Files.readString(SETTINGS);
        Matcher m = Pattern.compile("<password>(.*?)</password>", Pattern.DOTALL).matcher(xml);
        while (m.find()) {
            String value = m.group(1).trim();
            assertTrue(value.matches("\\$\\{env\\.[A-Z0-9_]+}"),
                    "ci/settings.xml <password> must be an ${env.*} reference, not a literal");
        }
    }

    @Test
    void noArtifactoryTokenLiteral() throws Exception {
        assertFalse(Files.readString(SETTINGS).contains("AKCp"),
                "ci/settings.xml must not contain an Artifactory identity token");
    }
}
