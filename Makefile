.PHONY: all clean test test-current test-1.7 test-1.6 deploy deps

LEIN := ./lein

all: clean test

clean:
	${LEIN} clean
	rm -f deps.txt
	rm -f pom.xml*

test-current: clean
	${LEIN} test

test-1.7: clean
	${LEIN} with-profile 1.7 test

test-1.6: clean
	${LEIN} with-profile 1.6 test

test: test-1.6 test-1.7 test-current

deploy: test
	${LEIN} deploy clojars

deps:
	${LEIN} deps :tree &> deps.txt
