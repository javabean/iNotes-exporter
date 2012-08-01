iNotes-exporter
===============

This project reads a Lotus Domino iNotes 8.5 webmail and exports the data as POP3, mbox and/or maildir.  
SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox maildir export

Requirements
------------
Lotus iNotes credentials! (tested with version 8.5.x)  
Java (see `pom.xml`):
* Java 6
* slf4j-api 1.6.6
* jcl-over-slf4j 1.6.6
* slf4j-simple 1.6.6 (or any other logger backend)
* commons-lang3 3.1
* commons-io 2.4
* spring-web 3.1.x

Configuration
-------------
mbox/maildir export: `src/main/resources/iNotes.properties`  
pop3 server: `src/main/resources/pop3.properties`  
logging: `src/main/resources/simplelogger.properties`  

Compiling
---------
(after configuration!)

	mvn -Dmaven.test.skip clean assembly:assembly -DdescriptorId=jar-with-dependencies
(or `mvn -Dmaven.test.skip clean package` if you want to manage your classpath manually)

Running
-------

### mbox/maildir export

	java -jar target/iNotes-exporter-1.3-jar-with-dependencies.jar (mboxrd|maildir) <output_file|output_dir> [yyyy-MM-dd'T'HH:mm]

where
* `yyyy-MM-dd'T'HH:mm` is the date of the oldest message to export
	* if none provided, and `output_file` does not exists, exports all messages
	* if none provided, and `output_file` exists, do an incremental export since last time this tool was run
* `<output_file>` will be overwritten if no start date is given. Otherwise, the newest email data is appended to it.

examples:
* `java -jar target/iNotes-exporter-1.3-jar-with-dependencies.jar mboxrd /tmp/my_mailbox 2012-01-20T20:00`
* `java -jar target/iNotes-exporter-1.3-jar-with-dependencies.jar maildir /tmp/my_mailir 2012-01-20T20:00`

If you don't know which mbox format to choose (mboxo, mboxrd, mboxcl, mboxcl2), use mboxrd.

### list available iNotes folders

	java -jar target/iNotes-exporter-1.3-jar-with-dependencies.jar listfolders

### pop3 server

	java -jar target/iNotes-exporter-1.3-jar-with-dependencies.jar pop3server

use as pop3 user login: `username@https://webmail.example.com`

Additional (non-standard) POP3 commands:
* `SHUTDOWN`: to shutdown the POP3 server (set the secret in `pop3.properties`)
* `QUOTA`: gives information on quota usage
* `LOGGER`: set loggers level at runtime
* `FOLDERS`: list available iNotes folders
* `FOLDER <id>`: change current folder
