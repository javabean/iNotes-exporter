iNotes-exporter
===============

This project reads a Lotus Domino iNotes 8.5 webmail and exports the data as POP3, mbox and/or maildir.  
SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox maildir export

Requirements
------------
* Lotus iNotes credentials (tested with version 8.5.3)  
* Java 6
* Maven 3 for compiling

Configuration
-------------
Personal credentials and preferences: `src/main/resources/iNotes.properties` (can be overriden on command-line)  
logging: `src/main/resources/simplelogger.properties`

If you need to use an HTTP/SOCKS proxy, you can also use the usual [Java Networking System properties](http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html "JavaDoc: Networking Properties").

Compiling
---------
(after configuration!)

	mvn -Dmaven.test.skip clean assembly:assembly -DdescriptorId=jar-with-dependencies

Running
-------

### mbox/maildir export

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] [-Dnotes.folder.id=($Inbox)] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar (mboxrd|maildir) <output_file|output_dir> [start date: yyyy-MM-dd'T'HH:mm [end date: yyyy-MM-dd'T'HH:mm [--delete]]]

where
* `<output_file>` will be overwritten if no start date is given. Otherwise, the newest email data is appended to it.
* `yyyy-MM-dd'T'HH:mm` is the date of the oldest message to export
	* if none provided, and `output_file` does not exists, exports all messages
	* if none provided, and `output_file` exists, do an incremental export since last time this tool was run
* `yyyy-MM-dd'T'HH:mm` is the date of the newest message to export
* `--delete`: delete messages from the Notes server after exporting

examples:
* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildir ~/my_mailir 2012-01-20T20:00`
* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar mboxrd ~/my_mailbox_archive_2012 2012-01-01T00:00 2013-01-01T00:00 --delete`

If you don't know which mbox format to choose (mboxo, mboxrd, mboxcl, mboxcl2, MMDF, maildir), use mboxrd.

The (incremental) last export date is stored in the Java Preferences (`~/.java/` for Linux / *BSD, `~/Library/Preferences/fr.cedrik.inotes.plist` for Mac OS X, and the Registry for Windows).

If running unattended, please have a process monitor the output for all `ERROR`'s that could occur!

### list available iNotes folders

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar listfolders

### maildir++ export

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildirpp <output_dir>

example:
* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildirpp ~/my_mailir_plus_plus`

If you don't know which maildir format to choose (maildir++, MH), use maildir++.

The (incremental) last export date is stored in the Java Preferences (`~/.java/` for Linux / *BSD, `~/Library/Preferences/fr.cedrik.inotes.plist` for Mac OS X, and the Registry for Windows).

If running unattended, please have a process monitor the output for all `ERROR`'s that could occur!

### pop3 server

	java [-Dpop3.port=110] [-Dpop3.shutdown=now!] [-Dinotes.server=...] [-Dnotes.folder.id=($Inbox)] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar pop3server

use as pop3 user login: `username@https://webmail.example.com`

This POP3 server has a user and IP lock out mechanism if there are too many failed authentication attempts in a given period of time. It achieves this by recording all failed logins, including those for users that do not exist. To prevent a DOS by deliberating making requests with invalid users (and hence causing this cache to grow) the size of the list of users and IPs that have failed authentication is limited. (Configurable in `iNotes.properties`)

Additional (non-standard) POP3 commands:
* `SHUTDOWN <secret>`: to shutdown the POP3 server (set the secret in `iNotes.properties`)
* `QUOTA`: gives information on quota usage
* `FOLDER`: list available iNotes folders
* `FOLDER <id>`: change current folder
* `LOGGER`: get loggers levels
* `LOGGER <logger_id> <level>`: set logger level
