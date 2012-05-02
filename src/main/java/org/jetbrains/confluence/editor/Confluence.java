package org.jetbrains.confluence.editor;

import org.jetbrains.confluence.editor.model.ConfluencePage;
import org.jetbrains.confluence.editor.model.ConfluenceSpace;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Natalia.Ukhorskaya
 */

public class Confluence {

    private static Confluence instance = new Confluence();

    private Confluence() {
    }

    public Map<String, ConfluencePage> getPagesHashMap() {
        ConfluenceSpace space = ConfluenceConnector.getInstance().getSpace(ConfluenceConfig.CONFLUENCE_SPACE);
        if (space == null) {
            return null;
        }
        List<String> pageIds = ConfluenceConnector.getInstance().getPageIds(space.getKey());
        return ConfluenceConnector.getInstance().getPages(pageIds);
    }

    public static Confluence getInstance() {
        return instance;
    }

    public void login() {
        ConfluenceConnector.getInstance().login();
    }

    public boolean uploadContent() {
        boolean result = true;
        ConfluenceSpace space = ConfluenceConnector.getInstance().getSpace(ConfluenceConfig.CONFLUENCE_SPACE);
        if (space == null) {
            return false;
        }
        List<String> pageIds = ConfluenceConnector.getInstance().getPageIds(space.getKey());
        Map<String, ConfluencePage> pages = ConfluenceConnector.getInstance().getPages(pageIds);

        File rootDir = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + ConfluenceConfig.CONFLUENCE_SPACE);
        if (rootDir.exists()) {
            File[] files = rootDir.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                if (file.getName().endsWith("_local")) {
                    //Copies of files from server when you have local changes
                    continue;
                }
                ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(file.getAbsolutePath());
                if (pageFromFileSystem == null) {
                    pageFromFileSystem = new ConfluencePage();
                    pageFromFileSystem.setSpace(ConfluenceConfig.CONFLUENCE_SPACE);
                    pageFromFileSystem.setTitle(Utils.substringExtension(file.getName()).substring(file.getName().indexOf("_") + 1));
                    pageFromFileSystem.setParentId((file.getName()).substring(0, file.getName().indexOf("_")));
                    boolean tmpResult = ConfluenceConnector.getInstance().savePage(pageFromFileSystem, true);
                    if (!tmpResult) result = false;
                    continue;
                }
                ConfluencePage pageFromServer = pages.get(pageFromFileSystem.getId());

                if (pageFromServer == null) {
                    ConfluenceConnector.getInstance().savePage(pageFromFileSystem, true);
                    continue;
                }

                PageComparator pageComparator = new PageComparator(pageFromServer, pageFromFileSystem);

                if (!pageComparator.compareByTitle()) {
                    System.err.println("Page " + file.getAbsolutePath() + " was renamed or moved");
                    result = false;
                    continue;
                }

                if (pageComparator.compareByHashCode()) {
                    IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                    continue;
                }

                if (pageComparator.compareByVersion() != 0) {
                    System.err.println("Versions are different. Download new version of page before upload your changes.");
                    System.err.println("   Page form server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getId() + " " + pageFromServer.getVersion());
                    System.err.println("   Page form file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getId() + " " + pageFromFileSystem.getVersion());
                    result = false;
                }
                else {
                    boolean tmpResult = ConfluenceConnector.getInstance().savePage(pageFromServer, false);
                    if (!tmpResult) result = false;
                }
            }
        }
        return result;
    }

    public void loadAllContent() {
        loadAllContent(true);
    }

    public boolean updateContent(boolean isForce) {
        return loadAllContent(isForce);
    }

    public void updateFile(String filePath, boolean isRewrite) {
        ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(filePath);
        if (pageFromFileSystem == null) {
            System.err.println("Page is absent in index.xml");
            System.err.println("   Page from file system: " + filePath);
            return;
        }
        ConfluencePage pageFromServer = ConfluenceConnector.getInstance().getPageById(pageFromFileSystem.getId());
        if (pageFromServer == null) {
            System.err.println("Page is absent at server.");
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getId());
            return;
        }

