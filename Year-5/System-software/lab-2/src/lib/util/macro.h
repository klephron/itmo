#pragma once

#define UNUSED(x) (void)(x)

#define MALLOC(x) ((typeof(x) *)malloc(sizeof(typeof(x))))
