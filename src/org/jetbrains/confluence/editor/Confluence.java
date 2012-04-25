package org.jetbrains.confluence.editor;

import org.apache.xmlrpc.XmlRpcClient;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.confluence.editor.model.ConfluencePage;
import org.jetbrains.confluence.editor.model.ConfluenceSpace;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * @author Natalia.Ukhorskaya
 */

public class Confluence {

    private String authorizationToken = null;

    private static Confluence instance = new Confluence();

    private Confluence() {
    }

    public Map<String, ConfluencePage> getPagesHashMap() {
        ConfluenceSpace space = getSpace();
        if (space == null) {
            return null;
        }
        List<String> pageIds = getPageIds(space.getKey());
        return getPages(pageIds);
    }

    public static Confluence getInstance() {
        return instance;
    }

    public void login() {
        Vector<Object> params = new Vector<Object>();
        params.add(ConfluenceConfig.CONFLUENCE_USERNAME);
        params.add(ConfluenceConfig.CONFLUENCE_PASSWORD);
        Object result = remoteCall(ConfluenceRequests.LOGIN, params);
        if (result != null) {
            authorizationToken = (String) result;
        }
    }

    public void uploadContent() {
        ConfluenceSpace space = getSpace();
        if (space == null) {
            return;
        }
        List<String> pageIds = getPageIds(space.getKey());
        Map<String, ConfluencePage> pages = getPages(pageIds);

        File rootDir = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + ConfluenceConfig.CONFLUENCE_SPACE);
        if (rootDir.exists()) {
            File[] files = rootDir.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file.getName().endsWith("_local")) {
                    //Copies of files from server when you have local changes
                    continue;
                }
                ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(file.getAbsolutePath());
                if (pageFromFileSystem == null) {
                    //todo
                    return;
                }
                ConfluencePage pageFromServer = pages.get(pageFromFileSystem.getId());

                if (pageFromServer == null) {
                    savePage(pageFromFileSystem, true);
                    continue;
                }

                PageComparator pageComparator = new PageComparator(pageFromServer, pageFromFileSystem);

                if (!pageComparator.compareByTitle()) {
                    System.err.println("Page " + file.getAbsolutePath() + " was renamed or moved");
                    continue;
                }

                if (pageComparator.compareByHashCode()) {
                    continue;
                }


