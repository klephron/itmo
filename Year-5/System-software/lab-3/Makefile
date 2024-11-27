rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR          ?= mkdir -p
CP             ?= cp -f
ANTLR          ?= java -jar /usr/local/maven/m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar
FORMAT         ?= clang-format --verbose -i
CC             ?= gcc
AR             ?= ar

FLAGS_NDEBUG   ?= -DNDEBUG
FLAGS_SANITIZE ?= -fsanitize=address,undefined,leak
FLAGS          ?= -g3 -O0 -m64 -Wall -Wextra $(FLAGS_SANITIZE) $(FLAGS_NDEBUG)

BUILD_DIR      ?= $(CURDIR)/build
GEN_DIR        ?= $(BUILD_DIR)/_gen
TARGET_DIR     ?= $(BUILD_DIR)
TARGET_TST_DIR ?= $(BUILD_DIR)/test

SRC_DIR := $(CURDIR)/src

UTIL.SRC_DIR   := $(SRC_DIR)/util
UTIL.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(UTIL.SRC_DIR))

COMPILER.SRC_DIR   := $(SRC_DIR)/compiler
COMPILER.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(COMPILER.SRC_DIR))
COMPILER.GEN_DIR   := $(patsubst $(SRC_DIR)/%,$(GEN_DIR)/%, $(COMPILER.SRC_DIR))

X86_64_CORE.SRC_DIR   := $(SRC_DIR)/x86_64_core
X86_64_CORE.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(X86_64_CORE.SRC_DIR))

TEST.SRC_DIR   := $(CURDIR)/test
TEST.BUILD_DIR := $(patsubst $(CURDIR)/%,$(BUILD_DIR)/%,$(TEST.SRC_DIR))


.DEFAULT_GOAL := build
.PHONY := grammar build dot/pdf run run/ast build/test test format index clean help echo FORCE

FORCE:


define COMPILER.ENV
	MKDIR="$(MKDIR)" \
	CP="$(CP)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	LIBS="$(UTIL.BUILD_DIR)/libutil.a" \
	INCS="$(patsubst %,-I%,$(sort $(SRC_DIR) $(GEN_DIR))) -lantlr3c" \
	ANTLR="$(ANTLR)" \
	GEN_DIR="$(COMPILER.GEN_DIR)" \
	BUILD_DIR="$(COMPILER.BUILD_DIR)"
endef

define UTIL.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(UTIL.BUILD_DIR)" \
	AR="$(AR)"
endef

define X86_64_CORE.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(X86_64_CORE.BUILD_DIR)"
endef

define TEST.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	LIBS="$(COMPILER.BUILD_DIR)/libcompiler.a $(UTIL.BUILD_DIR)/libutil.a $(X86_64_CORE.BUILD_DIR)/libx86_64_core.a" \
	INCS="-I$(SRC_DIR) -lantlr3c -lcriterion" \
	BUILD_DIR="$(TEST.BUILD_DIR)"
endef


### Makefile of util module
util: util/build/libutil.a
util/%:
	@$(MAKE) --no-print-directory -C $(UTIL.SRC_DIR) $(patsubst util/%,%,$@) $(UTIL.ENV)

### Makefile of compiler module
compiler: compiler/build/main
compiler/%: util/build/libutil.a
	@$(MAKE) --no-print-directory -C $(COMPILER.SRC_DIR) $(patsubst compiler/%,%,$@) $(COMPILER.ENV)

### Makefile of X86_64_core module
x86_64_core: x86_64_core/build/libx86_64_core.a
x86_64_core/%:
	@$(MAKE) --no-print-directory -C $(X86_64_CORE.SRC_DIR) $(patsubst x86_64_core/%,%,$@) $(X86_64_CORE.ENV)

### Makefile of test module
test: test/build
test/%: util/build/libutil.a compiler/build/libcompiler.a x86_64_core/build/libx86_64_core.a
	@$(MAKE) --no-print-directory -C $(TEST.SRC_DIR) $(patsubst test/%,%,$@) $(TEST.ENV)

### Run all tests
run/test: FORCE | test/build
	@for file in $(call rwildcard,$(TEST.BUILD_DIR),*); do "$$file"; done;

### Run main executable
run: compiler/build/main
	./build/compiler/main io/input.txt

### Run main executable and generate dot
run/pdf: compiler/build/main
	./build/compiler/main -d . -o io/a.asm --ignore-errors \
		--ast --cfg --cg \
		--cfg-add-expr --hir-symbols --mir \
		io/input.txt || true
	@echo "--------------------ASSEMBLY(io/a.asm)--------------------"
	@cat io/a.asm
	@echo "--------------------DOT2PDF(io/)--------------------"
	for file in $$(find io -type f -name "*.dot"); do dot -Tpdf "$$file" -o "$${file%.dot}.pdf" || true; done

### Format
format:
	@$(FORMAT) $(call rwildcard,$(SRC_DIR),*.c) $(call rwildcard,$(SRC_DIR),*.h) \
		 $(call rwildcard,$(TEST.SRC_DIR),*.c) $(call rwildcard,$(TEST.SRC_DIR),*.h)

### Index project
index:
	make clean
	bear -- make -j$(nproc) util compiler x86_64_core test/build

### Remove build and target files
clean:
	rm -rf $(BUILD_DIR)

### Remove io output files
clean/out:
	find io -type f -name "*.dot" -delete
	find io -type f -name "*.pdf" -delete
	find io -type f -name "*.asm" -delete

### Help
help:
	@printf "Targets:\n"
	@awk '/^[a-zA-Z\-_0-9%:\\]+/ { \
		helpMessage = match(lastLine, /^###(.*)/); \
		if (helpMessage) { \
			helpCommand = $$1; \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			gsub("\\\\", "", helpCommand); \
			gsub(":+$$", "", helpCommand); \
			printf "  \x1b[32;01m%-26s\x1b[0m %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST) | sort -u
