package it;

import junit.framework.TestCase;
import org.apache.commons.lang.math.RandomUtils;
import org.jetbrains.confluence.editor.*;
import org.jetbrains.confluence.editor.model.ConfluencePage;
import org.jetbrains.confluence.editor.model.ConfluenceSpace;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceIntegrationTest extends TestCase {

    private static boolean isFirstTest = true;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (isFirstTest) {
            ConfluenceConfig.parsePropertiesFile();
            Confluence.getInstance().login();
            addSpace();
            isFirstTest = false;
        }
        else {
            Confluence.getInstance().login();
        }
    }

    public void addSpace() {
        ConfluenceConnector.getInstance().addSpace(new ConfluenceSpace("test", "TestSpace"));
        ConfluenceConfig.CONFLUENCE_SPACE = "TestSpace";
        ConfluenceSpace space = ConfluenceConnector.getInstance().getSpace("TestSpace");
        assert space != null;
        assertEquals(space.getKey(), "test");
        assertEquals(space.getName(), "TestSpace");
    }

    public void testDownloadPages() {
        ConfluenceSpace space = ConfluenceConnector.getInstance().getSpace("TestSpace");
        assert space != null;
        Map<String, ConfluencePage> pages = ConfluenceConnector.getInstance().getPages(
                ConfluenceConnector.getInstance().getPageIds(space.getKey()));

        IndexFileModel.getInstance().createIndexFile();
        Confluence.getInstance().loadAllContent();

        for (ConfluencePage pageFromServer : pages.values()) {
            File file = new File(pageFromServer.getFilePath());
            assert file.exists();
            assertEquals(Utils.readFromFile(file), getContentFromServer(pageFromServer));
            ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(file.getAbsolutePath());
            assert pageFromFileSystem != null;
            comparePages(pageFromServer, pageFromFileSystem);
            comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
        }
    }

    public void testUpdatePageWithSameVersionAndDifferentContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();

        makeLocalChanges(pageFromFileSystem, this.getName() + RandomUtils.nextInt());

        String contentFromFileSystemBefore = getContentFromFileSystem(pageFromFileSystem);

        assert !getContentFromServer(pageFromFileSystem).equals(
                getContentFromFileSystem(pageFromFileSystem));

        assertFalse(Confluence.getInstance().updateContent(false));

        pageFromFileSystem = getPageFromFileSystem();
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        comparePages(pageFromServer, pageFromFileSystem);
        //Update file from server
        comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
        //Save old version of file to fileName + _local
        assertEquals("Content of page from file system was changed after update", Utils.readFromFile(new File(pageFromFileSystem.getFilePath() + "_local")), contentFromFileSystemBefore);
    }

    public void testForceUpdatePageWithSameVersionAndDifferentContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();

        makeLocalChanges(pageFromFileSystem, this.getName() + RandomUtils.nextInt());

        assert !getContentFromServer(pageFromFileSystem).equals(
                getContentFromFileSystem(pageFromFileSystem));

        assertTrue(Confluence.getInstance().updateContent(true));

        pageFromFileSystem = getPageFromFileSystem();
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        comparePages(pageFromServer, pageFromFileSystem);
        comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
    }

    public void testUploadPageWithSameVersionAndDifferentContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();

        makeLocalChanges(pageFromFileSystem, this.getName() + RandomUtils.nextInt());

        assert !getContentFromServer(pageFromFileSystem).equals(
                getContentFromFileSystem(pageFromFileSystem));

        assertTrue(Confluence.getInstance().uploadContent());

        pageFromFileSystem = getPageFromFileSystem();
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        comparePages(pageFromServer, pageFromFileSystem);
        comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
    }

    public void testUpdatePageWithDifferentVersionAndSameContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();

        String changes = this.getName() + RandomUtils.nextInt();
        makeChangesOnServer(pageFromFileSystem, changes);
        makeLocalChanges(pageFromFileSystem, changes);

        String pageContentFromServer = getContentFromServer(pageFromFileSystem);
        String pageContentFromFileSystem = getContentFromFileSystem(pageFromFileSystem);
        assert pageContentFromServer.equals(pageContentFromFileSystem);

        assertTrue(Confluence.getInstance().updateContent(false));
        pageFromFileSystem = getPageFromFileSystem();
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
        comparePages(pageFromServer, pageFromFileSystem);
    }

    public void testUploadPageWithDifferentVersionAndSameContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();

        String changes = this.getName() + RandomUtils.nextInt();
        makeChangesOnServer(pageFromFileSystem, changes);
        makeLocalChanges(pageFromFileSystem, changes);

        String pageContentFromServer = getContentFromServer(pageFromFileSystem);
        String pageContentFromFileSystem = getContentFromFileSystem(pageFromFileSystem);
        assert pageContentFromServer.equals(pageContentFromFileSystem);

        assertTrue(Confluence.getInstance().uploadContent());
        pageFromFileSystem = getPageFromFileSystem();
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        comparePageFromDifferentPlacesByContent(pageFromServer, pageFromFileSystem);
        comparePages(pageFromServer, pageFromFileSystem);
    }

    public void testUpdatePageWithDifferentVersionAndDifferentContent() {
        ConfluencePage pageFromFileSystem = getPageFromFileSystem();
        makeChangesOnServer(pageFromFileSystem, this.getName() + RandomUtils.nextInt());
        makeLocalChanges(pageFromFileSystem, this.getName() + RandomUtils.nextInt());
        assert !getContentFromServer(pageFromFileSystem).equals(
                getContentFromFileSystem(pageFromFileSystem));

        pageFromFileSystem = getPageFromFileSystem();
        String contentFromFileSystemBefore = getContentFromFileSystem(pageFromFileSystem);
        ConfluencePage pageFromServer = getPageFromServer(pageFromFileSystem.getId());

        assert !pageFromServer.getVersion().equals(pageFromFileSystem.getVersion());

        assertFalse(Confluence.getInstance().updateContent(false));
        pageFromFileSystem = getPageFromFileSystem();
        pageFromServer = getPageFromServer(pageFromFileSystem.getId());
        comparePages(pageFromFileSystem, pageFromServer);
        assertEquals("Content of page from file system was changed after update",
                Utils.readFromFile(new File(pageFromFileSystem.getFilePath() + "_local")),
                contentFromFileSystemBefore);
    }

    public void testUploadPageWithDifferentVersionAndDifferentContent() {
        ConfluencePage pageFromFileSystemBefore = getPageFromFileSystem();
        makeChangesOnServer(pageFromFileSystemBefore, this.getName() + RandomUtils.nextInt());
        makeLocalChanges(pageFromFileSystemBefore, this.getName() + RandomUtils.nextInt());
        assert !getContentFromServer(pageFromFileSystemBefore).equals(
                getContentFromFileSystem(pageFromFileSystemBefore));

        pageFromFileSystemBefore = getPageFromFileSystem();
        ConfluencePage pageFromServerBefore = getPageFromServer(pageFromFileSystemBefore.getId());

        assert !pageFromServerBefore.getVersion().equals(pageFromFileSystemBefore.getVersion());

        assertFalse(Confluence.getInstance().uploadContent());
        ConfluencePage pageFromFileSystemAfter = getPageFromFileSystem();
        ConfluencePage pageFromServerAfter = getPageFromServer(pageFromFileSystemAfter.getId());
        comparePages(pageFromFileSystemBefore, pageFromFileSystemAfter);
        comparePages(pageFromServerBefore, pageFromServerAfter);

    }

    private void comparePages(ConfluencePage pageFromServer, ConfluencePage pageFromFileSystem) {
        assertEquals(pageFromServer.getId(), pageFromFileSystem.getId());
        assertEquals(pageFromServer.getParentId(), pageFromFileSystem.getParentId());
        assertEquals(pageFromServer.getSpace(), pageFromFileSystem.getSpace());
        assertEquals(pageFromServer.getHashCode(), pageFromFileSystem.getHashCode());
        assertEquals(pageFromServer.getTitle(), pageFromFileSystem.getTitle());
        assertEquals(pageFromServer.getVersion(), pageFromFileSystem.getVersion());
    }

    private void comparePageFromDifferentPlacesByContent(ConfluencePage pageFromServer, ConfluencePage pageFromFileSystem) {
        assertEquals("first - from server, second - from file",
                getContentFromServer(pageFromServer),
                getContentFromFileSystem(pageFromFileSystem));
    }


    private void makeLocalChanges(ConfluencePage page, String content) {
        Utils.writeToFile(new File(page.getFilePath()), content);
        IndexFileModel.getInstance().updateContentHashFromFileSystem();
    }

    private void makeChangesOnServer(ConfluencePage page, String content) {
        Hashtable<String, String> pageData = new Hashtable<String, String>();
        pageData.put("id", page.getId());
        pageData.put("version", page.getVersion());
        pageData.put("space", page.getSpace());
        pageData.put("title", page.getTitle());
        pageData.put("parentId", page.getParentId());
        pageData.put("content", content);
        ConfluenceConnector.getInstance().savePageRemoteCall(pageData);
    }

    private ConfluencePage getPageFromFileSystem() {
        File[] files = new File(ConfluenceConfig.CONFLUENCE_ROOT_DIRECTORY + File.separatorChar + "TestSpace").listFiles();
        assert files != null;
        File file = files[0];
        ConfluencePage pageFromFileSystem = IndexFileModel.getInstance().getPageInfo(file.getAbsolutePath());
        assert pageFromFileSystem != null;
        return pageFromFileSystem;
    }

    private ConfluencePage getPageFromServer(String pageId) {
        ConfluencePage pageFromServer = ConfluenceConnector.getInstance().getPageById(pageId);
        assert pageFromServer != null;
        return pageFromServer;
    }

    private String getContentFromServer(ConfluencePage pageFromFileSystem) {
        return ConfluenceConnector.getInstance().getPageContent(pageFromFileSystem.getId());
    }

    private String getContentFromFileSystem(ConfluencePage pageFromFileSystem) {
        return Utils.readFromFile(new File(pageFromFileSystem.getFilePath()));
    }
}
