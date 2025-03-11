// #pragma once

// #include "x86_64_core/value.h"

// // UNARY
// x86_64_value __x86_64_plus(x86_64_value lsv);
// x86_64_value __x86_64_minus(x86_64_value lsv);
// x86_64_value __x86_64_not(x86_64_value lsv);
// x86_64_value __x86_64_bit_not(x86_64_value lsv);
// x86_64_value __x86_64_inc(x86_64_value lsv);
// x86_64_value __x86_64_dec(x86_64_value lsv);

// // BINARY
// x86_64_value __x86_64_or(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_and(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_bit_or(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_bit_xor(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_bit_and(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_eq(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_neq(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_less(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_less_eq(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_bit_shl(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_bit_shr(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_add(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_sub(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_mul(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_div(x86_64_value lsv, x86_64_value rsv);
// x86_64_value __x86_64_mod(x86_64_value lsv, x86_64_value rsv);

// // SPECIAL

// // ... -> x86_64_value
// // in array arguments are passed in reserve order
// // so arr[1,2] -> index(arr, 2, 2, 1), x86_64_value
// // last argument is null (indicates end)
// x86_64_value __x86_64_index_get(x86_64_value array, ...);
// void         __x86_64_index_set(x86_64_value from, x86_64_value array, ...);
// x86_64_value __x86_64_cast(x86_64_value value, x86_64_type_enum type);
// void         __x86_64_assign(x86_64_value *lsv, x86_64_value rsv);
// x86_64_value __x86_64_print(x86_64_value value);
// x86_64_value __x86_64_fflush();
