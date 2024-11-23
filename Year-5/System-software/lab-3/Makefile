rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR          ?= mkdir -p
CP             ?= cp -f
ANTLR          ?= java -jar /usr/local/maven/m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar
FORMAT         ?= clang-format --verbose -i
CC             ?= gcc

FLAGS_NDEBUG   ?= -DNDEBUG
FLAGS_SANITIZE ?= -fsanitize=address,undefined,leak
FLAGS          ?= -g3 -O0 -m64 -Wall -Wextra $(FLAGS_SANITIZE) $(FLAGS_NDEBUG)
LIBS           ?= antlr3c

SRC_DIR        ?= src
LIB_DIR        ?= $(SRC_DIR)/lib
ANTLR_DIR      ?= $(LIB_DIR)/antlr3
ANTLR_REC_DIR  ?= $(ANTLR_DIR)/rec
ANTLR_TREE_DIR ?= $(ANTLR_DIR)/tree
BIN_DIR        ?= $(SRC_DIR)
TST_DIR        ?= test

BUILD_DIR     ?= build
GEN_LIB_DIR   ?= $(BUILD_DIR)/_gen/lib
BUILD_LIB_DIR ?= $(BUILD_DIR)/lib
OBJ_DIR       ?= $(BUILD_LIB_DIR)

TARGET_DIR     ?= $(BUILD_DIR)
TARGET_TST_DIR ?= $(BUILD_DIR)/test


