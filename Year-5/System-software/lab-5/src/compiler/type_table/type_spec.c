#include "type.h"

#include "util/log.h"
#include "util/macro.h"

static type_mono_enum type_calc_mono_list(list_type_ref *list) {
  if (!list) {
    return TYPE_MONO_UNSET;
  }
  for (list_type_ref_it it = list_type_ref_begin(list); !END(it); NEXT(it)) {
    type_mono_enum spec = type_calc_mono(GET(it));
    if (spec == TYPE_MONO_UNSET || spec == TYPE_MONO_NOT_FULL) {
      return spec;
    }
  }
  return TYPE_MONO_FULL;
}

type_mono_enum type_calc_mono(const type_base *generic) {
  if (!generic) {
    return TYPE_MONO_UNSET;
  }

  switch (generic->kind) {

  case TYPE_PRIMITIVE:
    return TYPE_MONO_FULL;

  case TYPE_ARRAY: {
    const type_array *self = (const type_array *)generic;
    return type_calc_mono(self->element_ref);
  }

  case TYPE_CALLABLE: {
    const type_callable *self = (const type_callable *)generic;
    type_mono_enum       spec = type_calc_mono(self->ret_ref);
    if (spec == TYPE_MONO_UNSET || spec == TYPE_MONO_NOT_FULL) {
      return spec;
    }
    return type_calc_mono_list(self->params);
  }

  case TYPE_CLASS: {
    const type_class *self = (const type_class *)generic;
    if (!self->typenames) {
      return TYPE_MONO_UNSET;
    }
    if (list_type_ref_size(self->typenames)) {
      return TYPE_MONO_NOT_FULL;
    }
    return type_calc_mono_list(self->parents);
  }

  case TYPE_TYPENAME:
    return TYPE_MONO_NOT_FULL;

  case TYPE_MONO: {
    const type_mono *self = (const type_mono *)generic;

    if (!self->type_ref) {
      error("type_mono isn't attached to any type (null)");
      return TYPE_MONO_UNSET;
    }

    switch (self->type_ref->kind) {
    case TYPE_CLASS: {
      const type_class *attached = (const type_class *)self->type_ref;

      size_t typenames_sz = list_type_ref_size(attached->typenames);
      size_t templates_sz = list_type_ref_size(self->templates);
      size_t specs_sz = list_type_ref_size(self->specs);
      if (typenames_sz != templates_sz || typenames_sz != specs_sz) {
        error("typenames size is not equal to templates or specs size");
        return TYPE_MONO_UNSET;
      }

      return type_calc_mono_list(self->specs);
    }
    default:
      error("type_mono currently can be attached ONLY to classes");
      return TYPE_MONO_UNSET;
    }
  }

  case TYPE_UNKNOWN:
  default:
    error("unexpected type kind %d %p", generic->kind, generic);
    return TYPE_MONO_UNSET;
  }
}

type_mono_enum type_get_mono(const type_base *generic) {
  if (!generic) {
    return TYPE_MONO_UNSET;
  }
  switch (generic->kind) {
  case TYPE_PRIMITIVE:
    return TYPE_MONO_FULL;
  case TYPE_TYPENAME:
    return TYPE_MONO_NOT_FULL;
  case TYPE_ARRAY:
    return ((const type_array *)generic)->spec;
  case TYPE_CALLABLE:
    return ((const type_callable *)generic)->spec;
  case TYPE_CLASS:
    return ((const type_class *)generic)->spec;
  case TYPE_MONO:
    return ((const type_mono *)generic)->spec;
  case TYPE_UNKNOWN:
  default:
    error("unexpected type kind %d %p", generic->kind, generic);
    return TYPE_MONO_UNSET;
  }
}
