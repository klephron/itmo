#pragma once

#include "debugger/cmd/handler.h"
#include "debugger/context/context.h"

void cmd_exit(context *ctx, const cmd_entry *entry, const char *rest);
void cmd_echo(context *ctx, const cmd_entry *entry, const char *rest);
void cmd_file(context *ctx, const cmd_entry *entry, const char *rest);
void cmd_run(context *ctx, const cmd_entry *entry, const char *rest);
void cmd_info(context *ctx, const cmd_entry *entry, const char *rest);
void cmd_kill(context *ctx, const cmd_entry *entry, const char *rest);
