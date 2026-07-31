package cn.infstar.essentialsC;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigurationResourcesTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "config.yml",
        "modules.yml",
        "maintenance.yml",
        "blocks-menu.yml",
        "paper-plugin.yml",
        "lang/zh_CN.yml",
        "lang/en_US.yml"
    })
    void bundledYamlIsValid(String resourcePath) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(input, resourcePath);
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(new InputStreamReader(input, StandardCharsets.UTF_8));

            if (resourcePath.equals("config.yml")) {
                assertEquals(2, configuration.getInt("config-version"));
                assertEquals(5, configuration.getInt("tpa.max-pending-requests"));
            }
            if (resourcePath.equals("paper-plugin.yml")) {
                assertEquals("1.21.11", configuration.getString("api-version"));
            }
        }
    }
}
