// #pragma once

// #include <stdint.h>

// /*
// .u_debug_info
// header:
// - uint64_t subroutines_cnt
// - uint64_t files_cnt
// content:
// - subroutines (implicit index - pos in array):
//   - subroutine:
//     - uint64_t address_start
//     - uint64_t address_end
//     - uint64_t address_prologue_end
//     - uint64_t address_epilogue_start
//     // I don't want current to track initialization end (impl specific)
//     - uint64_t strs_index
//     - uint64_t args_cnt
//     - uint64_t vars_cnt
//     - args:
//       - arg:
//         - uint64_t rbp_offset
//         - uint64_t str_index
//     - vars:
//       - var:
//         - uint64_t rbp_offset
//         - uint64_t str_index
// - files (implicit index - pos in array):
//   - file:
//     - uint64_t str_file_index
//   - file:
//     - uint64_t str_file_index
//   - file:
//     - uint64_t str_file_index
// .u_debug_line
// header:
// - uint64_t entries_cnt
// content:
// - [entries: uint64_t address, uint64_t file_num, uint64_t line_num]
// - [entries: uint64_t address, uint64_t file_num, uint64_t line_num]
// - [entries: uint64_t address, uint64_t file_num, uint64_t line_num]

// .u_debug_str
// list of null-terminated strings, index - pos in list (from 0):
// <string1><string2><string3>
//  */

// // DEBUG_INFO
// typedef struct __attribute__((packed)) x86_64_debug_info_struct {
//   uint64_t subroutines_cnt;
//   uint64_t files_cnt;
//   uint8_t  data[0];
// } x86_64_debug_info;

// typedef struct __attribute__((packed)) x86_64_debug_info_sub_struct {
//   uint64_t address_start;
//   uint64_t address_end;
//   uint64_t address_prologue_end;
//   uint64_t address_epilogue_start;
//   uint64_t strs_index;
//   uint64_t args_cnt;
//   uint64_t vars_cnt;
//   uint8_t  data[0];
// } x86_64_debug_info_sub;

// typedef struct __attribute__((packed)) x86_64_debug_info_arg_struct {
//   uint64_t rbp_offset;
//   uint64_t strs_index;
// } x86_64_debug_info_arg;

// typedef struct __attribute__((packed)) x86_64_debug_info_var_struct {
//   uint64_t rbp_offset;
//   uint64_t strs_index;
// } x86_64_debug_info_var;

// typedef struct __attribute__((packed)) x86_64_debug_info_file_struct {
//   uint64_t strs_index;
// } x86_64_debug_info_file;

// // DEBUG_STR
// typedef struct __attribute__((packed)) x86_64_debug_str_struct {
//   uint64_t strs_cnt;
//   uint64_t data[0];
// } x86_64_debug_str;

// typedef struct __attribute__((packed)) x86_64_debug_str_str_struct {
//   uint8_t data[0];
// } x86_64_debug_str_str;

// // DEBUG_LINE
// typedef struct __attribute__((packed)) x86_64_debug_line_struct {
//   uint64_t entries_cnt;
//   uint8_t  data[0];
// } x86_64_debug_line;

// typedef struct __attribute__((packed)) x86_64_debug_line_entry_struct {
//   uint64_t address;
//   uint64_t file_id;
//   uint64_t line;
// } x86_64_debug_line_entry;
