rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

CC := gcc
LD := gcc
# -fsanitize=thread
FLAGS := -g3 -O0 -m64 -fsanitize=address,undefined,leak -Wall -Wextra
MKDIR := mkdir -p
# ANTLR := java -jar lib/antlr-3.5.3-complete.jar
# ANTLR := java -jar ~/.m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar
ANTLR := java -jar /usr/local/maven/m2/repository/org/antlr/antlr-complete/3.5.3/antlr-complete-3.5.3.jar

SRC_DIR := src
GRM_DIR := $(SRC_DIR)/grammar
BUILD_DIR := build
GRM_GEN_DIR := $(BUILD_DIR)/_gen
OBJ_DIR := $(BUILD_DIR)
TARGET := $(BUILD_DIR)/main

GRMS = $(call rwildcard,$(GRM_DIR), *.g3)
SRCS.GRM = $(patsubst $(GRM_DIR)/%.g3,$(GRM_GEN_DIR)/%Lexer.c, $(GRMS))
SRCS.GRM +=$(patsubst $(GRM_DIR)/%.g3,$(GRM_GEN_DIR)/%Parser.c, $(GRMS))
HDRS.GRM = $(patsubst %.c,%.h,$(SRCS.GRM))
OBJS.GRM = $(patsubst %.c,%.o,$(SRCS.GRM))

SRCS.SRC = $(call rwildcard,$(SRC_DIR),*.c)
HDRS.SRC = $(call rwildcard,$(SRC_DIR), *.h)
OBJS.SRC = $(patsubst %.c, $(OBJ_DIR)/%.o, $(patsubst $(SRC_DIR)/%, %, $(SRCS.SRC)))

SRCS = $(SRCS.SRC) $(SRCS.GRM)
HDRS = $(HDRS.SRC) $(HDRS.GRM)
OBJS = $(OBJS.SRC) $(OBJS.GRM)

# absolute imports
INCLUDE_DIRS := $(SRC_DIR) $(GRM_GEN_DIR)
INCLUDE_LIBS = antlr3c

INCS = $(patsubst %,-I%,$(sort $(INCLUDE_DIRS))) $(patsubst %,-l%,$(sort $(INCLUDE_LIBS)))

# with trailing slash
DIRS = $(sort $(dir $(OBJS)) $(dir $(TARGET))) 

# generate grammar files and objects
$(GRM_GEN_DIR)/%Lexer.c $(GRM_GEN_DIR)/%Parser.c $(GRM_GEN_DIR)/%Lexer.h $(GRM_GEN_DIR)/%Parser.h: $(GRM_DIR)/%.g3
	$(ANTLR) -fo $(GRM_GEN_DIR) $^ 

%.o: %.c $(HDRS) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

# generate object files excluding generated
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c $(HDRS) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

# create directories
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

# create target
$(TARGET): $(OBJS)
	$(LD) $(FLAGS) $^ -o $(TARGET) $(INCS)

### Generate antlr3 source and header files from grammar rules
grammar: $(SRCS.GRM) $(HDRS.GRM)
	
### Build executable
build: $(TARGET)

### Run executable
run: build
	./$(TARGET)

### Generate dot files and pdf files from them
dot/pdf:
	$(ANTLR) -dfa -fo $(GRM_GEN_DIR) $(GRMS)
	for file in $(GRM_GEN_DIR)/*.dot; do dot -Tpdf "$$file" -o "$${file%.dot}.pdf"; done

### Remove build and target files
clean:
	if [ -e $(TARGET) ] ; then rm $(TARGET); fi
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
