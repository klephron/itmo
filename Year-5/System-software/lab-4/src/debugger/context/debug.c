#include "context.h"

#include "util/macro.h"
#include "x86_64_core/debug.h"

void context_dbg_init(context *self) {
  if (!self->abfd) {
    return;
  }

  debug_data *dbg = &self->debug;

  for (asection *section = self->abfd->sections; section;
       section = section->next) {
    const char *name = bfd_section_name(section);

    if (!strcmp(name, ".u_debug_info")) {
      bfd_size_type size = bfd_section_size(section);
      uint8_t      *bytes = malloc(size);
      uint8_t      *cur = bytes;
      bfd_get_section_contents(self->abfd, section, bytes, 0, size);

      dbg->subroutines_cnt = ((x86_64_debug_info *)cur)->subroutines_cnt;
      dbg->files_cnt = ((x86_64_debug_info *)cur)->files_cnt;
      cur += sizeof(x86_64_debug_info);

      dbg->subroutines = MALLOCN(debug_info_sub *, dbg->subroutines_cnt);
      for (uint64_t i = 0; i < dbg->subroutines_cnt; ++i) {
        dbg->subroutines[i] = MALLOC(debug_info_sub);
        debug_info_sub *sub = dbg->subroutines[i];

        sub->address_start = ((x86_64_debug_info_sub *)cur)->address_start;
        sub->address_end = ((x86_64_debug_info_sub *)cur)->address_end;
        sub->address_prologue_end =
            ((x86_64_debug_info_sub *)cur)->address_prologue_end;
        sub->address_epilogue_start =
            ((x86_64_debug_info_sub *)cur)->address_epilogue_start;

        sub->strs_index = ((x86_64_debug_info_sub *)cur)->strs_index;
        sub->args_cnt = ((x86_64_debug_info_sub *)cur)->args_cnt;
        sub->vars_cnt = ((x86_64_debug_info_sub *)cur)->vars_cnt;
        cur += sizeof(x86_64_debug_info_sub);

        sub->args = MALLOCN(debug_info_arg, sub->args_cnt);
        for (uint64_t j = 0; j < sub->args_cnt; ++j) {
          debug_info_arg *arg = sub->args + j;
          arg->rbp_offset = ((x86_64_debug_info_arg *)cur)->rbp_offset;
          arg->strs_index = ((x86_64_debug_info_arg *)cur)->strs_index;
          cur += sizeof(x86_64_debug_info_arg);
        }

        sub->vars = MALLOCN(debug_info_var, sub->vars_cnt);
        for (uint64_t j = 0; j < sub->vars_cnt; ++j) {
          debug_info_var *var = sub->vars + j;
          var->rbp_offset = ((x86_64_debug_info_var *)cur)->rbp_offset;
          var->strs_index = ((x86_64_debug_info_var *)cur)->strs_index;
          cur += sizeof(x86_64_debug_info_var);
        }
      }

      dbg->files = MALLOCN(debug_info_file, dbg->files_cnt);
      for (uint64_t i = 0; i < dbg->files_cnt; ++i) {
        debug_info_file *file = dbg->files + i;
        file->strs_index = ((x86_64_debug_info_file *)cur)->strs_index;
        cur += sizeof(x86_64_debug_info_file);
      }

      free(bytes);
    } else if (!strcmp(name, ".u_debug_line")) {
      bfd_size_type size = bfd_section_size(section);
      uint8_t      *bytes = malloc(size);
      uint8_t      *cur = bytes;
      bfd_get_section_contents(self->abfd, section, bytes, 0, size);

      dbg->lines_cnt = ((x86_64_debug_line *)cur)->entries_cnt;
      cur += sizeof(x86_64_debug_line);

      dbg->lines = MALLOCN(debug_line, dbg->lines_cnt);
      for (uint64_t i = 0; i < dbg->lines_cnt; ++i) {
        debug_line *line = dbg->lines + i;
        line->line = ((x86_64_debug_line_entry *)cur)->line;
        line->file_id = ((x86_64_debug_line_entry *)cur)->file_id;
        line->address = ((x86_64_debug_line_entry *)cur)->address;
        cur += sizeof(x86_64_debug_line_entry);
      }

      free(bytes);
    } else if (!strcmp(name, ".u_debug_str")) {
      bfd_size_type size = bfd_section_size(section);
      uint8_t      *bytes = malloc(size);
      uint8_t      *cur = bytes;
      bfd_get_section_contents(self->abfd, section, bytes, 0, size);

      dbg->strs_cnt = ((x86_64_debug_str *)cur)->strs_cnt;
      cur += sizeof(x86_64_debug_str);

      dbg->strs = MALLOCN(char *, dbg->strs_cnt);
      for (uint64_t i = 0; i < dbg->strs_cnt; ++i) {
        uint64_t l = strlen((const char *)cur) + 1;
        dbg->strs[i] = strdup((const char *)cur);
        cur += sizeof(char) * l;
      }
      free(bytes);
    }
  }
}

void context_dbg_deinit(context *self) {
  debug_data *dbg = &self->debug;

  for (uint64_t i = 0; i < dbg->subroutines_cnt; ++i) {
    debug_info_sub *sub = dbg->subroutines[i];
    free(sub->args);
    free(sub->vars);
    free(sub);
  }
  free(dbg->subroutines);
  dbg->subroutines = NULL;

  free(dbg->files);
  dbg->files = NULL;

  free(dbg->lines);
  dbg->lines = NULL;

  for (uint64_t i = 0; i < dbg->strs_cnt; ++i) {
    free(dbg->strs[i]);
  }
  free(dbg->strs);
  dbg->strs = NULL;
}
