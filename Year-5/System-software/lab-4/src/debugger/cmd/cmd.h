#pragma once

#include "debugger/cmd/handler.h"
#include "debugger/context/context.h"

void cmd_exit(context *state, const cmd_entry *entry, const char *rest);
void cmd_echo(context *state, const cmd_entry *entry, const char *rest);
