package org.jetbrains.confluence;

import org.jetbrains.confluence.arguments.Arguments;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
    public static String ROOT_DIRECTORY = "confluence";

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
                    ROOT_DIRECTORY = value;
                }
            }
            File confluenceRootDirectory = new File(ROOT_DIRECTORY);
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

    public static boolean createPropertiesFile(Arguments arguments) {
        File file = new File("confluence.properties");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Properties properties = new Properties();
            properties.load(new FileReader(file));

            CONFLUENCE_SERVER = arguments.server;
            CONFLUENCE_USERNAME = arguments.userName;
            CONFLUENCE_SPACE = arguments.space;
            ROOT_DIRECTORY = arguments.rootDirectory;

            properties.setProperty("confluence.server", CONFLUENCE_SERVER);
            properties.setProperty("confluence.username", CONFLUENCE_USERNAME);
            properties.setProperty("confluence.space", CONFLUENCE_SPACE);
            properties.setProperty("confluence.directory", ROOT_DIRECTORY.trim());

            properties.store(new FileWriter(file), "Automatically created file by confluence-offline");

            File confluenceRootDirectory = new File(ROOT_DIRECTORY);
            if (!confluenceRootDirectory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                confluenceRootDirectory.mkdirs();
            }
            System.out.println("Commandline arguments are saved in confluence.properties file. You can change it later.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
