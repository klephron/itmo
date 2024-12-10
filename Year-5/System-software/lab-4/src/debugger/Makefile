rwildcard=$(foreach d,$(wildcard $(1:=/*)),$(call rwildcard,$d,$2) $(filter $(subst *,%,$2),$d))

MKDIR       := $(MKDIR)
CP          := $(CP)
CC          := $(CC)
FLAGS       := $(FLAGS)
LIBS        := $(LIBS)
INCS        := $(INCS)
AR          := $(AR)
BUILD_DIR   := $(BUILD_DIR)


LIB_DIR  := $(CURDIR)/lib


SRCS.LIB := $(call rwildcard,$(LIB_DIR), *.c)
HDRS.LIB := $(call rwildcard,$(LIB_DIR), *.h)
OBJS.LIB := $(patsubst $(CURDIR)/%.c, $(BUILD_DIR)/%.o, $(SRCS.LIB))

SRCS.OUT := $(filter-out $(SRCS.LIB),$(call rwildcard,$(CURDIR),*.c))
HDRS.OUT := $(filter-out $(HDRS.LIB),$(call rwildcard,$(CURDIR), *.h))
TGTS.OUT := $(patsubst $(CURDIR)/%.c, $(BUILD_DIR)/%, $(SRCS.OUT))

SRCS := $(SRCS.OUT) $(SRCS.LIB)
HDRS := $(HDRS.OUT) $(HDRS.LIB)
OBJS := $(OBJS.LIB)
TGTS := $(TGTS.OUT)

DIRS := $(sort $(dir $(OBJS)) $(dir $(TGTS)))
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

.PRECIOUS: $(SRCS) $(HDRS) $(OBJS) $(BUILD_DIR)/libdebugger.a


# object files
$(BUILD_DIR)/%.o: %.c $(HDRS.LIB) | $(DIRS)
	$(CC) $(FLAGS) -c $< -o $@ $(INCS)

# libraries
$(BUILD_DIR)/libdebugger.a: $(OBJS) $(LIBS)
	$(AR) rcs $@ $^

# targets
$(BUILD_DIR)/%: $(CURDIR)/%.c $(OBJS) $(LIBS) | $(DIRS)
	$(CC) $(FLAGS) -o $@ $^ $(INCS) $(LIBS)


build/%:
	@$(MAKE) --no-print-directory $(patsubst build/%,$(BUILD_DIR)/%,$@)
