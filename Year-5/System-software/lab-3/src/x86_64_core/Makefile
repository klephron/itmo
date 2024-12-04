rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR     := $(MKDIR)
CC        := $(CC)
FLAGS     := $(FLAGS)
INCS      := $(INCS)
BUILD_DIR := $(BUILD_DIR)
AR        := $(AR)

CORE.SRCS := x86_64_core.c
CORE.HDRS := x86_64_core.h
CORE.OBJS := $(patsubst %.c, $(BUILD_DIR)/%.o, $(CORE.SRCS))

ALL.SRCS := $(call rwildcard,$(CURDIR),*.c)
ALL.HDRS := $(call rwildcard,$(CURDIR), *.h)
ALL.OBJS := $(patsubst $(CURDIR)/%.c, $(BUILD_DIR)/%.o, $(ALL.SRCS))

DIRS := $(sort $(dir $(OBJS)))
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)


$(BUILD_DIR)/%.o: %.c $(HDRS) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

$(BUILD_DIR)/libx86_64_core.a: $(CORE.OBJS)
	$(AR) rcs $@ $^

$(BUILD_DIR)/libx86_64_all.a: $(ALL.OBJS)
	$(AR) rcs $@ $^


build/%:
	@$(MAKE) --no-print-directory $(patsubst build/%,$(BUILD_DIR)/%,$@)
