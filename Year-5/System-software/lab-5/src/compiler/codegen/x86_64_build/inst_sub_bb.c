#include "compiler/mir/str.h"
#include "inst_sub.h"
#include "util/log.h"
#include "util/macro.h"
#include "x86_64_core/value.h"
#include <stdarg.h>
#include <string.h>

static void cg_inst_frame_restore(cg_ctx *ctx, uint64_t frame_size_old) {
  if (frame_size_old != ctx->frame_size) {
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_ADDQ,
        cg_x86_64_op_new_immediate(frame_size_old - ctx->frame_size),
        cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
    ctx->frame_size = frame_size_old;
  }
}

// updates frame size
static void cg_inst_call_prep_values(cg_ctx *ctx, uint64_t values_cnt,
                                     const mir_value *const *values) {

  // calculate memory that will be used for arguments and add alignment
  uint64_t frame_size_new = ctx->frame_size;

  if (values_cnt > 6) {
    frame_size_new = ctx->frame_size + CG_X86_64_SIZE_QUAD * (values_cnt - 6);

    uint64_t alignment = cg_aligned(frame_size_new) - (frame_size_new);
    if (alignment) {
      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_SUBQ, cg_x86_64_op_new_immediate(alignment),
          cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
    }
  }

  // will overflow to uint64_max, prepares args from last to first
  for (uint64_t i = values_cnt - 1; i != UINT64_MAX; --i) {
    const mir_value     *value = values[i];
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    switch (i) {
      case 0: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);
        break;
      }
      case 1: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_RSI), NULL);
        break;
      }
      case 2: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_RDX), NULL);
        break;
      }
      case 3: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_RCX), NULL);
        break;
      }
      case 4: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_R8), NULL);
        break;
      }
      case 5: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_LEAQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
            cg_x86_64_op_new_register(CG_X86_64_REG_R9), NULL);
        break;
      }
      default: {
        cg_ctx_text_emplace_back_text(
            ctx, CG_X86_64_MNEM_PUSHQ,
            cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP), NULL);
        break;
      }
    }
  }

  ctx->frame_size = frame_size_new;
}

static void cg_inst_call_op_tbl(cg_ctx *ctx, const mir_value *self,
                                uint64_t offset) {
  const cg_value_meta *self_meta = cg_ctx_value_meta_find(ctx, self);

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ,
      cg_x86_64_op_new_base(self_meta->offset + offsetof(x86_64_value, op_tbl),
                            CG_X86_64_REG_RBP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ,
      cg_x86_64_op_new_base(offset, CG_X86_64_REG_RAX),
      cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

  cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_CALL,
                                cg_x86_64_op_new_register(CG_X86_64_REG_RAX),
                                NULL);
}

