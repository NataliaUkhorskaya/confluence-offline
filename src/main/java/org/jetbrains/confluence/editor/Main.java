package org.jetbrains.confluence.editor;

/**
 * @author Natalia.Ukhorskaya
 */

public class Main {
    public static void main(String[] args) {
        ConfluenceConfig.parsePropertiesFile();
        Confluence.getInstance().login();

        if (args[0].equals("-update")) {
            IndexFileModel.getInstance().updateContentHashFromFileSystem();
            if (args.length == 2) {
                if (args[1].equals("-force")) {
                    Confluence.getInstance().updateContent(true);
                }
                else {
                    Confluence.getInstance().updateFile(args[1], false);
                }
            }
            else if (args.length == 3) {
                if (args[1].equals("-force")) {
                    Confluence.getInstance().updateFile(args[2], true);
                }
            }
            else {
                Confluence.getInstance().updateContent(false);
            }

        }
        else if (args[0].equals("-download")) {
            IndexFileModel.getInstance().createIndexFile();
            Confluence.getInstance().loadAllContent();
        }
        else if (args[0].equals("-upload")) {
            IndexFileModel.getInstance().updateContentHashFromFileSystem();
            Confluence.getInstance().uploadContent();
        }
    }
}
