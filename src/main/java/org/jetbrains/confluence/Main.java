package org.jetbrains.confluence;

import com.sampullara.cli.Args;
import org.jetbrains.confluence.arguments.Arguments;

/**
 * @author Natalia.Ukhorskaya
 */

public class Main {
    public static void main(String[] args) {
        Arguments arguments = new Arguments();
        try {
            Args.parse(arguments, args);
        } catch (IllegalArgumentException e) {
            Args.usage(new Arguments());
            System.exit(1);
        }

        if (arguments.help) {
            Args.usage(new Arguments());
            return;
        }

        if (arguments.download) {
            if (arguments.userName == null ||
                    arguments.server == null ||
                    arguments.space == null ||
                    arguments.rootDirectory == null) {
                System.err.println("Some arguments are missed: userName, server, space, rootDirectory");
                Args.usage(new Arguments());
            }   else {
                ConfluenceConfig.createPropertiesFile(arguments);
//                ConfluenceConfig.parsePropertiesFile();
                Confluence.getInstance().login();
                System.out.println("Downloading content... Please wait.");
                IndexFileModel.getInstance().createIndexFile();
                Confluence.getInstance().loadAllContent();
                System.out.println("Content was downloaded.");
            }
        }
        else {
            ConfluenceConfig.parsePropertiesFile();
            Confluence.getInstance().login();
            if (arguments.update){
                System.out.println("Updating content... Please wait.");
                IndexFileModel.getInstance().updateContentHashFromFileSystem();

                if (arguments.file != null) {
                    Confluence.getInstance().updateFile(arguments.file, arguments.force);
                }  else {
                    Confluence.getInstance().updateContent(arguments.force);
                }
                System.out.println("Content was updated.");
            } else if (arguments.upload) {
                System.out.println("Uploading content to server... Please wait.");
                IndexFileModel.getInstance().updateContentHashFromFileSystem();
                Confluence.getInstance().uploadContent();
                System.out.println("Content was uploaded.");
            }
        }
    }

    private static boolean parseArguments(Arguments arguments, String[] args) {
        try {
            Args.parse(arguments, args);
            return true;
        } catch (IllegalArgumentException e) {
            Args.usage(new Arguments());
            return false;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

}
