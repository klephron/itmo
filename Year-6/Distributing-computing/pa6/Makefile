ARCH   ?= 64
FORMAT ?= clang-format --verbose -i
# FORMAT ?= clang-format -i

CC := clang
CC_FLAGS := -std=c99 -Wall -pedantic
CC_FLAGS_DEBUG := -g -fsanitize=address,undefined,leak

SRCS_APP := main.c
HDRS_APP :=

SRCS_LIB := argparse.c
HDRS_LIB := argparse.h
PATH_LIB := $(CURDIR)/lib$(ARCH)

PROG := a.out
LIBS := -lruntime
SRCS := $(SRCS_LIB) $(SRCS_APP)
HDRS := $(HDRS_LIB) $(HDRS_APP)
LOGS := $(wildcard *.log)

# If the first argument is "run"...
ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif


.PHONY: build run clean format


$(PROG): $(SRCS)
	$(CC) $(CC_FLAGS) $(CC_FLAGS_DEBUG) $^ -L$(PATH_LIB) $(LIBS) -o $@

build: $(PROG)

run: $(PROG)
	LD_LIBRARY_PATH=$(PATH_LIB) ./$< $(RUN_ARGS)

clean:
	rm -f $(PROG) $(LOGS)

format:
	@$(FORMAT) $(SRCS_APP) $(HDRS_APP)

