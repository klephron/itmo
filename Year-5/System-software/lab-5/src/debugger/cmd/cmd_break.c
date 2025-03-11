// #include "debugger/cmd/cmd.h"

// #include "debugger/context/context.h"
// #include "util/macro.h"
// #include "util/string.h"

// static const char *desc = "Set breakpoint at specific location";

// static const char *help =
//     "Usage: <command> (<address>|<file_id> <line>)\n"
//     "\t<address>: address in hex\n"
//     "\t<file_id>: id of file, see 'info info' or 'info files'\n"
//     "\t<line>: line in file";

// static void fn_out_breakpoint(context *ctx, breakpoint *breakpoint) {
//   char buf[64];
//   context_out_append(ctx, "breakpoint ");
//   snprintf(buf, STRMAXLEN(buf), "%zu ", breakpoint->id);
//   context_out_append(ctx, buf);
//   snprintf(buf, STRMAXLEN(buf), "at 0x%lx: ", breakpoint->address);
//   context_out_append(ctx, buf);
//   context_out_append(ctx, "file ");
//   if (breakpoint->file_id != UINT64_MAX) {
//     context_out_append(ctx, context_dbg_file_by_id(ctx, breakpoint->file_id));
//   } else {
//     context_out_append(ctx, "-");
//   }
//   context_out_append(ctx, ", line: ");
//   if (breakpoint->line != UINT64_MAX) {
//     snprintf(buf, STRMAXLEN(buf), "%zu", breakpoint->line);
//     context_out_append(ctx, buf);
//   } else {
//     context_out_append(ctx, "-");
//   }
//   context_out_appendln(ctx, "");
// }

// static void fn(cmd_handler *handler, context *ctx, const char *rest) {

//   UNUSED(handler);
//   char *args = NULL;
//   char *arg_first = NULL;
//   char *arg_second = NULL;

//   args = trim(strdup(rest));

//   if (cmd_error_if_no_target(ctx)) {
//     goto cleanup;
//   }

//   {
//     char *token_args = strdup(args);
//     char *token = strtok(token_args, " ");
//     if (token) {
//       arg_first = strdup(token);
//       token = strtok(NULL, " ");
//     }
//     if (token) {
//       arg_second = strdup(token);
//       token = strtok(NULL, " ");
//     }
//     if (token_args) {
//       free(token_args);
//     }
//   }

//   if (!arg_first) {
//     goto error_format;
//   }

//   uint64_t address;
//   uint64_t file_id = UINT64_MAX;
//   uint64_t line = UINT64_MAX;

//   // breakpoint from file+line
//   if (arg_second && *arg_second) {
//     debug_line d_line;
//     sscanf(arg_first, "%lu", &file_id);
//     sscanf(arg_second, "%lu", &line);
//     d_line.file_id = file_id;
//     d_line.line = line;
//     if (context_dbg_line_by_source(ctx, &d_line) == -1) {
//       char buf[64];
//       context_err_append(ctx, "debug info for file: ");
//       context_err_append(ctx, context_dbg_file_by_id(ctx, file_id));
//       context_err_append(ctx, buf);
//       context_err_append(ctx, ", line: ");
//       snprintf(buf, STRMAXLEN(buf), "%lu", line);
//       context_err_append(ctx, buf);
//       context_error(ctx, " doesn't exist");
//       goto cleanup;
//     }
//     address = d_line.address;
//   } else {
//     sscanf(arg_first, "%lx", &address);
//   }

//   hashset_breakpoint_it it =
//       hashset_breakpoint_find(ctx->breaks, &(breakpoint){.address = address});
//   if (!END(it)) {
//     char buf[64];
//     context_err_append(ctx, "breakpoint on address ");
//     snprintf(buf, STRMAXLEN(buf), "0x%lx", address);
//     context_err_append(ctx, buf);
//     context_error(ctx, " already exists");
//     goto cleanup;
//   }

//   breakpoint *breakpoint =
//       breakpoint_new(++ctx->breaks_cnt, 0, address, file_id, line);
//   breakpoint->is_enabled = 1;

//   hashset_breakpoint_insert(ctx->breaks, breakpoint);

//   if (context_tg_is_running(ctx)) {
//     context_bp_place(ctx, breakpoint);
//   }

//   fn_out_breakpoint(ctx, breakpoint);
//   goto cleanup;

// error_format:
//   cmd_error_format(ctx, args, help);

// cleanup:
//   if (arg_first) {
//     free(arg_first);
//   }
//   if (arg_second) {
//     free(arg_second);
//   }
//   if (args) {
//     free(args);
//   }
// }

// cmd *cmd_break(const char *cmd_ref) {
//   return cmd_entry_new(fn, desc, help, cmd_ref);
// }
