rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR          ?= mkdir -p
ANTLR          ?= java -jar /usr/local/maven/m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar
FORMAT         ?= clang-format --verbose -i
CC             ?= gcc

FLAGS_NDEBUG   ?= -DNDEBUG
FLAGS_SANITIZE ?= -fsanitize=address,undefined,leak
FLAGS          ?= -g3 -O0 -m64 -Wall -Wextra $(FLAGS_SANITIZE) $(FLAGS_NDEBUG)
LIBS           ?= antlr3c

SRC_DIR     ?= src
LIB_DIR     ?= $(SRC_DIR)/lib
GRM_DIR     ?= $(LIB_DIR)/grammar
GRM_REC_DIR ?= $(GRM_DIR)/rec
BIN_DIR     ?= $(SRC_DIR)
TST_DIR     ?= test

BUILD_DIR     ?= build
BUILD_LIB_DIR ?= $(BUILD_DIR)/lib
GRM_GEN_DIR   ?= $(BUILD_LIB_DIR)/_gen
OBJ_DIR       ?= $(BUILD_LIB_DIR)

TARGET_DIR     ?= $(BUILD_DIR)
TARGET_TST_DIR ?= $(BUILD_DIR)/test


# library files
GRMS := $(wildcard $(GRM_DIR)/*.g3)
SRCS.GRM := $(patsubst $(GRM_DIR)/%.g3,$(GRM_GEN_DIR)/%.c, $(GRMS))
HDRS.GRM := $(patsubst %.c,%.h,$(SRCS.GRM))
OBJS.GRM := $(patsubst %.c,%.o,$(SRCS.GRM))

GRM_RECS := $(call rwildcard,$(GRM_REC_DIR), *.g3)
SRCS.GRM_REC := $(patsubst $(GRM_REC_DIR)/%.g3,$(GRM_GEN_DIR)/%Lexer.c, $(GRM_RECS)) \
							 $(patsubst $(GRM_REC_DIR)/%.g3,$(GRM_GEN_DIR)/%Parser.c, $(GRM_RECS))
HDRS.GRM_REC := $(patsubst %.c,%.h,$(SRCS.GRM_REC))
OBJS.GRM_REC := $(patsubst %.c,%.o,$(SRCS.GRM_REC))
TKNS.GRM_REC := $(patsubst $(GRM_REC_DIR)/%.g3,$(GRM_GEN_DIR)/%.tokens, $(GRM_RECS))
# without generated files
SRCS.LIB := $(call rwildcard,$(LIB_DIR),*.c)
HDRS.LIB := $(call rwildcard,$(LIB_DIR), *.h)
OBJS.LIB := $(patsubst %.c, $(OBJ_DIR)/%.o, $(patsubst $(LIB_DIR)/%, %, $(SRCS.LIB)))
# all library files
SRCS := $(SRCS.LIB) $(SRCS.GRM) $(SRCS.GRM_REC)
HDRS := $(HDRS.LIB) $(HDRS.GRM) $(HDRS.GRM_REC)
OBJS := $(OBJS.LIB) $(OBJS.GRM) $(OBJS.GRM_REC)
# absolute imports
INCLUDE_LIBS := $(LIBS)
INCLUDE_DIRS := $(LIB_DIR) $(GRM_GEN_DIR)
INCS := $(patsubst %,-I%,$(sort $(INCLUDE_DIRS))) $(patsubst %,-l%,$(sort $(INCLUDE_LIBS)))

# executable files
SRCS.BIN := $(filter-out $(SRCS.LIB),$(call rwildcard,$(BIN_DIR),*.c))
HDRS.BIN := $(filter-out $(HDRS.LIB),$(call rwildcard,$(BIN_DIR), *.h))
TGTS.BIN := $(patsubst $(BIN_DIR)/%.c,$(TARGET_DIR)/%,$(SRCS.BIN))

# test files
SRCS.TST := $(call rwildcard,$(TST_DIR),*.c)
TGTS.TST := $(patsubst $(TST_DIR)/%.c,$(TARGET_TST_DIR)/%, $(SRCS.TST))

# directories with trailing slash to be created
DIRS := $(sort $(dir $(OBJS)) $(dir $(OBJS.BIN)) $(dir $(TGTS.BIN)) $(dir $(TGTS.TST)))


# prevent deletion
.PRECIOUS: $(HDRS) $(SRCS) $(OBJS) $(TKNS.GRM_REC)

.DEFAULT_GOAL := build
.PNONY := grammar build dot/pdf run run/ast build/test test format index clean help echo


# create directories
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

# generate grammar files and objects for recognizers (parser and lexer)
$(GRM_GEN_DIR)/%Lexer.c $(GRM_GEN_DIR)/%Parser.c $(GRM_GEN_DIR)/%Lexer.h $(GRM_GEN_DIR)/%Parser.h $(GRM_GEN_DIR)/%.tokens: $(GRM_REC_DIR)/%.g3 | $(DIRS)
	$(ANTLR) $^ -fo $(GRM_GEN_DIR)

# generate grammar files and objects for trees
$(GRM_GEN_DIR)/%.c $(GRM_GEN_DIR)/%.h $(GRM_GEN_DIR)/%.tokens: $(GRM_DIR)/%.g3 | $(DIRS) $(TKNS.GRM_REC)
	$(ANTLR) $^ -fo $(GRM_GEN_DIR)

# compile generated files
%.o: %.c $(HDRS) | $(DIRS)
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
grammar: $(SRCS.GRM) $(HDRS.GRM)

### Build executable
build: $(TGTS.BIN)

### Generate dot files and pdf files from them
dot/pdf:
	$(ANTLR) -dfa -nfa -fo $(GRM_GEN_DIR) $(GRM_RECS)
	for file in $(GRM_GEN_DIR)/*.dot; do dot -Tpdf "$$file" -o "$${file%.dot}.pdf"; done

### Run main executable
run: build
	./build/main io/input.txt

### Run main executable and generate dot
run/ast: build
	./build/main io/input.txt --ignore-errors || true
	dot -Tpdf io/input.txt.ast.dot -o io/input.txt.ast.pdf

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
	@echo GRM_DIR=$(GRM_DIR)
	@echo GRM_REC_DIR=$(GRM_REC_DIR)
	@echo INC_DIR=$(INC_DIR)
	@echo TST_DIR=$(TST_DIR)
	@echo
	@echo BUILD_DIR=$(BUILD_DIR)
	@echo BUILD_LIB_DIR=$(BUILD_LIB_DIR)
	@echo GRM_GEN_DIR=$(GRM_GEN_DIR)
	@echo OBJ_DIR=$(OBJ_DIR)
	@echo
	@echo TARGET_DIR=$(TARGET_DIR)
	@echo TARGET_TST_DIR=$(TARGET_TST_DIR)
