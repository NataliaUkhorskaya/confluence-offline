package org.jetbrains.confluence;

import org.apache.xmlrpc.XmlRpcClient;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.confluence.model.ConfluencePage;
import org.jetbrains.confluence.model.ConfluenceSpace;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceConnector {

    private String authorizationToken = null;

    private static ConfluenceConnector instance = new ConfluenceConnector();

    private ConfluenceConnector() {
    }

    public static ConfluenceConnector getInstance() {
        return instance;
    }

    public void login() {
        Vector<Object> params = new Vector<Object>();
        params.add(ConfluenceConfig.CONFLUENCE_USERNAME);
        if (ConfluenceConfig.CONFLUENCE_PASSWORD.isEmpty()) {
            System.out.println("Enter you password on confluence:");
            while (true) {
                try {
                    Thread.sleep(100);
                    if (System.in.available() > 0) {
                        if (System.console() != null) {
                            char[] password = System.console().readPassword();
                            ConfluenceConfig.CONFLUENCE_PASSWORD = String.copyValueOf(password);
                        } else {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            ConfluenceConfig.CONFLUENCE_PASSWORD = reader.readLine();
                        }
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        params.add(ConfluenceConfig.CONFLUENCE_PASSWORD);
        Object result = remoteCall(ConfluenceRequests.LOGIN, params);
        if (result != null && result instanceof String) {
            authorizationToken = (String) result;
        }
        else {
            System.err.println(result);
            System.exit(1);
        }
    }

    @Nullable
    public ConfluenceSpace getSpace(String spaceName) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);

        @SuppressWarnings("unchecked")
        Vector<Hashtable> spaceSummaries = (Vector<Hashtable>) remoteCall(ConfluenceRequests.GET_SPACES, params);
        if (spaceName.isEmpty()) {
            System.err.println("Specify confluence space at confluence.properties file");
            return null;
        }

        ConfluenceSpace space = null;
        for (Hashtable spaceSummary : spaceSummaries) {
            String key = (String) spaceSummary.get("key");
            if (key.startsWith("~")) {
                continue;
            }
            String name = (String) spaceSummary.get("name");
            if (spaceName.equals(name)) {
                space = new ConfluenceSpace(key, name);
            }
        }

        if (space == null) {
            System.err.println("Cannot find space with name " + ConfluenceConfig.CONFLUENCE_SPACE);
            return null;
        }
        return space;
    }

    public List<String> getPageIds(String spaceId) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(spaceId);
        @SuppressWarnings("unchecked")
        Vector<Hashtable> pageSummaries = (Vector<Hashtable>) remoteCall(ConfluenceRequests.GET_PAGES, params);
        List<String> pageIds = new ArrayList<String>();
        for (Hashtable pageSummary : pageSummaries) {
            pageIds.add((String) pageSummary.get("id"));
        }
        return pageIds;
    }

    public Map<String, ConfluencePage> getPages(List<String> pageIds) {
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

    public String getPageContent(String pageId) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageId);
        Hashtable contentSummaries = (Hashtable) remoteCall(ConfluenceRequests.GET_PAGE, params);

        return (String) contentSummaries.get("content");
    }

    public ConfluencePage getPageById(String pageId) {
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

    @Nullable
    public String getPageContentByVersion(String pageId, String version) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageId);

        @SuppressWarnings("unchecked")
        Vector<Hashtable> historySummaries = (Vector<Hashtable>) remoteCall(ConfluenceRequests.GET_PAGE_HISTORY, params);

        for (Hashtable historyEntry : historySummaries) {
            if (historyEntry.get("version").equals(version)) {
                return getPageContent((String) historyEntry.get("id"));
            }
        }
        return null;
    }

    public boolean savePage(ConfluencePage page, boolean isNew) {
        return savePageWithContent(page, Utils.readFromFile(new File(page.getFilePath())), isNew);
    }

    public boolean savePageWithContent(ConfluencePage page, String content, boolean isNew) {
        Hashtable<String, String> pageData = new Hashtable<String, String>();
        if (!isNew) {
            pageData.put("id", page.getId());
            pageData.put("version", page.getVersion());
        }
        pageData.put("space", page.getSpace());
        pageData.put("title", page.getTitle());
        pageData.put("parentId", page.getParentId());

        if (!isNew) {
            pageData.put("content", content);
            boolean result = savePageRemoteCall(pageData);
            if (result) {
                IndexFileModel.getInstance().updatePageInfo(getPageById(page.getId()));
                System.out.println("Page " + page.getTitle() + " was updated.");
            }
            return result;
        }
        else {
            pageData.put("content", content);
            Vector<Object> params = new Vector<Object>();
            params.add(authorizationToken);
            params.add(pageData);
            Object o = remoteCall(ConfluenceRequests.STORE_PAGE, params);
            if (o != null) {
                System.out.println("Page " + page.getTitle() + " was created.");
                return true;
            }
            else {
                System.out.println("Error until uploading page " + page.getTitle() + ".");
                return false;
            }
        }
    }

    public boolean savePageRemoteCall(Hashtable<String, String> pageData) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        params.add(pageData);
        Hashtable<String, String> updateOptions = new Hashtable<String, String>();
        updateOptions.put("versionComment", "Modified in Offline Confluence");
        updateOptions.put("minorEdit", "false");
        params.add(updateOptions);
        Object o = remoteCall(ConfluenceRequests.UPDATE_PAGE, params);
        return o != null;
    }

    public void addSpace(ConfluenceSpace space) {
        Vector<Object> params = new Vector<Object>();
        params.add(authorizationToken);
        Hashtable<String, String> pageData = new Hashtable<String, String>();
        pageData.put("name", space.getName());
        pageData.put("key", space.getKey());
        pageData.put("description", "");
        params.add(pageData);
        remoteCall(ConfluenceRequests.ADD_SPACE, params);
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
