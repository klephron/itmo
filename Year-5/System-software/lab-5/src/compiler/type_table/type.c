#include "type.h"
#include "util/log.h"

#include "util/macro.h"
#include <stdlib.h>

static void type_base_deinit(type_base *base) { UNUSED(base); }

static void type_base_init(type_base *base, type_enum kind) {
  base->kind = kind;
  base->type_entry_ref = NULL;
}

static void type_primitive_free(type_primitive *self) {
  if (self) {
    type_base_deinit(&self->base);
    free(self);
  }
}

static void type_array_free(type_array *self) {
  if (self) {
    type_base_deinit(&self->base);
    free(self);
  }
}

static void type_callable_free(type_callable *self) {
  if (self) {
    list_type_ref_free(self->params);
    type_base_deinit(&self->base);
    free(self);
  }
}

static void type_class_free(type_class *self) {
  if (self) {
    if (self->id) {
      free(self->id);
    }
    list_type_ref_free(self->typenames);
    list_type_ref_free(self->parents);
    type_base_deinit(&self->base);
    free(self);
  }
}

static void type_typename_free(type_typename *self) {
  if (self) {
    if (self->id) {
      free(self->id);
    }
    free(self);
  }
}

static void type_mono_free(type_mono *self) {
  if (self) {
    list_type_ref_free(self->types);
    type_base_deinit(&self->base);
    free(self);
  }
}

type_primitive *type_primitive_new(type_primitive_enum type) {
  type_primitive *self = MALLOC(type_primitive);
  type_base_init(&self->base, TYPE_PRIMITIVE);
  self->type = type;
  return self;
}

type_array *type_array_new(type_base *element_ref) {
  type_array *self = MALLOC(type_array);
  type_base_init(&self->base, TYPE_ARRAY);
  self->element_ref = element_ref;
  self->mono_kind = TYPE_MONO_UNSET;
  return self;
}

type_callable *type_callable_new(type_base *ret_ref, list_type_ref *params) {
  type_callable *self = MALLOC(type_callable);
  type_base_init(&self->base, TYPE_CALLABLE);
  self->ret_ref = ret_ref;
  self->params = params;
  self->mono_kind = TYPE_MONO_UNSET;
  return self;
}

type_class *type_class_new(char *id, list_type_ref *typenames,
                           list_type_ref *parents) {
  type_class *self = MALLOC(type_class);
  type_base_init(&self->base, TYPE_CLASS);
  self->id = id;
  self->typenames = typenames;
  self->parents = parents;
  self->mono_kind = TYPE_MONO_UNSET;
  return self;
}

type_typename *type_typename_new(char *id, type_base *source_ref) {
  type_typename *self = MALLOC(type_typename);
  type_base_init(&self->base, TYPE_TYPENAME);
  self->id = id;
  self->source_ref = source_ref;
  return self;
}

// can only specialize classes
type_mono *type_mono_new(type_base *type_ref, list_type_ref *types) {
  type_mono *self = MALLOC(type_mono);
  type_base_init(&self->base, TYPE_MONO);
  self->type_ref = type_ref;
  self->types = types;
  self->mono_kind = TYPE_MONO_UNSET;
  return self;
}

void type_free(type_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case TYPE_PRIMITIVE:
      return type_primitive_free((type_primitive *)generic);
    case TYPE_ARRAY:
      return type_array_free((type_array *)generic);
    case TYPE_CALLABLE:
      return type_callable_free((type_callable *)generic);
    case TYPE_CLASS:
      return type_class_free((type_class *)generic);
    case TYPE_TYPENAME:
      return type_typename_free((type_typename *)generic);
    case TYPE_MONO:
      return type_mono_free((type_mono *)generic);
    case TYPE_UNKNOWN:
    default:
      error("unexpected type_entry kind %d %p", generic->kind, generic);
      break;
    }
  }
}

void type_set_type_entry(type_base *generic, type_entry *type_entry_ref) {
  if (!generic) {
    error("can set type_entry %p, type is null", type_entry_ref);
    return;
  }

  switch (generic->kind) {
  case TYPE_PRIMITIVE:
  case TYPE_ARRAY:
  case TYPE_CALLABLE:
  case TYPE_CLASS:
  case TYPE_TYPENAME:
  case TYPE_MONO:
    generic->type_entry_ref = type_entry_ref;
    break;
  case TYPE_UNKNOWN:
  default:
    error("unexpected type_entry kind %d %p", generic->kind, generic);
    break;
  }
}

type_entry *type_get_type_entry(type_base *generic) {
  if (!generic) {
    error("can get type_entry, type is null");
    return NULL;
  }
  return generic->type_entry_ref;
}