static void cg_inst_stmt_op(cg_ctx *ctx, const mir_stmt *stmt) {
  uint64_t values_cnt =
      (stmt->op.ret ? 1 : 0) + list_mir_value_ref_size(stmt->op.args);

  // copy values from list to array
  const mir_value **values = MALLOCN(const mir_value *, values_cnt);
  {
    uint64_t i = 0;
    if (stmt->op.ret) {
      values[i++] = stmt->op.ret;
    }
    for (list_mir_value_ref_it it = list_mir_value_ref_begin(stmt->op.args);
         i != values_cnt; ++i, NEXT(it)) {
      values[i] = GET(it);
    }
  }

  uint64_t frame_size_old = ctx->frame_size;

  cg_inst_call_prep_values(ctx, values_cnt, values);

  const mir_value *self = list_mir_value_ref_front(stmt->op.args);
  if (!self) {
    error("expected any arg to be passed by op %s(%d) %p",
          mir_stmt_op_enum_str(stmt->op.kind), stmt->op.kind, stmt);
    goto cleanup;
  }

  switch (stmt->op.kind) {
    case MIR_STMT_OP_UNARY_PLUS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_plus));
      break;
    }
    case MIR_STMT_OP_UNARY_MINUS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_minus));
      break;
    }
    case MIR_STMT_OP_UNARY_LOGICAL_NOT: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_not));
      break;
    }
    case MIR_STMT_OP_UNARY_BITWISE_NOT: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_not));
      break;
    }
    case MIR_STMT_OP_UNARY_INC: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_inc));
      break;
    }
    case MIR_STMT_OP_UNARY_DEC: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_dec));
      break;
    }
    case MIR_STMT_OP_BINARY_LOGICAL_OR: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_or));
      break;
    }
    case MIR_STMT_OP_BINARY_LOGICAL_AND: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_and));
      break;
    }
    case MIR_STMT_OP_BINARY_BITWISE_OR: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_or));
      break;
    }
    case MIR_STMT_OP_BINARY_BITWISE_XOR: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_xor));
      break;
    }
    case MIR_STMT_OP_BINARY_BITWISE_AND: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_and));
      break;
    }
    case MIR_STMT_OP_BINARY_EQUALS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_eq));
      break;
    }
    case MIR_STMT_OP_BINARY_NOT_EQUALS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_neq));
      break;
    }
    case MIR_STMT_OP_BINARY_LESS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_less));
      break;
    }
    case MIR_STMT_OP_BINARY_LESS_EQUALS: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_less_eq));
      break;
    }
    case MIR_STMT_OP_BINARY_GREATER: {
      if (values_cnt > 3) {
        const mir_value *tmp = values[1];
        values[1]            = values[2];
        values[2]            = tmp;
        cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_less));
      } else {
        error("expected at least 3 arguments for %s(%d) to modify order %p",
              mir_stmt_op_enum_str(stmt->op.kind), stmt->op.kind, stmt);
      }
      break;
    }
    case MIR_STMT_OP_BINARY_GREATER_EQUALS: {
      if (values_cnt > 3) {
        const mir_value *tmp = values[1];
        values[1]            = values[2];
        values[2]            = tmp;
        cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_less_eq));
      } else {
        error("expected at least 3 arguments for %s(%d) to modify order %p",
              mir_stmt_op_enum_str(stmt->op.kind), stmt->op.kind, stmt);
      }
      break;
    }
    case MIR_STMT_OP_BINARY_BITWISE_SHIFT_LEFT: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_shl));
      break;
    }
    case MIR_STMT_OP_BINARY_BITWISE_SHIFT_RIGHT: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_bit_shr));
      break;
    }
    case MIR_STMT_OP_BINARY_ADD: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_add));
      break;
    }
    case MIR_STMT_OP_BINARY_SUB: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_sub));
      break;
    }
    case MIR_STMT_OP_BINARY_MUL: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_mul));
      break;
    }
    case MIR_STMT_OP_BINARY_DIV: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_div));
      break;
    }
    case MIR_STMT_OP_BINARY_REM: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_rem));
      break;
    }
    case MIR_STMT_OP_CALL: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_call));
      break;
    }
    case MIR_STMT_OP_INDEX: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_index));
      break;
    }
    case MIR_STMT_OP_INDEX_REF: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_index_ref));
      break;
    }
    case MIR_STMT_OP_DEREF: {
      cg_inst_call_op_tbl(ctx, self, offsetof(x86_64_op_tbl, op_deref));
      break;
    }
    default:
      error("unhandled op kind %s(%d) %p", mir_stmt_op_enum_str(stmt->op.kind),
            stmt->op.kind, stmt);
      break;
  }

cleanup:
  cg_inst_frame_restore(ctx, frame_size_old);

  free(values);
}

static void cg_inst_stmt_call(cg_ctx *ctx, const mir_stmt *stmt) {
  warn("unimplemented call");
}

static void cg_inst_stmt_member(cg_ctx *ctx, const mir_stmt *stmt) {
  warn("unimplemented member");
}

static void cg_inst_stmt_member_ref(cg_ctx *ctx, const mir_stmt *stmt) {
  warn("unimplemented member_ref");
}

static void cg_inst_stmt_builtin(cg_ctx *ctx, const mir_stmt *stmt) {
  warn("unimplemented builtin");
}

static void cg_inst_stmt_assign_lit(cg_ctx *ctx, const mir_stmt *stmt) {
  const mir_value *to_value = stmt->assign.to;
  const mir_lit   *from_lit = stmt->assign.from_lit;

  warn("unimplemented assign lit");
  // cg_ctx_text_emplace_back_text(
  //     ctx, CG_X86_64_MNEM_MOVQ,
  //     cg_x86_64_op_new_base(to_meta->offset + offsetof(x86_64_value, op_tbl),
  //                           CG_X86_64_REG_RBP),
  //     cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

  // cg_ctx_text_emplace_back_text(
  //     ctx, CG_X86_64_MNEM_MOVQ,
  //     cg_x86_64_op_new_base(offsetof(x86_64_op_tbl, op_assign),
  //                           CG_X86_64_REG_RAX),
  //     cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

  // cg_ctx_text_emplace_back_text(
  //     ctx, CG_X86_64_MNEM_LEAQ,
  //     cg_x86_64_op_new_base(to_meta->offset, CG_X86_64_REG_RBP),
  //     cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);
}