                if (pageComparator.compareByVersion() != 0) {
                    System.err.println("Versions are different. Download new version of page before upload your changes.");
                    System.err.println("   Page form server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getId() + " " + pageFromServer.getVersion());
                    System.err.println("   Page form file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getId() + " " + pageFromFileSystem.getVersion());
                }
                else {
                    savePage(pageFromServer, false);
                }
            }
        }
    }

    public void loadAllContent() {
        loadAllContent(true);
    }

    public void updateContent(boolean isForce) {
        loadAllContent(isForce);
    }

    public void updateFile(String filePath, boolean isRewrite) {
        ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(filePath);
        if (pageFromFileSystem == null) {
            System.err.println("Page is absent in index.xml");
            System.err.println("   Page from file system: " + filePath);
            return;
        }
        ConfluencePage pageFromServer = getPageById(pageFromFileSystem.getId());
        if (pageFromServer == null) {
            System.err.println("Page is absent at server.");
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getId());
            return;
        }

        if (isRewrite) {
            String content = getPageContent(pageFromServer.getId());
            pageFromServer.setHashCodeFromContent(content);
            Utils.writeToFile(Utils.getFile(filePath), content);
            IndexFileModel.getInstance().updatePageInfo(pageFromServer);
        }
        else {
            savePageToFileSystem(pageFromFileSystem, pageFromServer);
        }

        //loadAllContent(false);
    }

    @Nullable
    private ConfluenceSpace getSpace() {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        Vector<Hashtable> spaceSummaries = (Vector) remoteCall(ConfluenceRequests.GET_SPACES, params);
        if (ConfluenceConfig.CONFLUENCE_SPACE.isEmpty()) {
            System.err.println("Specify confluence space at confluence.properties file");
            return null;
        }
        ConfluenceSpace space = findSpace(spaceSummaries);
        if (space == null) {
            System.err.println("Cannot find space with name " + ConfluenceConfig.CONFLUENCE_SPACE);
            return null;
        }
        return space;
    }

    private List<String> getPageIds(String spaceId) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(spaceId);
        Vector<Hashtable> pageSummaries = (Vector) remoteCall(ConfluenceRequests.GET_PAGES, params);
        List<String> pageIds = new ArrayList<String>();
        for (Hashtable pageSummary : pageSummaries) {
            pageIds.add((String) pageSummary.get("id"));
        }
        return pageIds;
    }

    private Map<String, ConfluencePage> getPages(List<String> pageIds) {
        Map<String, ConfluencePage> pages = new HashMap<String, ConfluencePage>();
        for (String pageId : pageIds) {
            Vector<Object> params = new Vector<Object>();
            params.add(authorizationToken);
            params.add(pageId);
            Hashtable contentSummaries = (Hashtable) remoteCall(ConfluenceRequests.GET_PAGE, params);

            ConfluencePage page = new ConfluencePage();
            page.setId((String) contentSummaries.get("id"));
            page.setTitle((String) contentSummaries.get("title"));
            page.setParentId((String) contentSummaries.get("parentId"));
            page.setSpace((String) contentSummaries.get("space"));
            page.setHashCodeFromContent((String) contentSummaries.get("content"));
            page.setVersion((String) contentSummaries.get("version"));
            pages.put(page.getId(), page);
        }
        return pages;
    }

    private String getPageContent(String pageId) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageId);
        Hashtable contentSummaries = (Hashtable) remoteCall(ConfluenceRequests.GET_PAGE, params);

        return (String) contentSummaries.get("content");
    }

    private ConfluencePage getPageById(String pageId) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageId);
        Hashtable contentSummaries = (Hashtable) remoteCall(ConfluenceRequests.GET_PAGE, params);

        ConfluencePage page = new ConfluencePage();
        page.setId((String) contentSummaries.get("id"));
        page.setTitle((String) contentSummaries.get("title"));
        page.setParentId((String) contentSummaries.get("parentId"));
        page.setSpace((String) contentSummaries.get("space"));
        page.setHashCodeFromContent((String) contentSummaries.get("content"));
        page.setVersion((String) contentSummaries.get("version"));
        return page;
    }

    private void loadAllContent(boolean isRewrite) {
        ConfluenceSpace space = getSpace();
        if (space == null) {
            return;
        }
        List<String> pageIds = getPageIds(space.getKey());
        Map<String, ConfluencePage> pages = getPages(pageIds);

        savePagesToFileSystem(pages.values(), isRewrite);
    }


    private void savePage(ConfluencePage page, boolean isNew) {
        Hashtable<String, String> pageData = new Hashtable<String, String>();
        if (!isNew) {
            pageData.put("id", page.getId());
            pageData.put("version", page.getVersion());
        }
        pageData.put("space", page.getSpace());
        pageData.put("title", page.getTitle());
        pageData.put("parentId", page.getParentId());

        File file = Utils.getFile(page.getFilePath());
        String localContent = Utils.readFromFile(file);
        if (!isNew) {
            pageData.put("content", localContent);
            Vector params = new Vector();
            params.add(authorizationToken);
            params.add(pageData);
            Hashtable<String, String> updateOptions = new Hashtable<String, String>();
            updateOptions.put("versionComment", "Modified in Offline Confluence Editor");
            updateOptions.put("minorEdit", "false");
            params.add(updateOptions);
            remoteCall(ConfluenceRequests.UPDATE_PAGE, params);
            System.out.println("Page " + page.getTitle() + " was updated.");
        }
        else if (isNew) {
            pageData.put("content", localContent);
            Vector params = new Vector();
            params.add(authorizationToken);
            params.add(pageData);
            Object o = remoteCall(ConfluenceRequests.STORE_PAGE, params);
            if (o != null) {
                System.out.println("Page " + page.getTitle() + " was created.");
            }
        }

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void savePagesToFileSystem(Collection<ConfluencePage> pages, boolean isRewrite) {
        File rootDir = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + ConfluenceConfig.CONFLUENCE_SPACE);
        if (isRewrite && rootDir.exists()) {
            rootDir.delete();
            File[] files = rootDir.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                file.delete();
            }
        }
        rootDir.mkdir();

        for (ConfluencePage pageFromServer : pages) {
            if (!isRewrite) {
                ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfoById(pageFromServer.getId());
                if (pageFromFileSystem == null) {
                    savePageToFile(pageFromServer);
                    IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                    System.out.println("New page was loaded from server : " + pageFromServer.getFilePath());
                    continue;
                }
                String content = getPageContent(pageFromServer.getId());
                pageFromServer.setHashCodeFromContent(content);
                savePageToFileSystem(pageFromFileSystem, pageFromServer);
            }
            else {
                savePageToFile(pageFromServer);
            }
        }

    }

    private void savePageToFile(ConfluencePage pageFromServer) {
        File pageFile = Utils.getFile(pageFromServer.getFilePath());
        if (pageFile == null) {
            return;
        }
        String content = getPageContent(pageFromServer.getId());
        pageFromServer.setHashCodeFromContent(content);
        Utils.writeToFile(pageFile, content);
        IndexFileModel.getInstance().updatePageInfo(pageFromServer);
        System.out.println("File " + pageFile.getName() + " saved.");
    }

    private void savePageToFileSystem(ConfluencePage pageFromFileSystem, ConfluencePage pageFromServer) {
        PageComparator pageComparator = new PageComparator(pageFromServer, pageFromFileSystem);

        if (!pageComparator.compareByPath()) {
            System.err.println("Page was renamed or parent of the page was changed.");
            System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getTitle() + " " + pageFromServer.getParentId());
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getParentId());
            return;
        }

        if (pageComparator.compareByHashCode()) {
            IndexFileModel.getInstance().updatePageInfo(pageFromServer);
//            System.out.println("Page isn't modified : " + pageFromFileSystem.getFilePath());
            return;
        }
        String serverContent = getPageContent(pageFromServer.getId());
        if (pageComparator.compareByVersion() == -1) {
            System.err.println("Page in file system has older version that page on server.");
            System.err.println("   Page from server:      " + pageFromServer.getFilePath() + " " + pageFromServer.getTitle() + " " + pageFromServer.getVersion());
            System.err.println("   Page from file system: " + pageFromFileSystem.getFilePath() + " " + pageFromFileSystem.getTitle() + " " + pageFromFileSystem.getVersion());
        }
        else if (pageComparator.compareByVersion() == 1) {
            ConfluencePage oldPage = new ConfluencePage();
            oldPage.setHashCodeFromContent(getPageContentByVersion(pageFromFileSystem.getId(), pageFromFileSystem.getVersion()));
            File pageFile = Utils.getFile(pageFromServer.getFilePath());

            if (pageFromFileSystem.getHashCode() == oldPage.getHashCode()) {
                IndexFileModel.getInstance().updatePageInfo(pageFromServer);
                Utils.writeToFile(pageFile, serverContent);
                System.out.println("File " + pageFromFileSystem.getTitle() + " updated.");
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
        }
    }

    @Nullable
    private String getPageContentByVersion(String pageId, String version) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageId);
        Vector<Hashtable> historySummaries = (Vector) remoteCall(ConfluenceRequests.GET_PAGE_HISTORY, params);

        for (Hashtable historyEntry : historySummaries) {
            if (historyEntry.get("version").equals(version)) {
                return getPageContent((String) historyEntry.get("id"));
            }
        }
        return null;
    }


    @Nullable
    private ConfluenceSpace findSpace(Vector<Hashtable> spaceSummaries) {
        for (Hashtable spaceSummary : spaceSummaries) {
            String key = (String) spaceSummary.get("key");
            if (key.startsWith("~")) {
                continue;
            }
            String name = (String) spaceSummary.get("name");
            if (ConfluenceConfig.CONFLUENCE_SPACE.equals(name)) {
                return new ConfluenceSpace(key, name);
            }
        }
        return null;
    }

    private Object remoteCall(final String method, final Vector<Object> args) {
        XmlRpcClient client;
        try {
            client = new XmlRpcClient(ConfluenceConfig.CONFLUENCE_SERVER + ConfluenceRequests.PREFIX);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        try {
            return client.execute(method, args);

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }

    }
}
