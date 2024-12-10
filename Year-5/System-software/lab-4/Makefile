rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR          ?= mkdir -p
CP             ?= cp -f
ANTLR          ?= java -jar /usr/local/maven/m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar
FORMAT         ?= clang-format --verbose -i
CC             ?= gcc
AR             ?= ar
AS             ?= as
LD             ?= ld

FLAGS_NDEBUG   ?= -DNDEBUG
FLAGS_SANITIZE ?= -fsanitize=address,undefined,leak
FLAGS          ?= -g3 -O0 -m64 -Wall -Wextra $(FLAGS_SANITIZE) $(FLAGS_NDEBUG)

BUILD_DIR      ?= $(CURDIR)/build
GEN_DIR        ?= $(BUILD_DIR)/_gen
TARGET_DIR     ?= $(BUILD_DIR)
TARGET_TST_DIR ?= $(BUILD_DIR)/test

SRC_DIR := $(CURDIR)/src

util.SRC_DIR   := $(SRC_DIR)/util
util.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(util.SRC_DIR))

x86_64_core.SRC_DIR   := $(SRC_DIR)/x86_64_core
x86_64_core.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(x86_64_core.SRC_DIR))
x86_64_core.FLAGS     := -g3 -O0 -m64 -Wall -Wextra
x86_64_core.LIBS      := $(util.BUILD_DIR)/libutil.a

compiler.SRC_DIR   := $(SRC_DIR)/compiler
compiler.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(compiler.SRC_DIR))
compiler.GEN_DIR   := $(patsubst $(SRC_DIR)/%,$(GEN_DIR)/%, $(compiler.SRC_DIR))
compiler.LIBS      := $(util.BUILD_DIR)/libutil.a $(x86_64_core.BUILD_DIR)/libx86_64_all.a

stdc.SRC_DIR   := $(SRC_DIR)/stdc
stdc.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(stdc.SRC_DIR))
stdc.FLAGS     := -g3 -O0 -m64 -Wall -Wextra

debugger.SRC_DIR   := $(SRC_DIR)/debugger
debugger.BUILD_DIR := $(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(debugger.SRC_DIR))
debugger.LIBS      := $(x86_64_core.BUILD_DIR)/libx86_64_all.a $(util.BUILD_DIR)/libutil.a

test.SRC_DIR   := $(CURDIR)/test
test.BUILD_DIR := $(patsubst $(CURDIR)/%,$(BUILD_DIR)/%,$(test.SRC_DIR))
test.LIBS      := $(compiler.BUILD_DIR)/libcompiler.a $(util.BUILD_DIR)/libutil.a $(x86_64_core.BUILD_DIR)/libx86_64_all.a $(stdc.BUILD_DIR)/libstdc.a $(debugger.BUILD_DIR)/libdebugger.a

RUN.AS_FLAGS       := --64 -g
RUN.LD_FLAGS       := -g -z noexecstack
RUN.COMPILER_FLAGS := --mir

MODULES := util compiler x86_64_core stdc debugger test

.DEFAULT_GOAL := compiler
.PHONY := clean/build clean/out compiler debugger format help index run/asm run/pdf run/test stdc test util x86_64_core
.PRECIOUS: $(util.LIBS) $(x86_64_core.LIBS) $(compiler.LIBS) $(debugger.LIBS) $(test.LIBS)

FORCE:


define compiler.ENV
	MKDIR="$(MKDIR)" \
	CP="$(CP)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	LIBS="$(compiler.LIBS)" \
	INCS="$(patsubst %,-I%,$(sort $(SRC_DIR) $(GEN_DIR))) -lantlr3c" \
	ANTLR="$(ANTLR)" \
	GEN_DIR="$(compiler.GEN_DIR)" \
	BUILD_DIR="$(compiler.BUILD_DIR)"
endef

define util.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(util.BUILD_DIR)" \
	AR="$(AR)"
endef

