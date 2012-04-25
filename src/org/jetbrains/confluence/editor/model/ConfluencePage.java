package org.jetbrains.confluence.editor.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.confluence.editor.ConfluenceConfig;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluencePage {
    private String id = "";
    private String parentId = "0";
    private String space = null;
    private String title = "";
    private String filePath = null;
    private String version = "";
    private int hash;

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NotNull
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @NotNull
    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @NotNull
    public String getFilePath() {
        if (filePath == null) {

            String fileNameEncoded = parentId + "_" + title;
            String spaceNameEncoded = ConfluenceConfig.CONFLUENCE_SPACE;
            try {
                fileNameEncoded = URLEncoder.encode(fileNameEncoded, "UTF-8").replaceAll("\\W+", "");
                spaceNameEncoded = URLEncoder.encode(spaceNameEncoded, "UTF-8").replaceAll("\\W+", "");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }
            filePath = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar +
                    spaceNameEncoded + File.separatorChar + fileNameEncoded + ".confluence").getAbsolutePath();
        }
        return filePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (version != null) {
            this.version = version;
        }
    }

    public int getHashCode() {
        return hash;
    }

    public void setHashCodeFromContent(String content) {
        hash = calcHashForString(content);
    }

    public void setHashCodeFromString(String hash) {
        this.hash = Integer.parseInt(hash);
    }


    private int calcHashForString(String str) {
        int h = 0;
        for (int i = 0; i < str.length(); i++) {
            h = 31 * h + str.charAt(i);
        }
        return h;
    }
}
