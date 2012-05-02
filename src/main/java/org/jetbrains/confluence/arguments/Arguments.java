package org.jetbrains.confluence.arguments;

import com.sampullara.cli.Argument;

/**
 * @author Natalia.Ukhorskaya
 */

public class Arguments {
    @Argument(value = "download", description = "Download files", required = false)
    public boolean download;

    @Argument(value = "update", description = "Update files", required = false)
    public boolean update;

    @Argument(value = "upload", description = "Upload files to server", required = false)
    public boolean upload;

    @Argument(value = "help", alias = "h", description = "Show help", required = false)
    public boolean help;

    @Argument(value = "userName", description = "Username for confluence server.", required = false)
    public String userName;

    @Argument(value = "server", description = "Url to confluence server.", required = false)
    public String server;

    @Argument(value = "rootDirectory", description = "Directory to store content from confluence.", required = false)
    public String rootDirectory;

    @Argument(value = "space", description = "Space on confluence to download", required = false)
    public String space;

    @Argument(value = "file", description = "File name for update.", required = false)
    public String file;

    @Argument(value = "force", description = "Override local changes until update.", required = false)
    public boolean force;

}
