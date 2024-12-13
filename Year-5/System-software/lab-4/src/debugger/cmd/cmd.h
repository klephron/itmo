#pragma once

#include "debugger/context/context.h"

void cmd_exit(context *ctx, const char *rest);
void cmd_echo(context *ctx, const char *rest);
void cmd_file(context *ctx, const char *rest);
void cmd_run(context *ctx, const char *rest);
void cmd_info(context *ctx, const char *rest);
void cmd_kill(context *ctx, const char *rest);
void cmd_print(context *ctx, const char *rest);
void cmd_x(context *ctx, const char *rest);
void cmd_disassemble(context *ctx, const char *rest);
void cmd_continue(context *ctx, const char *rest);
