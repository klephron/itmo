#pragma once

#include "eir/eir.h"
#include "exception/container.h"
#include "mir/mir.h"

// builds MIR from EIR (which is useless, exists only for lab purpose)

typedef struct mir_ctx_struct {
  mir            *mir;
  list_exception *exceptions;
} mir_ctx;

mir_ctx *mir_ctx_new(mir *mir);
void     mir_ctx_free(mir_ctx *self);
void     mir_ctx_build(mir_ctx *self, eir *eir);
