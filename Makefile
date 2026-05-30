GRADLE = ./gradlew

build:
	$(GRADLE) build

clean:
	$(GRADLE) clean

test:
	$(GRADLE) test

functionalTest:
	$(GRADLE) functionalTest

spotless:
	$(GRADLE) spotlessApply

publishLocal:
	$(GRADLE) publishToMavenLocal
