.PHONY: all clean compile test deploy deps

all: clean compile test

clean:
	lein clean
	rm -f deps.txt
	rm -f pom.xml*

compile:
	lein compile

test:
	lein midje

deploy:
	if test -n "$$BUILD_NUMBER"; then lein deploy clojars; else echo "BUILD_NUMBER not set!"; fi

deps:
	lein deps :tree > deps.txt
