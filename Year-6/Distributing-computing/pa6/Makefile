ARCH 		 		 ?= 64
FORMAT       ?= clang-format --verbose -i
# FORMAT       ?= clang-format-3.6 -i

LIBRARY_PATH := $(CURDIR)/lib$(ARCH)

CC := clang
CC_FLAGS := -std=c99 -Wall -pedantic
CC_FLAGS_DEBUG := -g -fsanitize=address,undefined,leak

PROG := a.out
LIBS := -lruntime
SRCS := $(wildcard *.c)
HDRS := $(wildcard *.h)
LOGS := $(wildcard *.log)


# If the first argument is "run"...
ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif


.PHONY: build run clean


$(PROG): $(SRCS)
	$(CC) $(CC_FLAGS) $(CC_FLAGS_DEBUG) $^ -L$(LIBRARY_PATH) $(LIBS) -o $@

build: $(PROG)

run: $(PROG)
	LD_LIBRARY_PATH=$(LIBRARY_PATH) $(PWD)/$< $(RUN_ARGS)

clean:
	rm -f $(PROG) $(LOGS)

format:
	@$(FORMAT) $(SRCS) $(HDRS)

