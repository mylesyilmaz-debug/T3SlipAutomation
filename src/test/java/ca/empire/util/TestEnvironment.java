package ca.empire.util;

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

    private HashMap<String, Object> env;

    public TestEnvironment(boolean includeSystemEnv) {
        env = new HashMap<>();

        if (includeSystemEnv) {
            env.putAll(System.getenv());
        }
    }

    public TestEnvironment(@NotNull File envFile, boolean includeSystemEnv) throws IOException {
        this(includeSystemEnv);

        if (!envFile.exists()) {
            throw new IllegalArgumentException(envFile.getPath() + " does not exist.");
        } else if (!envFile.isFile()) {
            throw new IllegalArgumentException(envFile.getAbsolutePath() + "is not a file.");
        }

        if (!loadEnvFile(envFile)) {
            throw new IOException("Unable to load env file " + envFile.getAbsolutePath());
        }
    }

    /**
     * Loads in the environment variables from the provided file.
     *
     * @param envFile [File] The File that represents the .env file.
     */
    private boolean loadEnvFile(@NotNull File envFile) {
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
            e.printStackTrace();
            return false;
        }

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
        checkKey(key);
        return (T) env.get(key);
    }

    /**
     * Wrapper for the HashMap put method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public Object put(@Nullable String key, @Nullable Object value) {
        checkKey(key);
        return env.put(key, value);
    }

    /**
     * Wrapper for the HashMap containsKey method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public boolean containsKey(@Nullable String key) {
        checkKey(key);
        return env.containsKey(key);
    }

    /**
     * Wrapper for the HashMap remove method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public Object remove(@Nullable String key) {
        checkKey(key);
        return env.remove(key);
    }

    /**
     * Wrapper for the HashMap remove method.
     *
     * @throws IllegalArgumentException When the provided key is not valid (i.e. it is not
     *     alphanumeric).
     */
    public boolean remove(@Nullable String key, @Nullable Object value) {
        checkKey(key);
        return env.remove(key, value);
    }

    /** Wrapper for the HashMap keySet method. */
    public Set<String> keySet() {
        return env.keySet();
    }

    /** Wrapper for the HashMap values method. */
    public Collection<Object> values() {
        return env.values();
    }

    /**
     * Checks to ensure that the provided key is valid.
     *
     * @param key [String] The key that will be checked.
     * @throws IllegalArgumentException When the provided key is not valid.
     */
    private static void checkKey(String key) {
        if (!Pattern.compile(KEY_PATTERN).matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "The provided key <"
                            + key
                            + "> does not match the expected pattern: "
                            + KEY_PATTERN);
        }
    }
}
