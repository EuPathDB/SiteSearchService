C_BLUE := "\\033[94m"
C_NONE := "\\033[0m"
C_CYAN := "\\033[36m"

.PHONY: default
default:
	@echo "Please choose one of:"
	@echo ""
	@echo "$(C_BLUE)  make jar$(C_NONE)"
	@echo "    Compiles a 'fat jar' from this project and its dependencies."
	@echo ""
	@echo "$(C_BLUE)  make docker$(C_NONE)"
	@echo "    Builds a runnable docker image for this service"
	@echo ""
	@echo "$(C_BLUE)  make clean$(C_NONE)"
	@echo "    Remove files generated by other targets; put project back in its"
	@echo "    original state."
	@echo ""

.PHONY: jar
jar: fgputil
	mvn clean package

fgputil:
	bash bin/build-fgputil.sh

.PHONY: docker
docker:
	docker build -t site-search .

.PHONY: clean
clean:
	@rm -rf .build target
