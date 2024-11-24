AS=as
LD=ld

BUILD_DIR=build
TGTS=max
AS_FLAGS=--64 -g

all: run

run: link
	./$(BUILD_DIR)/$(TGTS)

build: $(patsubst %,$(BUILD_DIR)/%.o, $(TGTS))

link: $(patsubst %,$(BUILD_DIR)/%, $(TGTS))

clean:
	find $(BUILD_DIR) -type f -name "*.o" -delete

define make-objects

$$(BUILD_DIR)/$(1).o: $(1).s
	$$(AS) $$(AS_FLAGS) $$< -o $$@

$$(BUILD_DIR)/$(1): $$(BUILD_DIR)/$(1).o
	$$(LD) $$< -o $$@

endef


$(foreach target, $(TGTS), $(eval $(call make-objects,$(target))))
