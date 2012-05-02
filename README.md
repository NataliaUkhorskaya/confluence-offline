confluence-offline-editor
=========================

Download content from confluence space, edit it and load it back

### Usage

* download - download all files from Confluence space, specified in confluence.properties file. If file already exists it will be overwritten without any exception.
* update - update all files from Confluence space. If files are different you will see an exception.
* upload - upload all changes to Confluence space. If versions are different you will see an exception.


### confluence.properties file

* confluence.server=http://confluence.com  - confluence server url
* confluence.username=user - username for confluence server
* confluence.password=password  - password for confluence server
* confluence.space=MySpace - space on Confluence
* confluence.root.directory=confluence - directory to store content from confluence