.PHONY: all clean compile test deploy deps

LEIN := ./lein

all: clean compile test

clean:
	${LEIN} clean
	rm -f deps.txt
	rm -f pom.xml*

compile:
	${LEIN} compile

test:
	${LEIN} midje

deploy: test
	${LEIN} clojars

deps:
	${LEIN} deps :tree > deps.txt
