// static void mir_tree_str_type(strbuf *buffer, size_t pad,
//                               const hir_type_base *type) {
//   UNUSED(pad);
//   char *s = hir_type_str(type);
//   if (s) {
//     strbuf_append(buffer, s);
//     free(s);
//   } else {
//     strbuf_append(buffer, "(nil)");
//   }
// }

// static void mir_tree_str_debug_line(strbuf *buffer, size_t pad,
//                                     const mir_debug_line *debug_line) {
//   UNUSED(pad);
//   char buf[64];
//   if (debug_line) {
//     strbuf_append(buffer, debug_line->source_ref);
//     strbuf_append(buffer, ":");
//     snprintf(buf, STRMAXLEN(buf), "%zu", debug_line->line);
//     strbuf_append(buffer, buf);
//   } else {
//     strbuf_append(buffer, "(nil)");
//   }
// }

// static void mir_tree_str_debug_symbol(strbuf *buffer, size_t pad,
//                                       const mir_debug_symbol *debug_symbol) {
//   UNUSED(pad);
//   if (debug_symbol) {
//     const hir_symbol *binding = debug_symbol->binding_ref;
//     strbuf_append(buffer, "(name: ");
//     strbuf_append(buffer, binding->name);
//     char *type = hir_type_str(binding->type);
//     if (type) {
//       strbuf_append(buffer, ", type: ");
//       strbuf_append(buffer, type);
//       free(type);
//     }
//     strbuf_append(buffer, ")");
//   } else {
//     strbuf_append(buffer, "(nil)");
//   }
// }

// static void mir_tree_str_subroutine_ext(strbuf *buffer, size_t pad,
//                                         const mir_subroutine_ext *sub) {
//   strbuf_append(buffer, "method ");
//   strbuf_append(buffer, sub->name);
//   strbuf_append(buffer, "(");
//   {
//     list_type_it it = list_type_begin(sub->args);
//     if (!END(it)) {
//       mir_tree_str_type(buffer, pad, GET(it));
//       NEXT(it);
//     }
//     for (; !END(it); NEXT(it)) {
//       strbuf_append(buffer, ", ");
//       mir_tree_str_type(buffer, pad, GET(it));
//     }
//   }
//   strbuf_append(buffer, "): ");
//   mir_tree_str_type(buffer, pad, sub->ret);
// }

// static void mir_tree_str_value(strbuf *buffer, size_t pad,
//                                const mir_value *value) {
//   char buf[32];
//   strbuf_append(buffer, "VALUE(");
//   snprintf(buf, STRMAXLEN(buf), "_%zu)", value->id);
//   strbuf_append(buffer, buf);
//   if (value->debug_symbol) {
//     strbuf_append(buffer, "\t#debug");
//     mir_tree_str_debug_symbol(buffer, pad, value->debug_symbol);
//   }
// }

// static void mir_tree_str_stmt(strbuf *buffer, size_t pad,
//                               const mir_node_base *stmt) {
//   char buf[32];

//   if (!stmt) {
//     return;
//   }

//   switch (stmt->kind) {
//   case MIR_NODE_VALUE: {
//     const mir_value *value = (mir_value *)stmt;
//     snprintf(buf, STRMAXLEN(buf), "_%zu", value->id);
//     strbuf_append(buffer, buf);
//   } break;
//   case MIR_NODE_LIT: {
//     const mir_lit *lit = (mir_lit *)stmt;
//     char          *vs = hir_lit_value_str(lit->lit);
//     char          *ts = hir_type_str(lit->lit->type);

//     if (ts) {
//       strbuf_append(buffer, "(");
//       strbuf_append(buffer, ts);
//       strbuf_append(buffer, ")");
//       free(ts);
//     }
//     if (vs) {
//       strbuf_append(buffer, vs);
//       free(vs);
//     }
//   } break;
//   case MIR_NODE_BUILTIN: {
//     const mir_builtin *builtin = (mir_builtin *)stmt;
//     strbuf_append(buffer, mir_builtin_enum_str(builtin->kind));
//     if (builtin->spec) {
//       strbuf_append(buffer, "<");
//       char *ts = hir_type_str(builtin->spec);
//       if (ts) {
//         strbuf_append(buffer, ts);
//         free(ts);
//       }
//       strbuf_append(buffer, ">");
//     }

//     strbuf_append(buffer, "(");
//     list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//     if (!END(it)) {
//       mir_tree_str_stmt(buffer, pad, GET(it));
//       NEXT(it);
//     }
//     for (; !END(it); NEXT(it)) {
//       strbuf_append(buffer, ", ");
//       mir_tree_str_stmt(buffer, pad, GET(it));
//     }
//     strbuf_append(buffer, ")");
//     if (builtin->debug_line) {
//       strbuf_append(buffer, "\t#debug ");
//       mir_tree_str_debug_line(buffer, pad, builtin->debug_line);
//     }
//   } break;
//   case MIR_NODE_SUBROUTINE: {
//     const mir_subroutine *sub = (mir_subroutine *)stmt;
//     strbuf_append(buffer, sub->name);
//   } break;
//   case MIR_NODE_SUBROUTINE_EXTERNAL: {
//     const mir_subroutine_ext *sub = (mir_subroutine_ext *)stmt;
//     strbuf_append(buffer, sub->name);
//   } break;
//   case MIR_NODE_UNKNOWN:
//   default:
//     error("unexpected kind %d %p", stmt->kind, stmt);
//   }
// }

// static void mir_tree_str_jmp(strbuf *buffer, size_t pad,
//                              const mir_jmp *jmp) {
//   UNUSED(pad);

//   char buf[32];

