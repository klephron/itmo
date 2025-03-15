#include "util.h"

#include "util/log.h"
#include "util/strbuf.h"
#include "x86_64/proxy/value/error.h"
#include "x86_64/proxy/value/string.h"
#include "x86_64/util.h"
#include "x86_64/value/byte.h"
#include "x86_64/value/char.h"
#include "x86_64/value/int.h"
#include "x86_64/value/long.h"
#include "x86_64/value/uint.h"
#include "x86_64/value/ulong.h"

void __x86_64_proxy_op_error_undefined(x86_64_value *out, x86_64_value *self,
                                       const char *op) {
  strbuf *buffer = strbuf_new(64, 0);

  x86_64_value self_string;
  self->op_tbl->op_repr(&self_string, self);

  strbuf_append(buffer, "op ");
  strbuf_append(buffer, op);
  strbuf_append(buffer, " is undefined for `");
  strbuf_append(buffer, (char *)self_string.data_ptr);
  strbuf_append(buffer, "`: ");
  strbuf_append(buffer, __x86_64_type_enum_str(self->type));

  self_string.op_tbl->op_drop(&self_string);

  x86_64_value error_value;
  __x86_64_proxy_string_init(&error_value, (uint8_t *)strbuf_data(buffer));

  strbuf_free(buffer);

  x86_64_value error;
  __x86_64_proxy_error_init(&error, &error_value);

  *out = error;
}

void __x86_64_proxy_op_error_type_mismatch(x86_64_value *out,
                                           x86_64_value *self, const char *op,
                                           x86_64_type_enum type) {
  strbuf *buffer = strbuf_new(64, 0);

  x86_64_value self_string;
  self->op_tbl->op_repr(&self_string, self);

  strbuf_append(buffer, "op ");
  strbuf_append(buffer, op);
  strbuf_append(buffer, " type mismatch, expected ");
  strbuf_append(buffer, __x86_64_type_enum_str(self->type));
  strbuf_append(buffer, " got `");
  strbuf_append(buffer, (char *)self_string.data_ptr);
  strbuf_append(buffer, "`: ");

  strbuf_append(buffer, __x86_64_type_enum_str(type));

  self->op_tbl->op_drop(&self_string);

  x86_64_value error_value;
  __x86_64_proxy_string_init(&error_value, (uint8_t *)strbuf_data(buffer));

  strbuf_free(buffer);

  x86_64_value error;
  __x86_64_proxy_error_init(&error, &error_value);

  *out = error;
}

void __x86_64_proxy_op_error_cast_unable(x86_64_value *out, x86_64_value *self,
                                         x86_64_type_enum type) {
  strbuf      *buffer = strbuf_new(0, 0);
  x86_64_value error_value;

  strbuf_append(buffer, "unable to cast `");

  x86_64_value self_str;
  self->op_tbl->op_repr(&self_str, self);
  strbuf_append(buffer, self_str.data_ptr);
  self_str.op_tbl->op_drop(&self_str);

  strbuf_append(buffer, "`: ");
  strbuf_append(buffer, __x86_64_type_enum_str(self->type));
  strbuf_append(buffer, " to type ");
  strbuf_append(buffer, __x86_64_type_enum_str(type));
  __x86_64_proxy_string_init(&error_value, (uint8_t *)strbuf_data(buffer));
  strbuf_free(buffer);

  __x86_64_proxy_error_init(out, &error_value);
}

uint64_t __x86_64_proxy_value_as_index(const x86_64_value *self) {
  switch (self->type) {
    case X86_64_TYPE_BYTE: {
      x86_64_data_byte data = *(x86_64_data_byte *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_CHAR: {
      x86_64_data_char data = *(x86_64_data_char *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_INT: {
      x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_LONG: {
      x86_64_data_long data = *(x86_64_data_long *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_UINT: {
      x86_64_data_uint data = *(x86_64_data_uint *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_ULONG: {
      x86_64_data_ulong data = *(x86_64_data_ulong *)&self->data_raw;
      return data.value;
    }
    case X86_64_TYPE_ARRAY:
    case X86_64_TYPE_BOOL:
    case X86_64_TYPE_ERROR:
    case X86_64_TYPE_OBJECT:
    case X86_64_TYPE_STRING:
    case X86_64_TYPE_STRING_ELEM_REF:
    case X86_64_TYPE_VALUE_REF:
    case X86_64_TYPE_VOID:
    default:
      return UINT64_MAX;
  }
}

void __x86_64_proxy_op_error_not_number(x86_64_value *out, x86_64_value *self,
                                        const char *op) {
  strbuf *buffer = strbuf_new(64, 0);

  x86_64_value self_string;
  self->op_tbl->op_repr(&self_string, self);

  strbuf_append(buffer, "op ");
  strbuf_append(buffer, op);
  strbuf_append(buffer, " `");
  strbuf_append(buffer, (char *)self_string.data_ptr);
  strbuf_append(buffer, "`: ");
  strbuf_append(buffer, __x86_64_type_enum_str(self->type));
  strbuf_append(buffer, " can't convert to number (or uint64_t max) `");

  self->op_tbl->op_drop(&self_string);

  x86_64_value error_value;
  __x86_64_proxy_string_init(&error_value, (uint8_t *)strbuf_data(buffer));

  strbuf_free(buffer);

  x86_64_value error;
  __x86_64_proxy_error_init(&error, &error_value);

  *out = error;
}

void __x86_64_proxy_op_call_func_error(x86_64_value *ret, ...) {
  const char *err_msg = "op call is not implemented: error noop func is called";

  error(err_msg);

  x86_64_value error_value;
  __x86_64_proxy_string_init(&error_value, (const uint8_t *)err_msg);
  __x86_64_proxy_error_init(ret, &error_value);
}
