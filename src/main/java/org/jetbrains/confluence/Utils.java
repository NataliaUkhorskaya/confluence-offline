package org.jetbrains.confluence;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * @author Natalia.Ukhorskaya
 */

public class Utils {

    public static void writeToFile(File file, String text) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String readFromFile(File file) {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = reader.read()) >= 0) {
                builder.append((char) c);
            }
            reader.close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public static String substringExtension(String fileName) {
        return fileName.substring(0, fileName.length() - 11);
    }

    @Nullable
    public static Document getXmlDocument(File file) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document document;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        document.getDocumentElement().normalize();
        return document;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    public static File getFile(String filePath) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

}
