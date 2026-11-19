package in.advocateportal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Reads settings from environment variables first, then from application.properties. */
public final class Config {

    private static final Properties P = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                P.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            /* use defaults */
        }
    }

    private Config() {
    }

    public static String get(String env, String prop, String def) {
        String v = System.getenv(env);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        v = P.getProperty(prop);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        return def;
    }
}
