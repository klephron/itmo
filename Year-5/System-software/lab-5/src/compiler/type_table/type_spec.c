#include "type.h"

#include "util/log.h"
#include "util/macro.h"

static type_spec_enum type_calc_spec_list(list_type_ref *list) {
  if (!list) {
    return TYPE_SPEC_UNSET;
  }
  for (list_type_ref_it it = list_type_ref_begin(list); !END(it); NEXT(it)) {
    type_spec_enum spec = type_calc_spec(GET(it));
    if (spec == TYPE_SPEC_UNSET || spec == TYPE_SPEC_NOT_FULL) {
      return spec;
    }
  }
  return TYPE_SPEC_FULL;
}

type_spec_enum type_calc_spec(const type_base *generic) {
  if (!generic) {
    return TYPE_SPEC_UNSET;
  }

  switch (generic->kind) {

  case TYPE_PRIMITIVE:
    return TYPE_SPEC_FULL;

  case TYPE_ARRAY: {
    const type_array *self = (const type_array *)generic;
    return type_calc_spec(self->element_ref);
  }

  case TYPE_CALLABLE: {
    const type_callable *self = (const type_callable *)generic;
    type_spec_enum       spec = type_calc_spec(self->ret_ref);
    if (spec == TYPE_SPEC_UNSET || spec == TYPE_SPEC_NOT_FULL) {
      return spec;
    }
    return type_calc_spec_list(self->params);
  }

  case TYPE_CLASS: {
    const type_class *self = (const type_class *)generic;
    if (!self->typenames) {
      return TYPE_SPEC_UNSET;
    }
    if (list_type_ref_size(self->typenames)) {
      return TYPE_SPEC_NOT_FULL;
    }
    return type_calc_spec_list(self->parents);
  }

  case TYPE_TYPENAME:
    return TYPE_SPEC_NOT_FULL;

  case TYPE_SPECIALIZED: {
    const type_specialized *self = (const type_specialized *)generic;

    if (!self->type_ref) {
      error("type_specialized isn't attached to any type (null)");
      return TYPE_SPEC_UNSET;
    }

    switch (self->type_ref->kind) {
    case TYPE_CLASS: {
      const type_class *attached = (const type_class *)self->type_ref;

      size_t typenames_sz = list_type_ref_size(attached->typenames);
      size_t templates_sz = list_type_ref_size(self->templates);
      size_t specs_sz = list_type_ref_size(self->specs);
      if (typenames_sz != templates_sz || typenames_sz != specs_sz) {
        error("typenames size is not equal to templates or specs size");
        return TYPE_SPEC_UNSET;
      }

      return type_calc_spec_list(self->specs);
    }
    default:
      error("type_specialized currently can be attached ONLY to classes");
      return TYPE_SPEC_UNSET;
    }
  }

  case TYPE_UNKNOWN:
  default:
    error("unexpected type kind %d %p", generic->kind, generic);
    return TYPE_SPEC_UNSET;
  }
}

type_spec_enum type_get_spec(const type_base *generic) {
  if (!generic) {
    return TYPE_SPEC_UNSET;
  }
  switch (generic->kind) {
  case TYPE_PRIMITIVE:
    return TYPE_SPEC_FULL;
  case TYPE_TYPENAME:
    return TYPE_SPEC_NOT_FULL;
  case TYPE_ARRAY:
    return ((const type_array *)generic)->spec;
  case TYPE_CALLABLE:
    return ((const type_callable *)generic)->spec;
  case TYPE_CLASS:
    return ((const type_class *)generic)->spec;
  case TYPE_SPECIALIZED:
    return ((const type_specialized *)generic)->spec;
  case TYPE_UNKNOWN:
  default:
    error("unexpected type kind %d %p", generic->kind, generic);
    return TYPE_SPEC_UNSET;
  }
}
