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

check-deploy:
	ifndef BUILD_NUMBER
		$(error BUILD_NUMBER is not set)
	endif

deploy: check-deploy
	lein deploy clojars

deps:
	lein deps :tree > deps.txt
