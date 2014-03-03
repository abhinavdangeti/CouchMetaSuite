compile:
	javac -cp :lib/* Helper.java

run:
	java -cp .:lib/* Helper

clean:
	rm -rf *.class src/*.class

removelogs:
	rm -rf *log.txt
