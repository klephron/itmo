#include "ctx_build.h"

#include "util/macro.h"
#include <stdio.h>
#include <string.h>

static uint64_t ctx_debug_add_str(cg_x86_64_ctx *ctx, char *str) {
  list_cg_x86_64_unit *str_units = ctx->cg_ref->debug_str->units;
  uint64_t             str_id = ctx->debug_strs_cnt++;
  cg_units_push_back(str_units, cg_x86_64_data_new_ascii((uint8_t *)str));
  return str_id;
}

char *ctx_debug_sub_start(cg_x86_64_ctx *ctx, uint64_t id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "DSS%zu", id);
  return ctx_local_label(ctx, buf);
}

char *ctx_debug_sub_end(cg_x86_64_ctx *ctx, uint64_t id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "DSE%zu", id);
  return ctx_local_label(ctx, buf);
}

char *ctx_debug_sub_prologue_end(cg_x86_64_ctx *ctx, uint64_t id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "DSP%zu", id);
  return ctx_local_label(ctx, buf);
}

char *ctx_debug_sub_epilogue_start(cg_x86_64_ctx *ctx, uint64_t id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "DSL%zu", id);
  return ctx_local_label(ctx, buf);
}

char *ctx_debug_line(cg_x86_64_ctx *ctx, uint64_t id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "DL%zu", id);
  return ctx_local_label(ctx, buf);
}

// NOTE: should be executed when all subroutines are processed
void ctx_debug_info_post_push_header(cg_x86_64_ctx *ctx) {
  cg_units_push_front(ctx->cg_ref->debug_info->units,
                      cg_x86_64_data_new_quad(ctx->debug_files_cnt));
  cg_units_push_front(ctx->cg_ref->debug_info->units,
                      cg_x86_64_data_new_quad(ctx->debug_subs_cnt));
}

// NOTE: should be executed when all subroutines are processed
void ctx_debug_info_post_push_files(cg_x86_64_ctx *ctx) {
  list_cg_x86_64_unit *info_units = ctx->cg_ref->debug_info->units;
  for (hashset_cg_x86_64_debug_str_it it =
           hashset_cg_x86_64_debug_str_begin(ctx->debug_files_idx);
       !it.end(&it); it.next(&it)) {
    cg_x86_64_debug_str *debug_str = it.get(&it);
    cg_units_push_back(info_units, cg_x86_64_data_new_quad(debug_str->str_id));
  }
}

void ctx_debug_line_post_push_header(cg_x86_64_ctx *ctx) {
  cg_units_push_front(ctx->cg_ref->debug_line->units,
                      cg_x86_64_data_new_quad(ctx->debug_lines_cnt));
}

void ctx_debug_str_post_push_header(cg_x86_64_ctx *ctx) {
  cg_units_push_front(ctx->cg_ref->debug_str->units,
                      cg_x86_64_data_new_quad(ctx->debug_strs_cnt));
}

void ctx_debug_add_line(cg_x86_64_ctx *ctx, mir_debug_line *line, uint64_t id) {
  if (!line) {
    return;
  }
  uint64_t file_id;

  // get idx and insert if not existed
  hashset_cg_x86_64_debug_str_it it = hashset_cg_x86_64_debug_str_find(
      ctx->debug_files_idx,
      &(cg_x86_64_debug_str){.chars_ref = line->source_ref});

  if (it.end(&it)) {
    file_id = ctx->debug_files_cnt++;
    char    *source = strdup(line->source_ref);
    uint64_t str_id = ctx_debug_add_str(ctx, source);
    hashset_cg_x86_64_debug_str_insert(
        ctx->debug_files_idx,
        cg_x86_64_debug_str_new_file(file_id, str_id, source));
  } else {
    file_id = it.get(&it)->file_id;
  }

  // add entry: [address, file_num, line_num]
  list_cg_x86_64_unit *line_units = ctx->cg_ref->debug_line->units;
  cg_units_push_back(line_units,
                     cg_x86_64_data_new_label(ctx_debug_line(ctx, id)));
  cg_units_push_back(line_units, cg_x86_64_data_new_quad(file_id));
  cg_units_push_back(line_units, cg_x86_64_data_new_quad(line->line));
}

void ctx_debug_add_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub, uint64_t id) {
  list_cg_x86_64_unit *info_units = ctx->cg_ref->debug_info->units;

  // subroutine bounds
  cg_units_push_back(info_units,
                     cg_x86_64_data_new_label(ctx_debug_sub_start(ctx, id)));
  cg_units_push_back(info_units,
                     cg_x86_64_data_new_label(ctx_debug_sub_end(ctx, id)));
  cg_units_push_back(info_units, cg_x86_64_data_new_label(
                                     ctx_debug_sub_prologue_end(ctx, id)));
  cg_units_push_back(info_units, cg_x86_64_data_new_label(
                                     ctx_debug_sub_epilogue_start(ctx, id)));

  // add name
  uint64_t str_id = ctx_debug_add_str(ctx, strdup(sub->name));
  cg_units_push_back(info_units, cg_x86_64_data_new_quad(str_id));

  // add args + vars cnt
  uint64_t args_cnt = list_mir_value_size(sub->args);
  cg_units_push_back(info_units, cg_x86_64_data_new_quad(args_cnt));
  uint64_t vars_cnt = list_mir_value_size(sub->vars);
  cg_units_push_back(info_units, cg_x86_64_data_new_quad(vars_cnt));

  // fill args array
  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    mir_value           *value = it.get(&it);
    cg_x86_64_map_value *cg = ctx_sub_value_find(ctx, value);
    // rbp_offset
    cg_units_push_back(info_units, cg_x86_64_data_new_quad(cg->rbp_offset));
    // str_index
    uint64_t str_id =
        ctx_debug_add_str(ctx, strdup(value->debug_symbol->binding_ref->name));
    cg_units_push_back(info_units, cg_x86_64_data_new_quad(str_id));
  }

  // fill vars array
  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    mir_value           *value = it.get(&it);
    cg_x86_64_map_value *cg = ctx_sub_value_find(ctx, value);
    // rbp_offset
    cg_units_push_back(info_units, cg_x86_64_data_new_quad(cg->rbp_offset));
    // str_index
    uint64_t str_id =
        ctx_debug_add_str(ctx, strdup(value->debug_symbol->binding_ref->name));
    cg_units_push_back(info_units, cg_x86_64_data_new_quad(str_id));
  }
}
