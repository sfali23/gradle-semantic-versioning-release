GRADLE = ./gradlew

build:
	$(GRADLE) build

clean:
	$(GRADLE) clean

test:
	$(GRADLE) test

functionalTest:
	$(GRADLE) functionalTest

allTests:
	$(GRADLE) test functionalTest

spotless:
	$(GRADLE) spotlessApply

publishLocal:
	$(GRADLE) publishToMavenLocal

publish:
	$(GRADLE) setReleaseVersion publishToMavenCentral createTag pushTag
