AS=as
AS_FLAGS=--64 -g
LD=ld
LD_FLAGS=-g -z noexecstack
CC=gcc
CC_FLAGS=-m64 -g

BUILD_DIR=build

TGTS.CLIBS=example example2
TGTS.LIBC=hello_world malloc
TGTS.ASM=exit max

TGTS.MAIN=malloc


all: run

run: link
	./$(BUILD_DIR)/$(TGTS.MAIN)

link: $(BUILD_DIR)/$(TGTS.MAIN)

build: $(BUILD_DIR)/$(TGTS.MAIN).o

clean:
	rm -rf $(BUILD_DIR)

dir:
	mkdir -p $(BUILD_DIR)

objdump: $(BUILD_DIR)/$(TGTS.MAIN).o
	objdump -Mx86-64,att,amd64 -tr $<
	objdump -Mx86-64,att,amd64 -S $<


define make-clibs

$$(BUILD_DIR)/$(1).o: $(1).c | dir
	$$(CC) $$(CC_FLAGS) -c -o $$@ $$<

endef

define make-objects

$$(BUILD_DIR)/$(1).o: $(1).s | dir
	$$(AS) $$(AS_FLAGS) -o $$@ $$<

$$(BUILD_DIR)/$(1): $$(BUILD_DIR)/$(1).o | dir
	$$(LD) $$(LD_FLAGS) -o $$@ $$<

endef

define make-objects-libc

$$(BUILD_DIR)/$(1).o: $(1).s | dir
	$$(AS) $$(AS_FLAGS) $$< -o $$@

$$(BUILD_DIR)/$(1): $$(BUILD_DIR)/$(1).o $$(patsubst %,$$(BUILD_DIR)/%.o,$$(TGTS.CLIBS)) | dir
	$$(LD) $$(LD_FLAGS) -o $$@ \
		-dynamic-linker /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 \
		/usr/lib/x86_64-linux-gnu/crt1.o \
		/usr/lib/x86_64-linux-gnu/crti.o \
		-lc \
		$$^ \
		/usr/lib/x86_64-linux-gnu/crtn.o

endef


$(foreach target, $(TGTS.LIBC), $(eval $(call make-objects-libc,$(target))))
$(foreach target, $(TGTS.ASM), $(eval $(call make-objects,$(target))))
$(foreach target, $(TGTS.CLIBS), $(eval $(call make-clibs,$(target))))
