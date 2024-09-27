CC := clang++
FLAGS := -g3 -Wall
MKDIR := mkdir -p
BUILD_DIR := build

SRCS = $(basename $(wildcard *.cpp))
TGTS = $(patsubst %.cpp, %, $(SRCS))

DIRS = $(sort $(BUILD_DIR))


default: run/task-0

all: $(patsubst %, $(BUILD_DIR)/%, $(TGTS))


define make-target-rule

$$(BUILD_DIR)/$(1): $(1).cpp | $$(DIRS)
	$$(CC) $$(FLAGS) $$< -o $$@

# build/$(1): $$(BUILD_DIR)/$(1)

run/$(1): $$(BUILD_DIR)/$(1)
	./$$(BUILD_DIR)/$(1)

endef

$(foreach target, $(TGTS), $(eval $(call make-target-rule,$(target))))


$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

# Map % rule to run/%
%: run/%
	@true

clean:
	rm -rf $(BUILD_DIR)

### Help
help:
	@echo "Global targets: default all clean"
	@echo "Targets: $(TGTS)"
	@echo "Tasks for targets: % (run/%), build/%"

