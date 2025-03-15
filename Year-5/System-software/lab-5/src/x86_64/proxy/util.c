#include "util.h"

#include "util/strbuf.h"
#include "x86_64/proxy/value/error.h"
#include "x86_64/proxy/value/string.h"
#include "x86_64/util.h"

void __x86_64_proxy_op_error_undefined(x86_64_value *out, x86_64_value *self,
                                       const char *op) {
  strbuf *buffer = strbuf_new(64, 0);

  x86_64_value self_string;
  self->op_tbl->op_repr(&self_string, self);

  strbuf_append(buffer, "op ");
  strbuf_append(buffer, op);
  strbuf_append(buffer, " is undefined for '");
  strbuf_append(buffer, (char *)self_string.data_ptr);
  strbuf_append(buffer, "': ");
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
  strbuf_append(buffer, " got '");
  strbuf_append(buffer, (char *)self_string.data_ptr);
  strbuf_append(buffer, "': ");

  strbuf_append(buffer, __x86_64_type_enum_str(type));

  self->op_tbl->op_drop(&self_string);

  x86_64_value error_value;
  __x86_64_proxy_string_init(&error_value, (uint8_t *)strbuf_data(buffer));

  strbuf_free(buffer);

  x86_64_value error;
  __x86_64_proxy_error_init(&error, &error_value);

  *out = error;
}
