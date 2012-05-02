package org.jetbrains.confluence.editor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceConfig {
    public static String CONFLUENCE_SERVER = "";
    public static String CONFLUENCE_USERNAME = "";
    public static String CONFLUENCE_PASSWORD = "";
    public static String CONFLUENCE_SPACE = "";
    public static String CONFLUENCE_ROOT_DIRECTORY = "confluence";

    public static boolean parsePropertiesFile() {
        File file = new File("confluence.properties");
        if (!file.exists()) {
            System.err.println(file.getAbsolutePath() + " doesn't exists.");
            return false;
        }

        try {
            Properties properties = new Properties();
            properties.load(new FileReader(file));
            Set<String> names = properties.stringPropertyNames();
            for (String name : names) {
                String value = properties.get(name).toString();
                if (name.equals("confluence.server")) {
                    CONFLUENCE_SERVER = value;
                }
                else if (name.equals("confluence.username")) {
                    CONFLUENCE_USERNAME = value;
                }
                else if (name.equals("confluence.password")) {
                    CONFLUENCE_PASSWORD = value;
                }
                else if (name.equals("confluence.space")) {
                    CONFLUENCE_SPACE = value;
                }
                else if (name.equals("confluence.root.directory")) {
                    CONFLUENCE_ROOT_DIRECTORY = value;
                }
            }
            File confluenceRootDirectory = new File(CONFLUENCE_ROOT_DIRECTORY);
            if (!confluenceRootDirectory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                confluenceRootDirectory.mkdirs();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
