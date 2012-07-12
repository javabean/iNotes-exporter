This project reads a Lotus Domino iNotes 8.5 webmail data.
SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox export

Requirements
============
(see pom.xml)
	Java 6
	slf4j-api 1.6.6
	jcl-over-slf4j 1.6.6
	slf4j-simple 1.6.6 (or any other logger backend)
	commons-lang3 3.1
	commons-io 2.4
	spring-web 3.1.x

Configuration
=============
mbox export:
	src/main/resources/iNotes.properties
pop3 server:
	src/main/resources/pop3.properties
logging:
	src/main/resources/simplelogger.properties

Compiling
=========
(after configuration!)
	mvn -Dmaven.test.skip clean package
or
	mvn -Dmaven.test.skip clean assembly:assembly -DdescriptorId=jar-with-dependencies

Running
=======
mbox export:
	java -cp target/iNotes-exporter-1.0-jar-with-dependencies.jar fr.cedrik.inotes.mbox.MBoxrd <output_file> [yyyy-MM-dd'T'HH:mm]
		where yyyy-MM-dd is the date of the oldest message to export; if none provided, exports all messages
		<output_file> will be overwritten if no start date is given. Otherwise, the newest email data is appended to it.
	example: java -cp target/iNotes-exporter-1.0-jar-with-dependencies.jar fr.cedrik.inotes.mbox.MBoxrd /tmp/email 2012-01-20T20:00

pop3 server:
	java -cp target/iNotes-exporter-1.0-jar-with-dependencies.jar fr.cedrik.inotes.pop3.POP3Server
	login: username@https://webmail.example.com
