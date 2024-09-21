#pragma once

#include "Option2Lexer.h"
#include "Option2Parser.h"
#include <antlr3.h>

pANTLR3_BASE_TREE option2_ast(pANTLR3_UINT8 input_file,
                              pANTLR3_INPUT_STREAM *stream_out,
                              pOption2Lexer *lexer_out,
                              pANTLR3_COMMON_TOKEN_STREAM *tokens_out,
                              pOption2Parser *parser_out);
