#pragma once

/*
 * Linearized representation of eir (used mostly for lab purpose)
 * with SSA and some optimizations.
 *
 * Looks like this:
 *
 * `value` has fixed size, represents dynamic variable
 * can store type_id, data_size, data_ptr
 *
 * fn main(): ret_type {
 *   ALLOC(_0, ret_type);
 *   ALLOC(_1, unknown);
 *   ... other variables
 *   ALLOC(_3: bool);
 *
 *   // example implementation of ALLOC with ret_type specified as int_64
 *   sub rsp, 32
 *   mov [rbp - 8], 1      // type_id is 1, e.g., for int
 *   mov [rbp - 16], 8     // data_size is 8 bytes
 *   mov rax, rbp - 32     // Calculate rbp - 32 and store it in rax
 *   mov [rbp - 24], rax   // Store the value of rax (rbp - 32) into memory at [rbp - 24]
 *
 *   bb0: {
 *     // can work with dynamic values and constants
 *     STORE(_1, 6_i32);
 *     STORE(_3, EQUALS(_2, _3))
 *
 *     JE(_3, bb1, bb0); // jump if _3 is resolved as True
 *   }
 *
 *   bb1: {
 *     ...code
 *   }
 * }
 */
