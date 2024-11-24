AS=as
AS_FLAGS=--64 -g
LD=ld
LD_FLAGS=-g -z noexecstack

BUILD_DIR=build

TGTS.LIBC=hello_world
TGTS.ASM=exit max
TGTS.MAIN=hello_world


all: run

run: link
	./$(BUILD_DIR)/$(TGTS.MAIN)

link: $(BUILD_DIR)/$(TGTS.MAIN)

build: $(BUILD_DIR)/$(TGTS.MAIN).o

clean:
	rm -rf $(BUILD_DIR)

dir:
	mkdir -p $(BUILD_DIR)


define make-objects

$$(BUILD_DIR)/$(1).o: $(1).s | dir
	$$(AS) $$(AS_FLAGS) -o $$@ $$<

$$(BUILD_DIR)/$(1): $$(BUILD_DIR)/$(1).o | dir
	$$(LD) $$(LD_FLAGS) -o $$@ $$<

endef

define make-objects-libc

$$(BUILD_DIR)/$(1).o: $(1).s | dir
	$$(AS) $$(AS_FLAGS) $$< -o $$@

$$(BUILD_DIR)/$(1): $$(BUILD_DIR)/$(1).o | dir
	$$(LD) $$(LD_FLAGS) -o $$@ \
		-dynamic-linker /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 \
		/usr/lib/x86_64-linux-gnu/crt1.o \
		/usr/lib/x86_64-linux-gnu/crti.o \
		-lc \
		$$< \
		/usr/lib/x86_64-linux-gnu/crtn.o

endef


$(foreach target, $(TGTS.LIBC), $(eval $(call make-objects-libc,$(target))))
$(foreach target, $(TGTS.ASM), $(eval $(call make-objects,$(target))))
