#pragma once

#define IR_ANTLR3_USERP() (ctx->pTreeParser->rec->state->userp)

#define IR_ANTLR3_CHARS(id)                                                    \
  ((const char *)id->getText(id)->to8(id->getText(id))->chars)

#define IR_ANTLR3_SIZE(id) ((size_t)id->children->size(id->children))
