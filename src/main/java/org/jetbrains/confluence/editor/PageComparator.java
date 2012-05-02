package org.jetbrains.confluence.editor;

import org.jetbrains.confluence.editor.model.ConfluencePage;

/**
 * @author Natalia.Ukhorskaya
 */

public class PageComparator {

    private final ConfluencePage pageFromServer;
    private final ConfluencePage pageFromFileSystem;

    public PageComparator(ConfluencePage pageFromServer, ConfluencePage pageFromFileSystem) {
        this.pageFromServer = pageFromServer;
        this.pageFromFileSystem = pageFromFileSystem;
    }


    public boolean compareById() {
        return pageFromFileSystem.getId().equals(pageFromServer.getId());
    }

    public boolean compareByPath() {
        return pageFromFileSystem.getFilePath().equals(pageFromServer.getFilePath());
    }

    public boolean compareByHashCode() {
        return pageFromFileSystem.getHashCode() == pageFromServer.getHashCode();
    }

    public boolean compareByTitle() {
        return pageFromFileSystem.getTitle().equals(pageFromServer.getTitle());
    }

    public int compareByVersion() {
        int versionFromServer = Integer.parseInt(pageFromServer.getVersion());
        int versionFromFileSystem = Integer.parseInt(pageFromFileSystem.getVersion());
        if (versionFromFileSystem == versionFromServer) {
            return 0;
        }
        else if (versionFromFileSystem > versionFromServer) {
            return -1;
        }
        else {
            return 1;
        }
    }

}
