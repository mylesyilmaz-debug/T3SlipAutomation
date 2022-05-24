package ca.empire.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestEnvironment {
    private static final String KEY_PATTERN = "[\\w\\d-]+";
    private static final Logger logger = LogManager.getLogger(TestEnvironment.class);

    private HashMap<String, Object> env;

    public TestEnvironment(boolean includeSystemEnv) {
        logger.traceEntry(() -> includeSystemEnv);
        env = new HashMap<>();

        if (includeSystemEnv) {
            env.putAll(System.getenv());
        }
        logger.traceExit(this);
    }

    public TestEnvironment(@NotNull File envFile, boolean includeSystemEnv) throws IOException {
        this(includeSystemEnv);
        logger.traceEntry(() -> envFile, () -> includeSystemEnv);

        if (!envFile.exists()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(envFile.getPath() + " does not exist.");
            logger.error(e);
            throw e;
        } else if (!envFile.isFile()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(envFile.getAbsolutePath() + "is not a file.");
            logger.error(e);
            throw e;
        }

        if (!loadEnvFile(envFile)) {
            IOException e = new IOException("Unable to load env file " + envFile.getAbsolutePath());
            logger.error(e);
            throw e;
        }
        logger.traceExit(this);
    }

    /**
     * Loads in the environment variables from the provided file.
     *
     * @param envFile [File] The File that represents the .env file.
     */
    private boolean loadEnvFile(@NotNull File envFile) {
        logger.traceEntry(() -> envFile);

        try {
            FileReader fr = new FileReader(envFile);
            BufferedReader br = new BufferedReader(fr);
            Pattern keyPattern = Pattern.compile("(" + KEY_PATTERN + ")=(.*)");
            int line = 0;

            while (true) {
                String input = br.readLine();
                line++;

                if (input == null) {
                    break;
                }

                input = input.trim();
                Matcher matcher = keyPattern.matcher(input);

                if (input.isEmpty() || input.charAt(0) == '#') {
                    continue;
                }
                if (!matcher.matches()) {
                    throw new IOException(
                            "The .env entry at line " + line + " is malformed: " + input);
                }

                env.put(matcher.group(1), matcher.group(2));
            }
        } catch (IOException e) {
            logger.error(e);
            logger.traceExit(false);
            return false;
        }

        logger.traceExit(true);
        return true;
    }

    /**
     * Parses the provided string and replaces all valid env vars with their respective values. Any
     * invalid or unassigned variables will remain unchanged in the returned string. Valid env vars
     * are dictated by the regex "\{\{([\w\d-]+)}}". That is, any alphanumeric string surrounded by
     * double curly braces.<br>
     * Some examples are:<br>
     * - {{envVar}}<br>
     * - {{example-environment_var1}}<br>
     * - {{ENv1ronm3nt-Var_32}}<br>
     * - {{ENVIRONMENT_VAR}}
     *
     * @param string The string containing env vars that need to be parsed.
     * @return The string with all possible env vars parsed.
     */
    public String parseString(@NotNull String string) {
        String entryString = string;
        logger.traceEntry(() -> entryString);

        Matcher matcher = Pattern.compile("\\{\\{(" + KEY_PATTERN + ")}}").matcher(string);

        while (matcher.find()) {
            String key = matcher.group(1);

            if (env.containsKey(key)) {
                string =
                        string.substring(0, matcher.start())
                                + env.get(key).toString()
                                + string.substring(matcher.end());

                matcher.reset(string);
            }
        }

        logger.traceExit("***");
        return string;
    }

    /**
     * Retrieves the value mapped by &lt;key&gt; from the environment. This method passes on the
     * responsibility of type casting to the end user. They should only access environment variables
     * which they know the type of.
     *
     * @param key [String] The environment variable name.
     * @return [Object] The Object that is mapped to &lt;key&gt;.
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@Nullable String key) {
        logger.traceEntry(() -> key);
        checkKey(key);
        logger.traceExit("***");
        return (T) env.get(key);
    }

    /**
     * Wrapper for the HashMap put method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public Object put(@Nullable String key, @Nullable Object value) {
        logger.traceEntry(() -> key, () -> "***");
        checkKey(key);
        logger.traceExit("***");
        return env.put(key, value);
    }

    /**
     * Wrapper for the HashMap containsKey method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public boolean containsKey(@Nullable String key) {
        logger.traceEntry(() -> key);
        checkKey(key);
        boolean result = env.containsKey(key);
        logger.traceExit(result);
        return result;
    }

    /**
     * Wrapper for the HashMap remove method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public Object remove(@Nullable String key) {
        logger.traceEntry(() -> key);
        checkKey(key);
        logger.traceExit("***");
        return env.remove(key);
    }

    /**
     * Wrapper for the HashMap remove method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public boolean remove(@Nullable String key, @Nullable Object value) {
        logger.traceEntry(() -> key, () -> "***");
        checkKey(key);
        boolean result = env.remove(key, value);
        logger.traceExit(result);
        return result;
    }

    /** Wrapper for the HashMap keySet method. */
    public Set<String> keySet() {
        logger.traceEntry();
        logger.traceExit(env.keySet());
        return env.keySet();
    }

    /** Wrapper for the HashMap values method. */
    public Collection<Object> values() {
        logger.traceEntry();
        logger.traceExit("***");
        return env.values();
    }

    /**
     * Checks to ensure that the provided key is valid.
     *
     * @param key [String] The key that will be checked.
     * @throws IllegalArgumentException When the provided key is not valid.
     */
    private static void checkKey(String key) {
        logger.traceEntry(() -> key);
        if (!Pattern.compile(KEY_PATTERN).matcher(key).matches()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(
                            "The provided key <"
                                    + key
                                    + "> does not match the expected pattern: "
                                    + KEY_PATTERN);
            logger.error(e);
            throw e;
        }
        logger.traceExit();
    }
}
