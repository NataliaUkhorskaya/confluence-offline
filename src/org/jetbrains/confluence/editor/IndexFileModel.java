package org.jetbrains.confluence.editor;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.confluence.editor.model.ConfluencePage;
import org.w3c.dom.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Natalia.Ukhorskaya
 */

public class IndexFileModel {
    private File index = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + "index.xml");

    private static IndexFileModel instance = new IndexFileModel();

    private IndexFileModel() {
    }

    public static IndexFileModel getInstance() {
        return instance;
    }

    @Nullable
    public ConfluencePage getPageInfo(String pageFilePath) {
        Document document = Utils.getXmlDocument(index);
        if (document == null) {
            return null;
        }
        NodeList nodeList = document.getElementsByTagName("page");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            NamedNodeMap attributes = nodeList.item(i).getAttributes();
            if (attributes.getNamedItem("path").getTextContent().equals(pageFilePath)) {
                NodeList childNodes = nodeList.item(i).getChildNodes();
                ConfluencePage page = new ConfluencePage();
                page.setId(attributes.getNamedItem("id").getTextContent());
                for (int j = 0; j < childNodes.getLength(); j++) {
                    if (childNodes.item(j).getNodeName().equals("parent")) {
                        page.setParentId(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("version")) {
                        page.setVersion(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("title")) {
                        page.setTitle(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("hash")) {
                        page.setHashCodeFromString(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("space")) {
                        page.setSpace(childNodes.item(j).getTextContent());
                    }

                }
                return page;
            }
        }
        return null;
    }

    @Nullable
    public ConfluencePage getPageInfoById(String pageId) {
        Document document = Utils.getXmlDocument(index);
        if (document == null) {
            return null;
        }
        NodeList nodeList = document.getElementsByTagName("page");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            NamedNodeMap attributes = nodeList.item(i).getAttributes();
            if (attributes.getNamedItem("id").getTextContent().equals(pageId)) {
                NodeList childNodes = nodeList.item(i).getChildNodes();
                ConfluencePage page = new ConfluencePage();
                page.setId(attributes.getNamedItem("id").getTextContent());
                for (int j = 0; j < childNodes.getLength(); j++) {
                    if (childNodes.item(j).getNodeName().equals("parent")) {
                        page.setParentId(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("version")) {
                        page.setVersion(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("title")) {
                        page.setTitle(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("hash")) {
                        page.setHashCodeFromString(childNodes.item(j).getTextContent());
                    }
                    else if (childNodes.item(j).getNodeName().equals("space")) {
                        page.setSpace(childNodes.item(j).getTextContent());
                    }
                }
                return page;
            }
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void createIndexFile() {
        try {
            index.createNewFile();
            Map<String, ConfluencePage> map = Confluence.getInstance().getPagesHashMap();
            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\"?>");
            builder.append("<space>");
            for (ConfluencePage page : map.values()) {
                builder.append("<page path=\"");
                builder.append(page.getFilePath());
                builder.append("\" id=\"");
                builder.append(page.getId());
                builder.append("\">");
                builder.append("<title>");
                builder.append(page.getTitle());
                builder.append("</title>");
                builder.append("<parent>");
                builder.append(page.getParentId());
                builder.append("</parent>");
                builder.append("<version>");
                builder.append(page.getVersion());
                builder.append("</version>");
                builder.append("<hash>");
                builder.append(page.getHashCode());
                builder.append("</hash>");
                builder.append("<space>");
                builder.append(page.getSpace());
                builder.append("</space>");
                builder.append("</page>");
            }
            builder.append("</space>");

            Utils.writeToFile(index, builder.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void updatePageInfo(ConfluencePage page) {
        Document document = Utils.getXmlDocument(index);
        if (document == null) {
            return;
        }

        NodeList nodeList = document.getElementsByTagName("page");
        boolean isFound = false;
        for (int i = 0; i < nodeList.getLength(); ++i) {
            NamedNodeMap attributes = nodeList.item(i).getAttributes();
            if (attributes.getNamedItem("path").getTextContent().equals(page.getFilePath())) {
                isFound = true;
                NodeList childNodes = nodeList.item(i).getChildNodes();
                attributes.getNamedItem("id").setTextContent(page.getId());
                for (int j = 0; j < childNodes.getLength(); j++) {
                    if (childNodes.item(j).getNodeName().equals("parent")) {
                        childNodes.item(j).setTextContent(page.getParentId());
                    }
                    else if (childNodes.item(j).getNodeName().equals("version")) {
                        childNodes.item(j).setTextContent(page.getVersion());
                    }
                    else if (childNodes.item(j).getNodeName().equals("title")) {
                        childNodes.item(j).setTextContent(page.getTitle());
                    }
                    else if (childNodes.item(j).getNodeName().equals("hash")) {
                        childNodes.item(j).setTextContent(String.valueOf(page.getHashCode()));
                    }
                    else if (childNodes.item(j).getNodeName().equals("space")) {
                        childNodes.item(j).setTextContent(page.getSpace());
                    }
                }
            }
        }
        if (!isFound) {
            Node node = document.getElementsByTagName("page").item(0).cloneNode(true);
            node.getAttributes().getNamedItem("path").setTextContent(page.getFilePath());
            node.getAttributes().getNamedItem("id").setTextContent(page.getId());
            NodeList childNodes = node.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (childNodes.item(j).getNodeName().equals("parent")) {
                    childNodes.item(j).setTextContent(page.getParentId());
                }
                else if (childNodes.item(j).getNodeName().equals("version")) {
                    childNodes.item(j).setTextContent(page.getVersion());
                }
                else if (childNodes.item(j).getNodeName().equals("title")) {
                    childNodes.item(j).setTextContent(page.getTitle());
                }
                else if (childNodes.item(j).getNodeName().equals("hash")) {
                    childNodes.item(j).setTextContent(String.valueOf(page.getHashCode()));
                }
                else if (childNodes.item(j).getNodeName().equals("space")) {
                    childNodes.item(j).setTextContent(page.getSpace());
                }
            }
            document.getChildNodes().item(0).appendChild(node);
        }
        try {
            TransformerFactory tFactory =
                    TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(index);
            transformer.transform(source, result);
        } catch (Throwable e) {
            System.err.println("Error during updating index.xml.");
            e.printStackTrace();
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void updateContentHashFromFileSystem() {
        Document document = Utils.getXmlDocument(index);
        if (document == null) {
            return;
        }

        File rootDir = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + ConfluenceConfig.CONFLUENCE_SPACE);
        if (!rootDir.exists()) {
            System.err.println("Root directory doesn't exists.");
        }

        File[] files = rootDir.listFiles();
        if (files == null) {
            System.err.println("Root directory is empty.");
        }

        NodeList nodeList = document.getElementsByTagName("page");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            NamedNodeMap attributes = nodeList.item(i).getAttributes();
            for (File file : files) {
                if (attributes.getNamedItem("path").getTextContent().equals(file.getAbsolutePath())) {
                    ConfluencePage page = new ConfluencePage();
                    page.setHashCodeFromContent(Utils.readFromFile(file));
                    NodeList childNodes = nodeList.item(i).getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j).getNodeName().equals("hash")) {
                            childNodes.item(j).setTextContent(String.valueOf(page.getHashCode()));
                            break;
                        }
                    }
                }
            }
        }
        try {
            TransformerFactory tFactory =
                    TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(index);
            transformer.transform(source, result);
        } catch (Throwable e) {
            System.err.println("Error during updating index.xml.");
            e.printStackTrace();
        }
    }
}
