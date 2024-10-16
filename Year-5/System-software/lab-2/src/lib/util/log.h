#pragma once

int _debug(const char *fname, int lineno, const char *fxname, const char *fmt,
          ...);

int _info(const char *fname, int lineno, const char *fxname, const char *fmt,
         ...);

int _error(const char *fname, int lineno, const char *fxname, const char *fmt,
          ...);

#define debug(fmt, ...) (_debug(__FILE__, __LINE__, __FUNCTION__, fmt, ##__VA_ARGS__))
#define info(fmt, ...) (_info(__FILE__, __LINE__, __FUNCTION__, fmt, ##__VA_ARGS__))
#define error(fmt, ...) (_error(__FILE__, __LINE__, __FUNCTION__, fmt, ##__VA_ARGS__))
