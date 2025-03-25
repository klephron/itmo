#pragma once

#include "util/container_util.h"
#include "util/hashset.h"
#include "util/strbuf.h"

#include <bfd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/user.h>

// breakpoints
typedef struct breakpoint_struct {
  size_t   id;
  uint8_t  data;
  uint64_t address;
  uint64_t file_id;
  size_t   line;
  int      is_enabled;
  int      is_placed;
} breakpoint;

breakpoint *breakpoint_new(size_t id, uint8_t data, uint64_t address,
                           uint64_t file_id, size_t line);
void        breakpoint_free(breakpoint *self);

static inline int container_cmp_breakpoint(const void *_lsv, const void *_rsv) {
  const breakpoint *lsv = _lsv;
  const breakpoint *rsv = _rsv;
  int res = container_cmp_uint64((void *)lsv->address, (void *)rsv->address);
  return res;
}
static inline void container_delete_breakpoint(void *_lsv) {
  return breakpoint_free(_lsv);
}
static inline uint64_t container_hash_breakpoint(const void *_lsv) {
  const breakpoint *lsv = _lsv;
  uint64_t          res = container_hash_uint64((void *)lsv->address);
  return res;
}
HASHSET_DECLARE_STATIC_INLINE(hashset_breakpoint, breakpoint,
                              container_cmp_breakpoint, container_new_move,
                              container_delete_breakpoint,
                              container_hash_breakpoint);

// debug
typedef struct debug_info_arg_struct {
  int64_t  rbp_offset;
  uint64_t strs_index;
} debug_arg;

typedef struct debug_var_struct {
  int64_t  rbp_offset;
  uint64_t strs_index;
} debug_var;

typedef struct debug_sub_struct {
  uint64_t   address_start;
  uint64_t   address_end;
  uint64_t   address_prologue_end;
  uint64_t   address_epilogue_start;
  uint64_t   strs_index;
  uint64_t   args_cnt;
  debug_arg *args;
  uint64_t   vars_cnt;
  debug_var *vars;
} debug_sub;

typedef struct debug_file_struct {
  uint64_t strs_index;
  uint64_t lines_cnt;
  char   **lines;
} debug_file;

typedef struct debug_line_struct {
  uint64_t address;
  uint64_t file_id;
  uint64_t line;
} debug_line;

typedef struct debug_data_struct {
  // info
  uint64_t     subroutines_cnt;
  debug_sub  **subroutines;
  uint64_t     files_cnt;
  debug_file **files;
  // line
  uint64_t    lines_cnt;
  debug_line *lines;
  // str
  uint64_t strs_cnt;
  char   **strs;
} debug_data;

// state
typedef enum debugger_state_enum {
  DEBUGGER_UNKNOWN,
  DEBUGGER_IDLE, // waiting for user input
  DEBUGGER_PROCESSING,
  DEBUGGER_ERROR,
  DEBUGGER_EXITING,
} debugger_state_enum;

typedef enum target_state_enum {
  TARGET_UNKNOWN,
  TARGET_NO_TARGET,
  TARGET_NOT_RUNNING,
  TARGET_BRKPT,
  TARGET_TRAPPED,
  TARGET_ERROR, // only returned by functions, can't be actual state
} target_state_enum;

// options
typedef struct options_struct {
  bool source_lang;
} options;

// context
typedef struct context_struct {
  debugger_state_enum s_debugger;
  target_state_enum   s_target;
  strbuf             *buf_out;
  strbuf             *buf_err;
  // internal
  char               *fname;
  bfd                *abfd;
  asymbol           **abfd_asymbols;
  int64_t             abfd_asymbols_count;
  pid_t               pid;
  hashset_breakpoint *breaks;
  size_t              breaks_cnt;
  debug_data          debug;
  options             options;
  debug_line          line; // current line
} context;

context *context_new();
void     context_free(context *self);

void context_out_append(context *self, const char *data);
void context_out_appendln(context *self, const char *data);
void context_err_append(context *self, const char *data);
void context_err_appendln(context *self, const char *data);

void context_error(context *ctx, const char *msg);

int               context_tg_is_running(context *self);
target_state_enum context_tg_get_state(context *self);
target_state_enum context_tg_set(context *self, uint64_t abfd_asymbols_count,
                                 asymbol **abfd_asymbols, bfd *abfd,
                                 char *fname);
target_state_enum context_tg_wait(context *self);
target_state_enum context_tg_kill(context *self, int signal);
target_state_enum context_tg_continue(context *self);
target_state_enum context_tg_stepi(context *self);
target_state_enum context_tg_peekdata(context *self, uint64_t address,
                                      uint64_t *data_p);
target_state_enum context_tg_pokedata(context *self, uint64_t address,
                                      uint64_t data);
target_state_enum context_tg_run(context *self, char **argv);
target_state_enum context_tg_getregs(context                 *self,
                                     struct user_regs_struct *regs);
target_state_enum context_tg_setregs(context                 *self,
                                     struct user_regs_struct *regs);

int context_bp_place(context *self, breakpoint *bp);
int context_bp_remove(context *self, breakpoint *bp);

void        context_dbg_init(context *self);
void        context_dbg_deinit(context *self);
int         context_dbg_line_by_source(context *self, debug_line *result);
int         context_dbg_line_by_address(context *self, debug_line *result);
const char *context_dbg_file_by_id(context *self, uint64_t file);
const char *context_dbg_str_by_id(context *self, uint64_t id);
debug_sub  *context_dbg_sub_by_address(context *self, uint64_t address);

void context_dbg_line_reset(context *self);
void context_dbg_line_set(context *self, debug_line line);
int  context_dbg_line_changed(context *self, debug_line line);

#define peekdata_brkpt_with_int3(data) ((data & ~0xFF) | 0xCC)
#define peekdata_bpkpt_with_stored(data, stored) ((data & ~0xFF) | stored)
#define peekdata_brkpt_to_store(data) (data & 0xFF)
