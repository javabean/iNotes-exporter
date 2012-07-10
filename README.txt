This project reads a Lotus Domino iNotes 8.5 webmail data.

Requirements (see pom.xml)
	Java 6
	slf4j-api 1.6.6
	jcl-over-slf4j 1.6.6
	slf4j-simple 1.6.6 (or any other logger backend)
	commons-lang3 3.1
	commons-io 2.3
	spring-web 3.1.x

configuration:
	/src/main/resources/iNotes.properties
	/src/main/resources/simplelogger.properties

mbox export:
	java fr.cedrik.inotes.mbox.MBoxrd <output_file>

pop3 server:
	TODO!
	login: username@https://webmail.example.com










SEO keywords: IBM Lotus Notes Domino iNotes POP3 mbox export
