all: compile-server-rmi

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

compile-server-rmi: Server/RMI/*.java Server/TCP/*.java Server/Interface/*.java Server/Common/*.java
	javac -encoding UTF-8 Server/RMI/*.java Server/TCP/*.java Server/Interface/*.java Server/Common/*.java

compile-server-tcp: compile-server-rmi

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/TCP/*.class
	rm -f RMIInterface.jar
