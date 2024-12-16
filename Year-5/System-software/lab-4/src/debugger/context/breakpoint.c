#include "context.h"
#include "util/log.h"
#include "util/macro.h"
#include <errno.h>
#include <sys/ptrace.h>

breakpoint *breakpoint_new(size_t id, uint8_t data, uint64_t address,
                           char *file, size_t line) {
  breakpoint *self = MALLOC(breakpoint);
  self->id = id;
  self->data = data;
  self->address = address;
  self->file = file;
  self->line = line;
  self->is_enabled = 0;
  self->is_placed = 0;
  return self;
}

void breakpoint_free(breakpoint *self) {
  if (self) {
    if (self->file) {
      free(self->file);
    }
    free(self);
  }
}

int context_bp_place(context *self, breakpoint *bp) {
  if (bp->is_enabled && !bp->is_placed) {
    errno = 0;
    uint64_t data = ptrace(PTRACE_PEEKDATA, self->pid, bp->address, NULL);
    if (errno) {
      error("errno!");
      return -1;
    }
    bp->data = peekdata_brkpt_to_store(data);
    data = peekdata_brkpt_with_int3(data);
    if (ptrace(PTRACE_POKEDATA, self->pid, bp->address, data) == -1) {
      error("ptrace!");
      return -1;
    }
    bp->is_placed = 1;
  }
  return 0;
}

int context_bp_remove(context *self, breakpoint *bp) {
  if (bp->is_enabled && bp->is_placed) {
    errno = 0;
    uint64_t data = ptrace(PTRACE_PEEKDATA, self->pid, bp->address, NULL);
    if (errno) {
      error("errno!");
      return -1;
    }
    data = peekdata_bpkpt_with_stored(data, bp->data);
    if (ptrace(PTRACE_POKEDATA, self->pid, bp->address, data) == -1) {
      error("ptrace!");
      return -1;
    }
    bp->is_placed = 0;
  }
  return 0;
}
