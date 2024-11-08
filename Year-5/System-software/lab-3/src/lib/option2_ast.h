#pragma once

#include "antlr3/rec/MainLexer.h"
#include "antlr3/rec/MainParser.h"
#include "util/list.h"
#include <antlr3.h>
#include <antlr3interfaces.h>

struct option2_ast {
  pANTLR3_INPUT_STREAM        stream;
  pMainLexer               lexer;
  pANTLR3_COMMON_TOKEN_STREAM tokens;
  pMainParser              parser;
  p_list                      exceptions;

  MainParser_source_return result;

  void (*free)(struct option2_ast *);
};

typedef struct option2_ast option2_ast;

option2_ast *option2_ast_build(pANTLR3_UINT8 input_file);

p_list option2_ast_build_many(p_list input_files);

const char *option2_ast_filename(const option2_ast *ast);