# library files
ANTLR3_RECS 		:= $(wildcard $(ANTLR_REC_DIR)/*.g3)
SRCS.ANTLR3_REC := $(patsubst $(LIB_DIR)/%.g3,$(GEN_LIB_DIR)/%Lexer.c, $(ANTLR3_RECS)) \
							 $(patsubst $(LIB_DIR)/%.g3,$(GEN_LIB_DIR)/%Parser.c, $(ANTLR3_RECS))
HDRS.ANTLR3_REC := $(patsubst %.c,%.h,$(SRCS.ANTLR3_REC))
OBJS.ANTLR3_REC := $(patsubst $(GEN_LIB_DIR)/%.c,$(OBJ_DIR)/%.o,$(SRCS.ANTLR3_REC))
TKNS.ANTLR3_REC := $(patsubst $(LIB_DIR)/%.g3,$(GEN_LIB_DIR)/%.tokens, $(ANTLR3_RECS))
DIR.ANTLR3_REC  := $(patsubst $(LIB_DIR)/%, $(GEN_LIB_DIR)/%, $(ANTLR_REC_DIR))

ANTLR3_TREES := $(wildcard $(ANTLR_TREE_DIR)/*.g3)
SRCS.ANTLR3_TREE := $(patsubst $(LIB_DIR)/%.g3,$(GEN_LIB_DIR)/%.c, $(ANTLR3_TREES))
HDRS.ANTLR3_TREE := $(patsubst %.c,%.h,$(SRCS.ANTLR3_TREE))
OBJS.ANTLR3_TREE := $(patsubst $(GEN_LIB_DIR)/%.c,$(OBJ_DIR)/%.o,$(SRCS.ANTLR3_TREE))
TKNS.ANTLR3_TREE := $(patsubst $(LIB_DIR)/%.g3,$(GEN_LIB_DIR)/%.tokens, $(ANTLR3_TREES))
DIR.ANTLR3_TREE  := $(patsubst $(LIB_DIR)/%, $(GEN_LIB_DIR)/%, $(ANTLR_TREE_DIR))

# without generated files
SRCS.LIB := $(call rwildcard,$(LIB_DIR),*.c)
HDRS.LIB := $(call rwildcard,$(LIB_DIR), *.h)
OBJS.LIB := $(patsubst %.c, $(OBJ_DIR)/%.o, $(patsubst $(LIB_DIR)/%, %, $(SRCS.LIB)))

# all library files
SRCS := $(SRCS.LIB) $(SRCS.ANTLR3_REC) $(SRCS.ANTLR3_TREE)
HDRS := $(HDRS.LIB) $(HDRS.ANTLR3_REC) $(HDRS.ANTLR3_TREE)
OBJS := $(OBJS.LIB) $(OBJS.ANTLR3_REC) $(OBJS.ANTLR3_TREE)

# absolute imports
INCLUDE_LIBS := $(LIBS)
INCLUDE_DIRS := $(LIB_DIR) $(GEN_LIB_DIR)
INCS := $(patsubst %,-I%,$(sort $(INCLUDE_DIRS))) $(patsubst %,-l%,$(sort $(INCLUDE_LIBS)))

# executable files
SRCS.BIN := $(filter-out $(SRCS.LIB),$(call rwildcard,$(BIN_DIR),*.c))
HDRS.BIN := $(filter-out $(HDRS.LIB),$(call rwildcard,$(BIN_DIR), *.h))
TGTS.BIN := $(patsubst $(BIN_DIR)/%.c,$(TARGET_DIR)/%,$(SRCS.BIN))

# test files
SRCS.TST := $(call rwildcard,$(TST_DIR),*.c)
TGTS.TST := $(patsubst $(TST_DIR)/%.c,$(TARGET_TST_DIR)/%, $(SRCS.TST))

# directories with trailing slash to be created
DIRS := $(sort $(dir $(OBJS)) $(dir $(OBJS.BIN)) $(dir $(TGTS.BIN)) $(dir $(TGTS.TST)) $(DIR.ANTLR3_REC) $(DIR.ANTLR3_TREE))


# prevent deletion
.PRECIOUS: $(SRCS) $(HDRS) $(OBJS) $(TKNS.ANTLR3_REC)

.DEFAULT_GOAL := build
.PNONY := grammar build dot/pdf run run/ast build/test test format index clean help echo


# create directories
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

# generate grammar files and objects for recognizers (parser and lexer)
$(GEN_LIB_DIR)/%Lexer.c $(GEN_LIB_DIR)/%Parser.c $(GEN_LIB_DIR)/%Lexer.h $(GEN_LIB_DIR)/%Parser.h $(GEN_LIB_DIR)/%.tokens: $(LIB_DIR)/%.g3 | $(DIRS)
	$(ANTLR) $^ -fo $(DIR.ANTLR3_REC)

# generate grammar files and objects for trees
$(GEN_LIB_DIR)/%.c $(GEN_LIB_DIR)/%.h $(GEN_LIB_DIR)/%.tokens: $(LIB_DIR)/%.g3 | $(DIRS) $(TKNS.ANTLR3_REC)
	$(CP) $(TKNS.ANTLR3_REC) $(DIR.ANTLR3_TREE)
	$(ANTLR) $^ -fo $(DIR.ANTLR3_TREE)

# compile generated files
$(OBJ_DIR)/%.o: $(GEN_LIB_DIR)/%.c $(HDRS) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

# compile object files excluding generated
$(OBJ_DIR)/%.o: $(LIB_DIR)/%.c $(HDRS) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

# compile targets
$(TARGET_DIR)/%: $(BIN_DIR)/%.c $(OBJS) | $(DIRS)
	$(CC) $(FLAGS) $^ -o $@ $(INCS)

# compile test targets
$(TARGET_TST_DIR)/%: $(TST_DIR)/%.c $(OBJS) | $(DIRS)
	$(CC) $(FLAGS) -o $@ $< -lcriterion $(INCS) $(OBJS)


### Generate antlr3 source and header files from grammar rules
grammar: $(SRCS.ANTLR3_REC) $(SRCS.ANTLR3_TREE)

### Build executable
build: $(TGTS.BIN)

### Generate dot files and pdf files from them
dot/pdf:
	$(ANTLR) -dfa -nfa -fo $(GEN_LIB_DIR) $(GRM_RECS)
	for file in $(GEN_LIB_DIR)/*.dot; do dot -Tpdf "$$file" -o "$${file%.dot}.pdf"; done

### Run main executable
run: build
	./build/main io/input.txt

### Run main executable and generate dot
run/pdf: build
	./build/main -d . -o io/a.asm --ignore-errors \
		--ast --cfg --cg \
		--cfg-add-expr --hir-symbols --mir \
		io/input.txt || true
	for file in $$(find io -type f -name "*.dot"); do dot -Tpdf "$$file" -o "$${file%.dot}.pdf" || true; done

### Build tests
build/test: $(TGTS.TST)

### Run all tests
test: build/test
	@for file in $(TGTS.TST); do "./$$file"; done;

### Format
format:
	@$(FORMAT) $(SRCS.LIB) $(HDRS.LIB) $(SRCS.TST)

### Index project
index:
	make clean
	bear -- make build/test

### Remove build and target files
clean:
	rm -rf $(BUILD_DIR)

### Remove io output files
clean/out:
	find io -type f -name "*.dot" -delete
	find io -type f -name "*.pdf" -delete

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

### Echo variables
echo/vars:
	@echo MKDIR=$(MKDIR)
	@echo ANTLR=$(ANTLR)
	@echo FORMAT=$(FORMAT)
	@echo CC=$(CC)
	@echo
	@echo FLAGS_NDEBUG=$(FLAGS_NDEBUG)
	@echo FLAGS_SANITIZE=$(FLAGS_SANITIZE)
	@echo FLAGS=$(FLAGS)
	@echo LIBS=$(LIBS)
	@echo
	@echo SRC_DIR=$(SRC_DIR)
	@echo LIB_DIR=$(LIB_DIR)
	@echo ANTLR_DIR=$(ANTLR_DIR)
	@echo ANTLR_REC_DIR=$(ANTLR_REC_DIR)
	@echo INC_DIR=$(INC_DIR)
	@echo TST_DIR=$(TST_DIR)
	@echo
	@echo BUILD_DIR=$(BUILD_DIR)
	@echo BUILD_LIB_DIR=$(BUILD_LIB_DIR)
	@echo GEN_LIB_DIR=$(GEN_LIB_DIR)
	@echo OBJ_DIR=$(OBJ_DIR)
	@echo
	@echo TARGET_DIR=$(TARGET_DIR)
	@echo TARGET_TST_DIR=$(TARGET_TST_DIR)


echo/files:
	@echo ANTLR3_RECS=$(ANTLR3_RECS)
	@echo SRCS.ANTLR3_REC=$(SRCS.ANTLR3_REC)
	@echo HDRS.ANTLR3_REC=$(HDRS.ANTLR3_REC)
	@echo OBJS.ANTLR3_REC=$(OBJS.ANTLR3_REC)
	@echo TKNS.ANTLR3_REC=$(TKNS.ANTLR3_REC)
	@echo DIR.ANTLR3_REC=$(DIR.ANTLR3_REC)
	@echo
	@echo ANTLR3_TREES=$(ANTLR3_TREES)
	@echo SRCS.ANTLR3_TREE=$(SRCS.ANTLR3_TREE)
	@echo HDRS.ANTLR3_TREE=$(HDRS.ANTLR3_TREE)
	@echo OBJS.ANTLR3_TREE=$(OBJS.ANTLR3_TREE)
	@echo TKNS.ANTLR3_TREE=$(TKNS.ANTLR3_TREE)
	@echo DIR.ANTLR3_TREE=$(DIR.ANTLR3_TREE)
	@echo
	@echo SRCS.LIB=$(SRCS.LIB)
	@echo HDRS.LIB=$(HDRS.LIB)
	@echo OBJS.LIB=$(OBJS.LIB)
	@echo
	@echo SRCS=$(SRCS)
	@echo HDRS=$(HDRS)
	@echo OBJS=$(OBJS)
	@echo
	@echo INCLUDE_LIBS=$(INCLUDE_LIBS)
	@echo INCLUDE_DIRS=$(INCLUDE_DIRS)
	@echo INCS=$(INCS)
	@echo
	@echo SRCS.BIN=$(SRCS.BIN)
	@echo HDRS.BIN=$(HDRS.BIN)
	@echo TGTS.BIN=$(TGTS.BIN)
	@echo
	@echo SRCS.TST=$(SRCS.TST)
	@echo TGTS.TST=$(TGTS.TST)
	@echo DIRS=$(DIRS)
