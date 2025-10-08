FORMAT       ?= clang-format --verbose -i
# FORMAT       ?= clang-format -i

CC := clang
CC_FLAGS := -std=c99 -Wall -pedantic -Wno-strict-prototypes
CC_FLAGS_DEBUG := -g -fsanitize=address,undefined,leak

SRCS_APP := bank_robbery.c main.c
HDRS_APP :=

SRCS_LIB := argparse.c
HDRS_LIB := argparse.h

PROG := a.out
SRCS := $(SRCS_LIB) $(SRCS_APP)
HDRS := $(HDRS_LIB) $(HDRS_APP)
LOGS := $(wildcard *.log)

# If the first argument is "run"...
ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  # with duplicates it will show 'up to date'
  $(eval $(sort $(RUN_ARGS)):;@:)
	# replaces 'up to date' -> 'nothing to be done'
  .PHONY: $(sort $(RUN_ARGS))
endif


.PHONY: build run clean format


$(PROG): $(SRCS)
	$(CC) $(CC_FLAGS) $(CC_FLAGS_DEBUG) $^ -o $@

build: $(PROG)

run: $(PROG)
	./$< $(RUN_ARGS)

clean:
	rm -f $(PROG) $(LOGS)

format:
	@$(FORMAT) $(SRCS_APP) $(HDRS_APP)

