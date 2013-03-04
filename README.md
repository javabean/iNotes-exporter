iNotes-exporter
===============

This project reads a Lotus Domino iNotes 8.5 webmail and exports the data as POP3(S), mbox and/or maildir.  
SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox maildir export

Requirements
------------
* Lotus iNotes credentials (tested with version 8.5.3)  
* Java 6
* Maven 3 for compiling

Configuration
-------------
Personal credentials and preferences: `src/main/resources/iNotes.properties` (can be overriden on command-line: `-D<key>=<value>`)  
logging: `src/main/resources/simplelogger.properties`

If you need to use an HTTP/SOCKS proxy, you can also use the usual [Java Networking System properties](http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html "JavaDoc: Networking Properties").

Compiling
---------
(after configuration!)

	mvn -Dmaven.test.skip clean assembly:assembly -DdescriptorId=jar-with-dependencies

Running
-------

In addition to exposing a Notes INBOX as a POP3 server, iNotes exporter supports exporting either 1 mailbox or all Notes server emails, to different formats:

* single mailbox
	* `mbox`: traditional UNIX mailbox format; single file contains multiple messages.  
		Multiple variants exist:
		* `mboxo` (deprecated)
		* `mboxrd`
		* `MMDF`
	* `maildir`: one file contains one message
* multiple mailboxes
	* `MH` (deprecated)
	* `maildir++`

### Single mailbox export (`mbox` / `maildir`)

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] [-Dnotes.folder.id=($Inbox)] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar (mboxrd|maildir) <output_file|output_dir> [start date: yyyy-MM-dd'T'HH:mm [end date: yyyy-MM-dd'T'HH:mm [--delete]]]

where

* `<output_file>` will be overwritten if no start date is given. Otherwise, the newest email data is appended to it.
* `yyyy-MM-dd'T'HH:mm` is the date of the oldest message to export
	* if none provided, and `output_file` does not exists, exports all messages
	* if none provided, and `output_file` exists, do an incremental export since last time this tool was run
* `yyyy-MM-dd'T'HH:mm` is the date of the newest message to export
* `--delete`: delete messages from the Notes server after exporting

examples:

* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildir ~/Maildir 2012-01-20T20:00`
* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar mboxrd ~/mailbox_archive_2012 2012-01-01T00:00 2013-01-01T00:00 --delete`

If you don't know which mbox format to choose (mboxo, mboxrd, mboxcl, mboxcl2, MMDF, maildir), use mboxrd.

The (incremental) last export date is stored in the Java Preferences (`~/.java/` for Linux / *BSD, `~/Library/Preferences/fr.cedrik.inotes.plist` for Mac OS X, and the Registry for Windows).

If running unattended, please have a process monitor the output for all `ERROR`'s that could occur!

### List available iNotes folders

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar listfolders

### All mailboxes export (`maildir++`)

	java [-Dinotes.server=...] [-Dnotes.user=...] [-Dnotes.password=...] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildirpp <output_dir>

example:

* `java -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar maildirpp ~/Maildir`

If you don't know which maildir format to choose (maildir++, MH), use maildir++.

The (incremental) last export date is stored in the Java Preferences (`~/.java/` for Linux / *BSD, `~/Library/Preferences/fr.cedrik.inotes.plist` for Mac OS X, and the Registry for Windows).

If running unattended, please have a process monitor the output for all `ERROR`'s that could occur!

### POP3 / POP3S server

#### POP3

	java [-Dpop3.port=110] [-Dpop3.shutdown=now!] [-Dinotes.server=...] [-Dnotes.folder.id=($Inbox)] -jar target/iNotes-exporter-1.7-jar-with-dependencies.jar pop3server

Use as pop3 user login: `username@https://webmail.example.com`

This POP3 server has a user and IP lock out mechanism if there are too many failed authentication attempts in a given period of time. It achieves this by recording all failed logins, including those for users that do not exist. To prevent a DOS by deliberating making requests with invalid users (and hence causing this cache to grow) the size of the list of users and IPs that have failed authentication is limited. (Configurable in `iNotes.properties`)

#### POP3S

POP3 communication is clear-text. For extra security, you can add an SSL/TLS transport to POP3.  
You will need to generate and configure a certificate. For example:

	$JAVA_HOME/bin/keytool -genkey -alias pop3s -keyalg RSA -keysize 2048 -validity 3650 -keystore /opt/iNotes-keystore -storepass b92kqmp -keypass b92kqmp -dname "cn=<your_FQN_server_name>, o=cedrik.fr, l=Paris, s=Ile-de-France, c=FR"

which will then be configured in `iNotes.properties` as:

	pop3s.port=995
	pop3s.keyStoreName=/opt/iNotes-keystore
	pop3s.keyStorePassword=b92kqmp
	pop3s.keyStoreType=jks

As an alternative to configuring POP3S, you can also use [stunnel](http://www.stunnel.org/).

#### Additional (non-standard) POP3 commands

The following POP3 commands are not RFC standard, but can help managing your iNotes account:

* `SHUTDOWN <secret>`: to shutdown the POP3 server (set the secret in `iNotes.properties`)
* `QUOTA`: gives information on quota usage
* `FOLDER`: list available iNotes folders
* `FOLDER <id>`: change current folder
* `LOGGER`: get loggers levels
* `LOGGER <logger_id> <level>`: set logger level
