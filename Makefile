.PHONY: check-java build verify test format clean version validate doctor run

JAVA ?= java
MVN := ./mvnw

check-java:
	@$(JAVA) -version
	@$(MVN) -version

build:
	$(MVN) verify

verify: build

test:
	$(MVN) test

format:
	$(MVN) spotless:apply

clean:
	$(MVN) clean

version:
	./bin/autotest-tool version

validate:
	./bin/autotest-tool validate --workspace=autotest-workspace

doctor:
	./bin/autotest-tool doctor --workspace=autotest-workspace --env=local

run:
	./bin/autotest-tool run --workspace=autotest-workspace --env=local --suite=upgrade-regression
