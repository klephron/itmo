#pragma once

#include <stdint.h>

uint8_t  read_bool();
uint8_t  read_byte();
uint8_t  read_char();
int32_t  read_int();
uint32_t read_uint();
int64_t  read_long();
uint64_t read_ulong();
uint8_t *read_string();

void write_bool(uint8_t);
void write_byte(uint8_t);
void write_char(uint8_t);
void write_int(int32_t);
void write_uint(uint32_t);
void write_long(int64_t);
void write_ulong(uint64_t);
void write_string(const uint8_t *);
