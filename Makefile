.PHONY: all clean test deploy deps

LEIN := ./lein

all: clean test

clean:
	${LEIN} clean
	rm -f deps.txt
	rm -f pom.xml*

test:
	${LEIN} midje

deploy: test
	${LEIN} clojars

deps:
	${LEIN} with-profile production deps :tree &> deps.txt