        if (isRewrite) {
            String content = ConfluenceConnector.getInstance().getPageContent(pageFromServer.getId());
            pageFromServer.setHashCodeFromContent(content);
            Utils.writeToFile(Utils.getFile(filePath), content);
            IndexFileModel.getInstance().updatePageInfo(pageFromServer);
        }
        else {
            savePageToFileSystem(pageFromFileSystem, pageFromServer);
        }
    }

    private boolean loadAllContent(boolean isRewrite) {
        ConfluenceSpace space = ConfluenceConnector.getInstance().getSpace(ConfluenceConfig.CONFLUENCE_SPACE);
        if (space == null) {
            return false;
        }
        List<String> pageIds = ConfluenceConnector.getInstance().getPageIds(space.getKey());
        Map<String, ConfluencePage> pages = ConfluenceConnector.getInstance().getPages(pageIds);

        return savePagesToFileSystem(pages.values(), isRewrite);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean savePagesToFileSystem(Collection<ConfluencePage> pages, boolean isRewrite) {
        File rootDir = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + ConfluenceConfig.CONFLUENCE_SPACE);
        if (isRewrite && rootDir.exists()) {
            rootDir.delete();
            File[] files = rootDir.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                file.delete();
            }
        }
        rootDir.mkdir();

        boolean result = true;
        for (ConfluencePage pageFromServer : pages) {
            if (!isRewrite) {
                ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfoById(pageFromServer.getId());
                if (pageFromFileSystem == null) {
                    pageFromServer.setHashCodeFromContent(ConfluenceConnector.getInstance().getPageContent(pageFromServer.getId()));
                    savePageToFile(pageFromServer);
                    IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                    System.out.println("New page was loaded from server : " + pageFromServer.getFilePath());
                    continue;
                }
                String content = ConfluenceConnector.getInstance().getPageContent(pageFromServer.getId());
                pageFromServer.setHashCodeFromContent(content);
                boolean tmpResult = savePageToFileSystem(pageFromFileSystem, pageFromServer);
                if (!tmpResult) {
                    result = false;
                }
            }
            else {
                boolean tmpResult = savePageToFile(pageFromServer);
                if (!tmpResult) {
                    result = false;
                }

            }
        }
        return result;
    }

    private boolean savePageToFile(ConfluencePage pageFromServer) {
        File pageFile = Utils.getFile(pageFromServer.getFilePath());
        if (pageFile == null) {
            return false;
        }
        String content = ConfluenceConnector.getInstance().getPageContent(pageFromServer.getId());
        pageFromServer.setHashCodeFromContent(content);
        Utils.writeToFile(pageFile, content);
        IndexFileModel.getInstance().updatePageInfo(pageFromServer);
        System.out.println("File " + pageFile.getName() + " saved.");
        return true;
    }

    private boolean savePageToFileSystem(ConfluencePage pageFromFileSystem, ConfluencePage pageFromServer) {
        PageComparator pageComparator = new PageComparator(pageFromServer, pageFromFileSystem);

        if (!pageComparator.compareByPath()) {
            System.err.println("Page was renamed or parent of the page was changed.");
            System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getTitle() + " " + pageFromServer.getParentId());
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getParentId());
            return false;
        }

        if (pageComparator.compareByHashCode()) {
            IndexFileModel.getInstance().updatePageInfo(pageFromServer);
//            System.out.println("Page isn't modified : " + pageFromFileSystem.getFilePath());
            return true;
        }
        String serverContent = ConfluenceConnector.getInstance().getPageContent(pageFromServer.getId());
        if (pageComparator.compareByVersion() == -1) {
            System.err.println("Page in file system has older version that page on server.");
            System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getTitle() + " " + pageFromServer.getVersion());
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getVersion());
            return false;
        }
        else if (pageComparator.compareByVersion() == 1) {
            ConfluencePage oldPage = new ConfluencePage();
            oldPage.setHashCodeFromContent(ConfluenceConnector.getInstance().getPageContentByVersion(pageFromFileSystem.getId(), pageFromFileSystem.getVersion()));
            File pageFile = Utils.getFile(pageFromServer.getFilePath());

            if (pageFromFileSystem.getHashCode() == oldPage.getHashCode()) {
                IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                Utils.writeToFile(pageFile, serverContent);
                System.out.println("File " + pageFromFileSystem.getTitle() + " updated.");
                return true;
            }
            else {
                File mergeFile = Utils.getFile(pageFromFileSystem.getFilePath() + "_local");
                Utils.writeToFile(mergeFile, Utils.readFromFile(pageFile));
                Utils.writeToFile(pageFile, serverContent);
                IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                System.err.println("You have local changes in old version of page.");
                System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getHashCode() + " " + pageFromServer.getVersion());
                System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getHashCode() + " " + pageFromFileSystem.getVersion());
                System.err.println("   Your local file was saved into " + pageFromFileSystem.getFilePath() + "_local file.");
                return false;
            }
        }
        else {
            File pageFile = Utils.getFile(pageFromServer.getFilePath());
            File mergeFile = Utils.getFile(pageFromFileSystem.getFilePath() + "_local");
            Utils.writeToFile(mergeFile, Utils.readFromFile(pageFile));
            Utils.writeToFile(pageFile, serverContent);
            IndexFileModel.getInstance().updatePageInfo(pageFromServer);
            System.err.println("You have local change on page.");
            System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getHashCode() + " " + pageFromServer.getVersion());
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getHashCode() + " " + pageFromFileSystem.getVersion());
            System.err.println("   Version from server was saved into " + pageFromFileSystem.getFilePath() + "_local file.");
            return false;
        }
    }

}
