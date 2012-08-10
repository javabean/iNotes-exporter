iNotes-exporter
===============

This project reads a Lotus Domino iNotes 8.5 webmail and exports the data as POP3, mbox and/or maildir.  
SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox maildir export

Requirements
------------
* Lotus iNotes credentials (tested with version 8.5.x)  
* Java 6
* Maven for compiling

Configuration
-------------
mbox/maildir export: `src/main/resources/iNotes.properties`  
pop3 server: `src/main/resources/pop3.properties`  
logging: `src/main/resources/simplelogger.properties`  

Compiling
---------
(after configuration!)

	mvn -Dmaven.test.skip clean assembly:assembly -DdescriptorId=jar-with-dependencies

Running
-------

### mbox/maildir export

	java -jar target/iNotes-exporter-1.3.1-jar-with-dependencies.jar (mboxrd|maildir) <output_file|output_dir> [yyyy-MM-dd'T'HH:mm]

where
* `yyyy-MM-dd'T'HH:mm` is the date of the oldest message to export
	* if none provided, and `output_file` does not exists, exports all messages
	* if none provided, and `output_file` exists, do an incremental export since last time this tool was run
* `<output_file>` will be overwritten if no start date is given. Otherwise, the newest email data is appended to it.

examples:
* `java -jar target/iNotes-exporter-1.3.1-jar-with-dependencies.jar mboxrd /tmp/my_mailbox 2012-01-20T20:00`
* `java -jar target/iNotes-exporter-1.3.1-jar-with-dependencies.jar maildir /tmp/my_mailir 2012-01-20T20:00`

If you don't know which mbox format to choose (mboxo, mboxrd, mboxcl, mboxcl2), use mboxrd.

The (incremental) last export date is stored in the Java Preferences (`~/.java/` for Linux / *BSD, `~/Library/Preferences/fr.cedrik.inotes.plist` for Mac OS X, and the Registry for Windows).

### list available iNotes folders

	java -jar target/iNotes-exporter-1.3.1-jar-with-dependencies.jar listfolders

### pop3 server

	java -jar target/iNotes-exporter-1.3.1-jar-with-dependencies.jar pop3server

use as pop3 user login: `username@https://webmail.example.com`

Additional (non-standard) POP3 commands:
* `SHUTDOWN`: to shutdown the POP3 server (set the secret in `pop3.properties`)
* `QUOTA`: gives information on quota usage
* `LOGGER`: set loggers level at runtime
* `FOLDERS`: list available iNotes folders
* `FOLDER <id>`: change current folder
