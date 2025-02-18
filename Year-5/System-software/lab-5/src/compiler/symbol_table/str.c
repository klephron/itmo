// #include "symbol_table.h"

// // HIR_SYMBOL
// typedef struct hir_symbol_table_struct {
//   char *id_ptr;
//   char *name;
//   char *bind_kind;
//   char *bind_parent;
//   char *type;
//   char *span;
// } hir_symbol_table;

// static void hir_symbol_table_deinit(hir_symbol_table *self) {
//   if (self) {
//     free(self->id_ptr);
//     free(self->name);
//     free(self->bind_kind);
//     free(self->bind_parent);
//     free(self->type);
//     free(self->span);
//   }
// }

// const char *hir_binding_enum_str(hir_binding_enum kind) {
//   switch (kind) {
//   case HIR_BINDING_GLOBAL:
//     return "GLOBAL";
//   case HIR_BINDING_SUBROUTINE:
//     return "SUBROUTINE";
//   default:
//     warn("unknown binding_enum %d", kind);
//     return "UNKNOWN";
//   }
// }

// static void hir_binding_parent_str(string_buffer    *buffer,
//                                    const hir_symbol *parent) {
//   if (parent) {
//     hir_binding_parent_str(buffer, parent->bind_parent_ref);
//     if (parent->bind_parent_ref) {
//       string_buffer_append(buffer, "::");
//     }
//     string_buffer_append(buffer, parent->name);
//   }
// }

// static hir_symbol_table hir_symbol_to_table(const hir_symbol *symbol) {
//   char id_ptr[16];
//   snprintf(id_ptr, STRMAXLEN(id_ptr), "%p", symbol);

//   string_buffer *bind_parent_buffer = string_buffer_new(16, 0);
//   if (symbol->bind_parent_ref) {
//     hir_binding_parent_str(bind_parent_buffer, symbol->bind_parent_ref);
//   } else {
//     string_buffer_append(bind_parent_buffer, "-");
//   }

//   char *type = hir_type_str(symbol->type);

//   return (hir_symbol_table){
//       .id_ptr = strdup(id_ptr),
//       .name = strdup(symbol->name),
//       .bind_kind = strdup(hir_binding_enum_str(symbol->bind_kind)),
//       .bind_parent = string_buffer_detach(bind_parent_buffer),
//       .type = type ? type : strdup("-"),
//       .span = span_str(symbol->decl_span),
//   };
// }

// char *hir_list_symbol_str(const list_hir_symbol *symbols) {
//   string_buffer *buffer = string_buffer_new(0, 0);

//   const char *id_h = "ID(PTR)";
//   const char *name_h = "NAME";
//   const char *bind_kind_h = "BIND_KIND";
//   const char *bind_parent_h = "BIND_PARENT";
//   const char *type_h = "TYPE";
//   const char *span_h = "SPAN";

//   size_t id_ms = strlen(id_h);
//   size_t name_ms = strlen(name_h);
//   size_t bind_kind_ms = strlen(bind_kind_h);
//   size_t bind_parent_ms = strlen(bind_parent_h);
//   size_t type_ms = strlen(type_h);
//   size_t span_ms = strlen(span_h);

//   for (list_hir_symbol_it it =
//            list_hir_symbol_begin((list_hir_symbol *)symbols);
//        !END(it); NEXT(it)) {
//     const hir_symbol *symbol = GET(it);
//     hir_symbol_table  table = hir_symbol_to_table(symbol);

//     size_t id_s = strlen(table.id_ptr);
//     size_t name_s = strlen(table.name);
//     size_t bind_kind_s = strlen(table.bind_kind);
//     size_t bind_parent_s = strlen(table.bind_parent);
//     size_t type_s = strlen(table.type);
//     size_t span_s = strlen(table.span);

//     id_ms = max_size_t(id_ms, id_s);
//     name_ms = max_size_t(name_ms, name_s);
//     bind_kind_ms = max_size_t(bind_kind_ms, bind_kind_s);
//     bind_parent_ms = max_size_t(bind_parent_ms, bind_parent_s);
//     type_ms = max_size_t(type_ms, type_s);
//     span_ms = max_size_t(span_ms, span_s);

//     hir_symbol_table_deinit(&table);
//   }

//   // separator
//   {
//     string_buffer_append(buffer, "┌");
//     string_buffer_append_cell(buffer, id_ms, "", "─");
//     string_buffer_append(buffer, "┬");
//     string_buffer_append_cell(buffer, name_ms, "", "─");
//     string_buffer_append(buffer, "┬");
//     string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
//     string_buffer_append(buffer, "┬");
//     string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
//     string_buffer_append(buffer, "┬");
//     string_buffer_append_cell(buffer, type_ms, "", "─");
//     string_buffer_append(buffer, "┬");
//     string_buffer_append_cell(buffer, span_ms, "", "─");
//     string_buffer_append(buffer, "┐\n");
//   }

//   // header
//   {
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, id_ms, id_h, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, name_ms, name_h, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, bind_kind_ms, bind_kind_h,
//     HIR_STR_SPACE); string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, bind_parent_ms, bind_parent_h,
//                               HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, type_ms, type_h, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, span_ms, span_h, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│\n");
//   }

//   // separator
//   {
//     string_buffer_append(buffer, "├");
//     string_buffer_append_cell(buffer, id_ms, "", "─");
//     string_buffer_append(buffer, "┼");
//     string_buffer_append_cell(buffer, name_ms, "", "─");
//     string_buffer_append(buffer, "┼");
//     string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
//     string_buffer_append(buffer, "┼");
//     string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
//     string_buffer_append(buffer, "┼");
//     string_buffer_append_cell(buffer, type_ms, "", "─");
//     string_buffer_append(buffer, "┼");
//     string_buffer_append_cell(buffer, span_ms, "", "─");
//     string_buffer_append(buffer, "┤\n");
//   }

//   // body
//   for (list_hir_symbol_it it =
//            list_hir_symbol_begin((list_hir_symbol *)symbols);
//        !END(it); NEXT(it)) {
//     const hir_symbol *symbol = GET(it);
//     hir_symbol_table  table = hir_symbol_to_table(symbol);

//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, id_ms, table.id_ptr, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, name_ms, table.name, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, bind_kind_ms, table.bind_kind,
//                               HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, bind_parent_ms, table.bind_parent,
//                               HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, type_ms, table.type, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│");
//     string_buffer_append_cell(buffer, span_ms, table.span, HIR_STR_SPACE);
//     string_buffer_append(buffer, "│\n");

//     hir_symbol_table_deinit(&table);
//   }

//   // separator
//   {
//     string_buffer_append(buffer, "└");
//     string_buffer_append_cell(buffer, id_ms, "", "─");
//     string_buffer_append(buffer, "┴");
//     string_buffer_append_cell(buffer, name_ms, "", "─");
//     string_buffer_append(buffer, "┴");
//     string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
//     string_buffer_append(buffer, "┴");
//     string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
//     string_buffer_append(buffer, "┴");
//     string_buffer_append_cell(buffer, type_ms, "", "─");
//     string_buffer_append(buffer, "┴");
//     string_buffer_append_cell(buffer, span_ms, "", "─");
//     string_buffer_append(buffer, "┘\n");
//   }

//   return string_buffer_detach(buffer);
// }