static void cg_inst_stmt_assign_value(cg_ctx *ctx, const mir_stmt *stmt) {
  const mir_value *to_value   = stmt->assign.to;
  const mir_value *from_value = stmt->assign.from_value;

  const mir_value *values[] = {to_value, from_value};

  uint64_t frame_size_old = ctx->frame_size;

  cg_inst_call_prep_values(ctx, sizeof(values) / sizeof(values[0]), values);

  cg_inst_call_op_tbl(ctx, to_value, offsetof(x86_64_op_tbl, op_assign));

  cg_inst_frame_restore(ctx, frame_size_old);
}

static void cg_inst_stmt_assign_sub(cg_ctx *ctx, const mir_stmt *stmt) {
  const mir_value      *to_value = stmt->assign.to;
  const mir_subroutine *from_sub = stmt->assign.from_sub;
  warn("unimplemented assign sub");
}

static void cg_inst_stmt_assign(cg_ctx *ctx, const mir_stmt *stmt) {
  switch (stmt->assign.kind) {
    case MIR_STMT_ASSIGN_LIT: {
      return cg_inst_stmt_assign_lit(ctx, stmt);
    }
    case MIR_STMT_ASSIGN_VALUE: {
      return cg_inst_stmt_assign_value(ctx, stmt);
    }
    case MIR_STMT_ASSIGN_SUB: {
      return cg_inst_stmt_assign_sub(ctx, stmt);
    }
  }
  error("unhandled assign kind %d", stmt->assign.kind);
}

static void cg_inst_stmt(cg_ctx *ctx, const mir_stmt *stmt) {
  switch (stmt->kind) {
    case MIR_STMT_OP:
      return cg_inst_stmt_op(ctx, stmt);
    case MIR_STMT_CALL:
      return cg_inst_stmt_call(ctx, stmt);
    case MIR_STMT_MEMBER:
      return cg_inst_stmt_member(ctx, stmt);
    case MIR_STMT_MEMBER_REF:
      return cg_inst_stmt_member_ref(ctx, stmt);
    case MIR_STMT_BUILTIN:
      return cg_inst_stmt_builtin(ctx, stmt);
    case MIR_STMT_ASSIGN:
      return cg_inst_stmt_assign(ctx, stmt);
  }
  error("unhandled stmt kind %d %p", stmt->kind, stmt);
}

static void cg_inst_sub_bb(cg_ctx *ctx, const mir_bb *bb) {
  ctx->bb = bb;

  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_text(cg_label_sub_global_bb(ctx->sub, bb)));

  for (list_mir_stmt_it it = list_mir_stmt_begin(bb->stmts); !END(it);
       NEXT(it)) {
    const mir_stmt *stmt = GET(it);
    cg_inst_stmt(ctx, stmt);
  }

  mir_bb_enum kind = mir_bb_get_cond(bb);
  switch (kind) {
    // unwrap as bool and jump if true, else not jump
    case MIR_BB_COND: {
      const cg_value_meta *value_meta =
          cg_ctx_value_meta_find(ctx, bb->jmp.cond_ref);

      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_LEAQ,
          cg_x86_64_op_new_base(value_meta->offset, CG_X86_64_REG_RBP),
          cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_CALL,
          cg_x86_64_op_new_direct(strdup("__x86_64_unwrap_bool")), NULL);

      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_TESTB,
          cg_x86_64_op_new_register(CG_X86_64_REG_RAX),
          cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_JNZ,
          cg_x86_64_op_new_direct(
              cg_label_sub_global_bb(ctx->sub, bb->jmp.je_ref)),
          NULL);

      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_JMP,
          cg_x86_64_op_new_direct(
              cg_label_sub_global_bb(ctx->sub, bb->jmp.jz_ref)),
          NULL);
      break;
    }
    case MIR_BB_NEXT: {
      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_JMP,
          cg_x86_64_op_new_direct(
              cg_label_sub_global_bb(ctx->sub, bb->jmp.next_ref)),
          NULL);
      break;
    }
    case MIR_BB_TERM: {
      cg_ctx_text_emplace_back_text(
          ctx, CG_X86_64_MNEM_JMP,
          cg_x86_64_op_new_direct(cg_label_sub_global_suf(ctx->sub, "deinit")),
          NULL);
      break;
    }
    case MIR_BB_UNKNOWN:
    default:
      error("unknown bb kind %d %p", kind, bb);
      break;
  }
}

void cg_inst_sub_bbs(cg_ctx *ctx, const list_mir_bb *bbs) {
  for (list_mir_bb_it it = list_mir_bb_begin(bbs); !END(it); NEXT(it)) {
    const mir_bb *bb = GET(it);
    cg_inst_sub_bb(ctx, bb);
  }
}
