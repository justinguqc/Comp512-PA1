all: compile-client

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

compile-client: ../Server/RMIInterface.jar
	make -C ../Server/ compile-server-rmi
	javac -encoding UTF-8 -cp ../Server Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
