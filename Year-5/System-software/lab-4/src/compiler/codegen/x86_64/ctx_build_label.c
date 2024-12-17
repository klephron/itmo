#include "ctx_build.h"

#include "util/macro.h"
#include <stdio.h>
#include <string.h>

char *ctx_local_label(cg_x86_64_ctx *ctx, const char *old) {
  UNUSED(ctx);
  size_t len = strlen(old);
  char  *name = malloc(len + 3);
  memcpy(name, ".L", 2);
  memcpy(name + 2, old, len + 1);
  return name;
}

char *ctx_block_label(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_bb *bb) {
  size_t bb_sz = strlen(sub->name) + 1 + 32;
  char  *bb_name = malloc(bb_sz);
  snprintf(bb_name, bb_sz, "%s_%zu", sub->name, bb->id);
  char *local = ctx_local_label(ctx, bb_name);
  free(bb_name);
  return local;
}

char *ctx_data_label(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                     mir_value *value) {
  char buf[32];

  snprintf(buf, STRMAXLEN(buf), "_data_%lu", value->id);
  size_t d_sz = strlen(sub->name) + strlen(buf) + 1;
  char  *name = malloc(d_sz);
  {
    char *end = name;
    end = stpcpy(end, sub->name);
    end = stpcpy(end, buf);
  }
  char *local = ctx_local_label(ctx, name);
  free(name);
  return local;
}

char *ctx_sub_label(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  if (ctx_in_globals(ctx, (mir_node_base *)sub)) {
    return strdup(sub->name);
  } else {
    return ctx_local_label(ctx, sub->name);
  }
}

char *ctx_ext_label(cg_x86_64_ctx *ctx, mir_subroutine_ext *ext) {
  if (ctx_in_globals(ctx, (mir_node_base *)ext)) {
    return strdup(ext->name);
  } else {
    return ctx_local_label(ctx, ext->name);
  }
}

char *ctx_sub_epilogue_label(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  const char epilogue[] = "_ep";

  size_t ssz = strlen(sub->name);
  size_t lsz = ssz + sizeof(epilogue);
  char  *label = malloc(lsz);
  memcpy(label, sub->name, ssz);
  memcpy(label + ssz, epilogue, sizeof(epilogue));
  char *local = ctx_local_label(ctx, label);
  free(label);
  return local;
}