//   if (jmp) {
//     if (jmp->value_ref || jmp->je_ref) {
//       strbuf_append(buffer, "JE(");

//       if (jmp->value_ref) {
//         snprintf(buf, STRMAXLEN(buf), "_%zu, ", jmp->value_ref->id);
//       } else {
//         snprintf(buf, STRMAXLEN(buf), "(nil), ");
//       }
//       strbuf_append(buffer, buf);

//       if (jmp->je_ref) {
//         snprintf(buf, STRMAXLEN(buf), "bb%zu, ", jmp->je_ref->id);
//       } else {
//         snprintf(buf, STRMAXLEN(buf), "(nil), ");
//       }
//       strbuf_append(buffer, buf);

//     } else {
//       strbuf_append(buffer, "JMP(");
//     }

//     if (jmp->jz_ref) {
//       snprintf(buf, STRMAXLEN(buf), "bb%zu", jmp->jz_ref->id);
//     } else {
//       snprintf(buf, STRMAXLEN(buf), "(nil)");
//     }
//     strbuf_append(buffer, buf);
//     strbuf_append(buffer, ")");

//     if (jmp->debug_line) {
//       strbuf_append(buffer, "\t#debug ");
//       mir_tree_str_debug_line(buffer, pad, jmp->debug_line);
//     }
//   } else {
//     strbuf_append(buffer, "return");
//   }
// }

// static void mir_tree_str_bb(strbuf *buffer, size_t pad,
//                             const mir_bb *bb) {
//   char buf[32];
//   snprintf(buf, STRMAXLEN(buf), "bb%zu {", bb->id);
//   strbuf_appendln(buffer, buf);
//   pad += MIR_STR_TAB;

//   for (list_mir_node_it it = list_mir_node_begin(bb->stmts); !END(it);
//        NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_stmt(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "");
//   }

//   strbuf_append_padding(buffer, pad);
//   mir_tree_str_jmp(buffer, pad, bb->jmp);
//   strbuf_appendln(buffer, "");

//   pad -= MIR_STR_TAB;
//   strbuf_append_padding(buffer, pad);
//   strbuf_append(buffer, "}");
// }

// static void mir_tree_str_subroutine(strbuf *buffer, size_t pad,
//                                     const mir_subroutine *sub) {
//   strbuf_append(buffer, "method ");
//   strbuf_append(buffer, sub->name);
//   strbuf_append(buffer, "(");
//   {
//     list_mir_value_it it = list_mir_value_begin(sub->args);
//     if (!END(it)) {
//       mir_tree_str_type(buffer, pad,
//                         GET(it)->debug_symbol->binding_ref->type);
//       NEXT(it);
//     }
//     for (; !END(it); NEXT(it)) {
//       strbuf_append(buffer, ", ");
//       mir_tree_str_type(buffer, pad,
//                         GET(it)->debug_symbol->binding_ref->type);
//     }
//   }
//   strbuf_append(buffer, "): ");
//   strbuf_appendln(buffer, " {");
//   pad += MIR_STR_TAB;

//   strbuf_append_padding(buffer, pad);
//   mir_tree_str_value(buffer, pad, sub->ret);
//   strbuf_appendln(buffer, "\t// ret");

//   for (list_mir_value_it it = list_mir_value_begin(sub->args); !END(it);
//        NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_value(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "\t// arg");
//   }

//   for (list_mir_value_it it = list_mir_value_begin(sub->vars); !END(it);
//        NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_value(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "\t// var");
//   }

//   for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !END(it);
//        NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_value(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "\t// tmp");
//   }

//   for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !END(it);
//        NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_bb(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "");
//   }

//   pad -= MIR_STR_TAB;
//   strbuf_append_padding(buffer, pad);
//   strbuf_append(buffer, "}");
// }

// char *mir_tree_str(const mir *mir) {
//   strbuf *buffer = strbuf_new(0, 0);
//   strbuf_appendln(buffer, "{");
//   size_t pad = MIR_STR_TAB;

//   strbuf_append_padding(buffer, pad);
//   strbuf_appendln(buffer, "EXTERNAL SUBROUTINES: [");
//   pad += MIR_STR_TAB;
//   for (list_mir_subroutine_ext_it it =
//            list_mir_subroutine_ext_begin(mir->sub_exts);
//        !END(it); NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_subroutine_ext(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "");
//   }
//   pad -= MIR_STR_TAB;
//   strbuf_append_padding(buffer, MIR_STR_TAB);
//   strbuf_appendln(buffer, "]");

//   strbuf_append_padding(buffer, pad);
//   strbuf_appendln(buffer, "SUBROUTINES: [");
//   pad += MIR_STR_TAB;
//   for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
//        !END(it); NEXT(it)) {
//     strbuf_append_padding(buffer, pad);
//     mir_tree_str_subroutine(buffer, pad, GET(it));
//     strbuf_appendln(buffer, "");
//   }
//   pad -= MIR_STR_TAB;
//   return strbuf_detach(buffer);
// }

// const char *mir_node_enum_str(mir_node_enum kind) {
//   switch (kind) {
//   case MIR_NODE_VALUE:
//     return "VALUE";
//   case MIR_NODE_LIT:
//     return "LIT";
//   case MIR_NODE_BUILTIN:
//     return "BUILTIN";
//   case MIR_NODE_SUBROUTINE:
//     return "SUBROUTINE";
//   case MIR_NODE_SUBROUTINE_EXTERNAL:
//     return "SUBROUTINE_EXTERNAL";
//   default:
//   case MIR_NODE_UNKNOWN:
//     return "UNKNOWN";
//   }
// }