define x86_64_core.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(x86_64_core.FLAGS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(x86_64_core.BUILD_DIR)"
endef

define stdc.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(stdc.FLAGS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(stdc.BUILD_DIR)"
endef

define debugger.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	LIBS="$(debugger.LIBS)" \
	INCS="-I$(SRC_DIR)" \
	BUILD_DIR="$(debugger.BUILD_DIR)"
endef

define test.ENV
	MKDIR="$(MKDIR)" \
	CC="$(CC)" \
	FLAGS="$(FLAGS)" \
	LIBS="$(test.LIBS)" \
	INCS="-I$(SRC_DIR) -lantlr3c -lcriterion" \
	BUILD_DIR="$(test.BUILD_DIR)"
endef


define make-module-rule
$(1)/%: $$($(1).LIBS)
	@$$(MAKE) --no-print-directory -C $$($(1).SRC_DIR) $$(patsubst $(1)/%,%,$$@) $$($(1).ENV)
endef

$(foreach module, $(MODULES), $(eval $(call make-module-rule,$(module))))

### Execute any target of submakefile (recursively build required libs), transitive
/%: FORCE
	@$(eval TARGET_NAME=$(firstword $(subst /, ,$(patsubst $(BUILD_DIR)/%,%,$@)))) \
	$(eval ENV_NAME=$(patsubst %,%.ENV,$(TARGET_NAME))) \
	$(foreach lib,$($(TARGET_NAME).LIBS),$(MAKE) --no-print-directory $(lib) &&) \
	$(MAKE) --no-print-directory -C $(SRC_DIR)/$(TARGET_NAME) $@ $($(ENV_NAME))

### Build libutil.a
util: util/build/libutil.a

### Build compiler executable
compiler: compiler/build/main

### Build libx86_64_core.a
x86_64_core: x86_64_core/build/libx86_64_core.a

### Build libstdc.a
stdc: stdc/build/libstdc.a

### Build debugger executable
debugger: debugger/build/main

### Build tests
test: test/build

### Run all tests or specific
run/test: FORCE test/build
	for file in $(call rwildcard,$(TEST.BUILD_DIR),*.out); do "$$file"; done;
run/test/%: test/build/%
	$(patsubst run/test/%,$(TEST.BUILD_DIR)/%,$@)

### Run main executable and generate dot
run/pdf: run/pdf/io/suite8.txt
run/pdf/%: RUN.INPUT_FILE=$(patsubst run/pdf/%,%,$@)
run/pdf/%: RUN.ASM_FILE=$(patsubst run/pdf/%,%.asm,$@)
run/pdf/%: compiler/build/main
	./build/compiler/main -d . -o $(RUN.ASM_FILE) --ignore-errors \
		--ast --cfg --cg \
		--cfg-add-expr --hir-symbols --mir \
		$(RUN.INPUT_FILE) || true
	@echo "--------------------ASSEMBLY($(RUN.ASM_FILE))--------------------"
	@cat $(RUN.ASM_FILE)
	@echo "--------------------DOT2PDF($(RUN.INPUT_FILE))--------------------"
	for file in $$(find io -type f -name "*.dot"); do dot -Tpdf "$$file" -o "$${file%.dot}.pdf" || true; done

### Compile file
run/asm: run/asm/io/suite8.txt
run/asm/%: RUN.INPUT_FILE=$(patsubst run/asm/%,%,$@)
run/asm/%: RUN.ASM_FILE=$(patsubst run/asm/%,%.asm,$@)
run/asm/%: RUN.OBJECT_FILE=$(patsubst run/asm/%,%.o,$@)
run/asm/%: RUN.OUTPUT_FILE=$(patsubst run/asm/%,%.out,$@)
run/asm/%: compiler/build/main x86_64_core/build/libx86_64_core.a stdc/build/libstdc.a
	./build/compiler/main $(RUN.COMPILER_FLAGS) \
		-o $(RUN.ASM_FILE) $(RUN.INPUT_FILE)
	@echo "--------------------ASSEMBLY($(RUN.ASM_FILE))--------------------"
	@cat $(RUN.ASM_FILE)
	@echo "--------------------COMPILE($(RUN.ASM_FILE))--------------------"
	$(AS) $(RUN.AS_FLAGS) -o $(RUN.OBJECT_FILE) $(RUN.ASM_FILE)
	$(LD) $(RUN.LD_FLAGS) -o $(RUN.OUTPUT_FILE) \
		-dynamic-linker /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 \
		/usr/lib/x86_64-linux-gnu/crt1.o \
		/usr/lib/x86_64-linux-gnu/crti.o \
		-lc \
		$(RUN.OBJECT_FILE) \
		$(BUILD_DIR)/x86_64_core/libx86_64_core.a \
		$(BUILD_DIR)/stdc/libstdc.a \
		/usr/lib/x86_64-linux-gnu/crtn.o
	@echo "--------------------RUN($(RUN.OUTPUT_FILE))--------------------"
	@./$(RUN.OUTPUT_FILE)

### Format
format:
	@$(FORMAT) $(call rwildcard,$(SRC_DIR),*.c) $(call rwildcard,$(SRC_DIR),*.h) \
		 $(call rwildcard,$(TEST.SRC_DIR),*.c) $(call rwildcard,$(TEST.SRC_DIR),*.h)

### Index project
index:
	make clean
	bear -- make -j$(nproc) util compiler x86_64_core test/build

### Remove build files
clean/build:
	rm -rf $(BUILD_DIR)

### Remove io output files
clean/out:
	find io -type f -name "*.dot" -delete
	find io -type f -name "*.pdf" -delete
	find io -type f -name "*.asm" -delete
	find io -type f -name "*.o" -delete
	find io -type f -name "*.out" -delete

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
	@printf "Modules (prefixes of module targets):\n  \033[32;01m$(MODULES)\033[0m \n"
