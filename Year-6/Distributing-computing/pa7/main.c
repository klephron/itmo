#define _GNU_SOURCE

#include "argparse.h"
#include "banking.h"
#include "common.h"
#include "ipc.h"
#include "pa2345.h"

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

typedef enum {
  STATUS_OK = 0,
  STATUS_BAD_ALLOC = 1,
  STATUS_BAD_PIPE = 2,
  STATUS_BAD_FILE = 3,
  STATUS_BAD_FORK = 4,
  STATUS_BAD_TYPE = 5,
  STATUS_BAD_USAGE = 255,
  STATUS_UNEXPECTED = -1,
  STATUS_EOF = 10,
  STATUS_EAGAIN = 11,
} status_t;

typedef struct {
  int proc_n;
  const char *pipes_path;
  const char *events_path;
  int *balances;
} args_t;

typedef struct {
  int r;
  int w;
} pipe_t;

typedef enum {
  PIPE_UNDEFINED = 0,
  PIPE_UNUSED,
  PIPE_READER,
  PIPE_WRITER,
} pipe_type_t;

typedef struct {
  int i;
  int j;
} pipe_idx_t;

typedef timestamp_t local_time_t;

typedef enum {
  CLOCK_CMP_EQ = 0,                           // equal
  CLOCK_CMP_LT = 1 << 0,                      // lower
  CLOCK_CMP_GT = 1 << 1,                      // greater
  CLOCK_CMP_PR = CLOCK_CMP_GT | CLOCK_CMP_LT, // parallel
} clock_cmp_t;

typedef struct {
  const args_t *args;

  FILE *events_f;
  FILE *pipes_f;

  pipe_t *pipes;

  int proc_id;
} ctx_t;

typedef enum {
  STATE_INITED,
  STATE_STARTED,
  STATE_STOPPED,
  STATE_DONE,
} state_t;

typedef enum {
  SNAPSHOT_NONE,
  SNAPSHOT_STARTED,
  SNAPSHOT_ACKED,
} snapshot_t;

typedef struct {
  ctx_t *ctx_rf;

  local_time_t *vec;
  int *stats;

  balance_t balance;

  state_t state;

  snapshot_t snapshot;
} clck_t;

typedef struct {
  ctx_t *ctx;
  int nio; // nonblocking
  int id;  // identifier, from which message is received
} rec_ctx_t;

typedef struct {
  clck_t *clck;
  int nio;
  int id;
} rec_clck_t;

int send_clck(clck_t *self, local_id dst, Message *msg);
int send_multicast_clck(clck_t *self, Message *msg);
int receive_clck(rec_clck_t *self, local_id from, Message *msg,
                 const local_time_t **vec_o);
int receive_any_clck(rec_clck_t *self, Message *msg,
                     const local_time_t **vec_o);

__attribute__((unused)) static const char *msg_type_str(MessageType type) {
  switch (type) {
  case STARTED:
    return "STARTED";
  case DONE:
    return "DONE";
  case ACK:
    return "ACK";
  case STOP:
    return "STOP";
  case TRANSFER:
    return "TRANSFER";
  case BALANCE_HISTORY:
    return "BALANCE_HISTORY";
  case CS_REQUEST:
    return "CS_REQUEST";
  case CS_REPLY:
    return "CS_REPLY";
  case CS_RELEASE:
    return "CS_RELEASE";
  case BALANCE_STATE:
    return "BALANCE_STATE";
  case SNAPSHOT_VTIME:
    return "SNAPSHOT_VTIME";
  case SNAPSHOT_ACK:
    return "SNAPSHOT_ACK";
  case EMPTY:
    return "EMPTY";
  }
  return "UNKNOWN";
}

static void error_send(const ctx_t *ctx, const Message *msg) {
  fprintf(stderr, "error: %i unable to send %s\n", ctx->proc_id,
          msg_type_str(msg->s_header.s_type));
}

static void error_send_multicast(const ctx_t *ctx, const Message *msg) {
  fprintf(stderr, "error: %i unable to send_multicast %s\n", ctx->proc_id,
          msg_type_str(msg->s_header.s_type));
}

static void error_receive_not(const ctx_t *ctx) {
  fprintf(stderr, "error: %i unable to receive\n", ctx->proc_id);
}

static void error_receive_type(const ctx_t *ctx, const Message *msg,
                               MessageType expected) {
  fprintf(stderr, "error: %i received %s, expected %s\n", ctx->proc_id,
          msg_type_str(msg->s_header.s_type), msg_type_str(expected));
}

static int check_receive(const ctx_t *ctx, const Message *msg, int status,
                         MessageType type) {
  if (status) {
    error_receive_not(ctx);
    return status;
  }
  if (msg->s_header.s_type != type) {
    error_receive_type(ctx, msg, type);
    return STATUS_BAD_TYPE;
  }
  return STATUS_OK;
}

static void args_deinit(args_t *args) {
  if (args->balances) {
    free(args->balances);
    args->balances = NULL;
  }
}

static int args_parse(args_t *args, int argc, const char **argv) {
  static const char *const s_usages[] = {
      "app [options] balances...\n"
      "    balances...     list of integer process balances, length\n"
      "                    must be equal to number of processes",
      NULL,
  };

  struct argparse_option options[] = {
      OPT_HELP(),
      OPT_GROUP("Options:"),
      OPT_INTEGER('p', "procs", &args->proc_n, "number of processes"),
      OPT_END(),
  };

  struct argparse argparse;
  argparse_init(&argparse, options, s_usages, ARGPARSE_STOP_AT_NON_OPTION);
  argparse_describe(&argparse, NULL, NULL);

  argc = argparse_parse(&argparse, argc, argv);

  if (argc != args->proc_n) {
    argparse_usage(&argparse);
    return STATUS_BAD_USAGE;
  }

  args->balances = (int *)malloc(sizeof(int) * args->proc_n);
  if (!args->balances) {
    return STATUS_BAD_ALLOC;
  }

  for (int i = 0; i < args->proc_n; ++i) {
    char *endptr;
    long balance = strtol(argv[i], &endptr, 10);

    if (*endptr != '\0' || balance > INT_MAX || balance < INT_MIN) {
      args_deinit(args);

      argparse_usage(&argparse);
      return STATUS_BAD_USAGE;
    }

    args->balances[i] = balance;
  }

  return STATUS_OK;
}

static void logs_close(ctx_t *ctx) {
  if (ctx->events_f) {
    fclose(ctx->events_f);
    ctx->events_f = NULL;
  }
  if (ctx->pipes_f) {
    fclose(ctx->pipes_f);
    ctx->events_f = NULL;
  }
}

static int logs_open(ctx_t *ctx) {
  int status = 0;

  if (!(ctx->events_f = fopen(ctx->args->events_path, "w"))) {
    status = STATUS_BAD_FILE;
  }
  if (!(ctx->pipes_f = fopen(ctx->args->pipes_path, "w"))) {
    status = STATUS_BAD_FILE;
  }

  if (status) {
    logs_close(ctx);
  }

  return status;
}

static void logs_event_write(ctx_t *ctx, char *data, int size) {
  fwrite(data, sizeof(*data), size, stdout);
  fwrite(data, sizeof(*data), size, ctx->events_f);
}

static int pipes_n(const ctx_t *ctx) {
  return ctx->args->proc_n + 1;
}

static pipe_t *pipes_select(const ctx_t *ctx, pipe_idx_t idx) {
  return ctx->pipes + ((idx.i * pipes_n(ctx)) + idx.j);
}

static pipe_idx_t pipes_idx(const ctx_t *ctx, pipe_t *p) {
  const int n = pipes_n(ctx);

  int idx = p - ctx->pipes;
  int i = idx / n;
  int j = idx % n;
  return (pipe_idx_t){.i = i, .j = j};
}

static void pipes_close1(pipe_t *p) {
  close(p->r);
  close(p->w);
}

static pipe_type_t pipes_type(ctx_t *ctx, pipe_idx_t idx) {
  if (idx.i == idx.j) {
    return PIPE_UNDEFINED;
  }
  if (ctx->proc_id == idx.i) {
    return PIPE_WRITER;
  }
  if (ctx->proc_id == idx.j) {
    return PIPE_READER;
  }
  return PIPE_UNUSED;
}

static pipe_type_t pipes_type_pipe(ctx_t *ctx, pipe_t *p) {
  return pipes_type(ctx, pipes_idx(ctx, p));
}

static void pipes_close(ctx_t *ctx) {
  const int n = pipes_n(ctx);

  if (ctx->pipes) {
    for (int i = 0; i < n; ++i) {
      for (int j = 0; j < n; ++j) {
        pipe_idx_t idx = {.i = i, .j = j};

        if (pipes_type(ctx, idx) == PIPE_UNDEFINED) {
          continue;
        }

        pipe_t *p = pipes_select(ctx, idx);
        pipes_close1(p);
      }
    }
    free(ctx->pipes);
    ctx->pipes = NULL;
  }
}

static int pipes_open(ctx_t *ctx) {
  const int n = pipes_n(ctx);

  ctx->pipes = calloc(1, sizeof(*ctx->pipes) * n * n);

  if (!ctx->pipes) {
    return STATUS_BAD_ALLOC;
  }

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      pipe_idx_t idx = {.i = i, .j = j};

      if (pipes_type(ctx, idx) == PIPE_UNDEFINED) {
        continue;
      }

      pipe_t *p = pipes_select(ctx, idx);
      if (pipe2((int *)p, O_NONBLOCK)) {
        goto err;
      }
      fprintf(ctx->pipes_f, "%d/%d\n", p->r, p->w);
    }
  }

  fflush(ctx->pipes_f);
  return 0;

err:
  fflush(ctx->pipes_f);
  pipes_close(ctx);
  return STATUS_BAD_PIPE;
}

// closes and configures all pipes
static void pipes_configure(ctx_t *ctx) {
  const int n = pipes_n(ctx);

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      pipe_idx_t idx = (pipe_idx_t){.i = i, .j = j};
      pipe_t *p = pipes_select(ctx, idx);

      switch (pipes_type(ctx, idx)) {
      case PIPE_UNDEFINED: {
        break;
      }
      case PIPE_READER: {
        close(p->w);
        break;
      }
      case PIPE_WRITER: {
        close(p->r);
        break;
      }
      case PIPE_UNUSED: {
        close(p->r);
        close(p->w);
        break;
      }
      }
    }
  }
}

// match proc_id with idx cuz they may be encoded differently
static pipe_t *pipes_select_proc_id(ctx_t *ctx, int id_from, int id_to) {
  int i = id_from;
  int j = id_to;
  return pipes_select(ctx, (pipe_idx_t){.i = i, .j = j});
}

static int pipes_writer_fd(ctx_t *ctx, int id_to) {
  pipe_t *p = pipes_select_proc_id(ctx, ctx->proc_id, id_to);

  assert(pipes_type_pipe(ctx, p) == PIPE_WRITER);

  return p->w;
}

static int pipes_reader_fd(ctx_t *ctx, int id_from) {
  pipe_t *p = pipes_select_proc_id(ctx, id_from, ctx->proc_id);

  assert(pipes_type_pipe(ctx, p) == PIPE_READER);

  return p->r;
}

static int clck_n(const clck_t *clck) {
  return clck->ctx_rf->args->proc_n;
}

static void clck_deinit(clck_t *clck) {
  clck->ctx_rf = NULL;

  if (clck->vec) {
    free(clck->vec);
    clck->vec = NULL;
  }
  if (clck->stats) {
    free(clck->stats);
    clck->stats = NULL;
  }

  clck->balance = 0;
  clck->snapshot = SNAPSHOT_NONE;
}

static int clck_init(clck_t *clck, ctx_t *ctx_rf, balance_t balance) {
  clck->ctx_rf = ctx_rf;

  const int n = clck_n(clck);

  clck->vec = (local_time_t *)malloc(sizeof(local_time_t) * n);
  if (!clck->vec) {
    goto err;
  }

  clck->stats = (int *)malloc(sizeof(int) * n);
  if (!clck->stats) {
    goto err;
  }

  for (int i = 0; i < n; ++i) {
    clck->vec[i] = 0;
    clck->stats[i] = 0;
  }

  clck->balance = balance;
  clck->state = STATE_INITED;
  clck->snapshot = SNAPSHOT_NONE;

  return 0;

err:
  clck_deinit(clck);
  return STATUS_BAD_ALLOC;
}

static int clck_by_proc_id(int proc_id) {
  return proc_id - 1;
}

__attribute__((unused)) static void clck_print(const clck_t *clck) {
  const int n = clck_n(clck);

  int width = snprintf(NULL, 0, "%d", n);

  printf("%*d: ", width, clck->ctx_rf->proc_id);
  printf("vec=[");
  for (int i = 0; i < n; ++i) {
    if (i) {
      printf(", ");
    }
    printf("%d", clck->vec[i]);
  }
  printf("], stats=[");
  for (int i = 0; i < n; ++i) {
    if (i) {
      printf(", ");
    }
    printf("%d", clck->stats[i]);
  }
  printf("]\n");
}

static clock_cmp_t clck_syn(clck_t *clck, const local_time_t *vec) {
  const int n = clck_n(clck);

  clock_cmp_t cmp = CLOCK_CMP_EQ;

  for (int i = 0; i < n; ++i) {
    if (clck->vec[i] < vec[i]) {
      clck->vec[i] = vec[i];
      cmp |= CLOCK_CMP_LT;
    } else if (clck->vec[i] > vec[i]) {
      cmp |= CLOCK_CMP_GT;
    }
  }

  return cmp;
}

static void clck_inc(clck_t *clck) {
  int id = clck_by_proc_id(clck->ctx_rf->proc_id);
  clck->vec[id]++;
}

static void clck_msg_send(clck_t *clck, Message *msg) {
  const int n = clck_n(clck);

  for (int i = 0; i < n; ++i) {
    msg->s_header.s_local_timevector[i] = clck->vec[i];
  }
}

static void clck_msg_receive(Message *msg, int status,
                             const local_time_t **vec_o) {
  if (status == STATUS_OK) {
    *vec_o = msg->s_header.s_local_timevector;
  } else {
    *vec_o = NULL;
  }
}

// counts number of events
static local_time_t clck_time(const clck_t *clck) {
  const int n = clck_n(clck);

  local_time_t time = 0;
  for (int i = 0; i < n; ++i) {
    time += clck->vec[i];
  }

  return time;
}

static rec_ctx_t clck_rec_t(rec_clck_t *rec) {
  return (rec_ctx_t){
      .ctx = rec->clck->ctx_rf,
      .id = rec->id,
      .nio = rec->nio,
  };
}

static void clck_activate(clck_t *clck, int proc_id) {
  clck->stats[clck_by_proc_id(proc_id)] = 1;
}

static void clck_deactivate(clck_t *clck, int proc_id) {
  clck->stats[clck_by_proc_id(proc_id)] = 0;
}

static int clck_active_n(const clck_t *clck) {
  const int n = clck_n(clck);
  int c = 0;
  for (int i = 0; i < n; ++i) {
    c += clck->stats[i];
  }
  return c;
}

static int parent_id() {
  return 0;
}

static int parent_is(ctx_t *ctx) {
  return ctx->proc_id == 0;
}

static int parent_fn(ctx_t *ctx) {
  int n = ctx->args->proc_n;

  Message msg = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
          },
      .s_payload = {0},
  };

  pipes_configure(ctx);

  {
    for (int i = 1; i <= n; ++i) {
      int s = receive(&(rec_ctx_t){.ctx = ctx, .nio = 0}, i, &msg);
      check_receive(ctx, &msg, s, STARTED);
    }
  }

  {
    local_id max_id = ctx->args->proc_n;
    bank_robbery(ctx, max_id);

    msg.s_header.s_type = STOP;
    msg.s_header.s_payload_len = 0;

    if (send_multicast(ctx, &msg)) {
      error_send_multicast(ctx, &msg);
    }
  }

  {
    for (int i = 1; i <= n; ++i) {
      int s = receive(&(rec_ctx_t){.ctx = ctx, .nio = 0}, i, &msg);
      check_receive(ctx, &msg, s, DONE);
    }
  }

  int wpid;
  int status = 0;

  while ((wpid = wait(&status)) > 0)
    ;

  return 0;
}

static int child_handle(clck_t *clck, Message *msg, int proc_jd,
                        const local_time_t *vec_proc_jd) {
  const int proc_id = clck->ctx_rf->proc_id;
  MessageType type = msg->s_header.s_type;

  switch (type) {
  case STOP: {
    clck_inc(clck);

    if (proc_jd != parent_id()) {
      clck_deactivate(clck, proc_jd);
      break;
    }

    // multicast (without parent) send STOP messages to notify that we are
    // stopping. FIFO will help us go to DONE state

    clck->state = STATE_STOPPED;

    clck_deactivate(clck, proc_id);

    const int n = clck->ctx_rf->args->proc_n;

    clck_inc(clck);
    for (int id = 1; id <= n; ++id) {
      if (id == proc_id) {
        continue;
      }
      if (send_clck(clck, id, msg)) {
        error_send(clck->ctx_rf, msg);
      }
    }

    break;
  }
  case TRANSFER: {
    clck_syn(clck, vec_proc_jd);
    clck_inc(clck);

    const TransferOrder *order_rf = (const TransferOrder *)msg->s_payload;

    // to src
    if (order_rf->s_src == proc_id) {
      clck->balance -= order_rf->s_amount;

      clck_inc(clck);
      if (send_clck(clck, order_rf->s_dst, msg)) {
        error_send(clck->ctx_rf, msg);
      }

      int len = sprintf(msg->s_payload, log_transfer_out_fmt, clck_time(clck),
                        proc_id, order_rf->s_amount, order_rf->s_dst);
      logs_event_write(clck->ctx_rf, msg->s_payload, len);
      break;
    }

    // to dst
    if (order_rf->s_dst == proc_id) {
      clck->balance += order_rf->s_amount;

      msg->s_header.s_type = ACK;
      msg->s_header.s_payload_len = 0;

      clck_inc(clck);
      if (send(clck->ctx_rf, parent_id(), msg)) {
        error_send(clck->ctx_rf, msg);
      }

      int len = sprintf(msg->s_payload, log_transfer_in_fmt, clck_time(clck),
                        proc_id, order_rf->s_amount, order_rf->s_src);
      logs_event_write(clck->ctx_rf, msg->s_payload, len);
      break;
    }

    fprintf(stderr,
            "error: %d received not its transfer order (%d -> %d) $%d\n",
            proc_id, order_rf->s_src, order_rf->s_dst, order_rf->s_amount);

    return STATUS_UNEXPECTED;
  }
  default:
    fprintf(stderr, "unexpected type in %i handling %s\n", proc_id,
            msg_type_str(type));
    return STATUS_BAD_TYPE;
  }

  if (clck->state == STATE_STOPPED && !clck_active_n(clck)) {
    clck->state = STATE_DONE;
  }

  return STATUS_OK;
}

static int child_yield_active(clck_t *clck, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  const int proc_id = clck->ctx_rf->proc_id;
  const int n = clck->ctx_rf->args->proc_n;

  const local_time_t *vec_proc_jd = NULL;

  while (1) {
    for (int id = 0; id <= n; ++id) {
      // receive only from actives or parent, not to read DONE msg
      if (proc_id == id || (id != 0 && !clck->stats[clck_by_proc_id(id)])) {
        continue;
      }

      int s = receive_clck(&(rec_clck_t){.clck = clck, .nio = 1}, id, msg,
                           &vec_proc_jd);
      if (s) {
        continue;
      }

      return child_handle(clck, msg, id, vec_proc_jd);
    }

    nanosleep(&s_ts, NULL);
  }
}

static int child_fn(ctx_t *ctx, balance_t balance) {
  const int n = ctx->args->proc_n;
  const int proc_id = ctx->proc_id;

  const local_time_t *vec_proc_jd = NULL;

  Message msg = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
          },
      .s_payload = {0},
  };

  clck_t clck;

  pipes_configure(ctx);

  clck_init(&clck, ctx, balance);

  {
    clck_inc(&clck);
    int len = sprintf(msg.s_payload, log_started_fmt, clck_time(&clck), proc_id,
                      getpid(), getppid(), clck.balance);
    msg.s_header.s_type = STARTED;
    msg.s_header.s_payload_len = len;

    if (send_multicast_clck(&clck, &msg)) {
      error_send_multicast(ctx, &msg);
    }

    logs_event_write(ctx, msg.s_payload, len);

    clck_activate(&clck, proc_id);
  }

  {
    for (int i = 1; i <= n; ++i) {
      if (i == proc_id) {
        continue;
      }

      int s = receive_clck(&(rec_clck_t){.clck = &clck, .nio = 0}, i, &msg,
                           &vec_proc_jd);

      check_receive(ctx, &msg, s, STARTED);

      clck_syn(&clck, vec_proc_jd);
      clck_inc(&clck);

      clck_activate(&clck, i);
    }

    int len = sprintf(msg.s_payload, log_received_all_started_fmt,
                      clck_time(&clck), proc_id);

    logs_event_write(ctx, msg.s_payload, len);
  }

  {
    clck.state = STATE_STARTED;

    while (clck.state != STATE_DONE) {
      child_yield_active(&clck, &msg);
    }
  }

  {
    clck_inc(&clck);
    int len = sprintf(msg.s_payload, log_done_fmt, clck_time(&clck), proc_id,
                      clck.balance);
    msg.s_header.s_type = DONE;
    msg.s_header.s_payload_len = len;

    if (send_multicast_clck(&clck, &msg)) {
      error_send_multicast(ctx, &msg);
    }

    logs_event_write(ctx, msg.s_payload, len);
  }

  {
    for (int i = 1; i <= n; ++i) {
      if (i == proc_id) {
        continue;
      }

      int s;
      do {
        s = receive_clck(&(rec_clck_t){.clck = &clck, .nio = 0}, i, &msg,
                         &vec_proc_jd);
        if (s) {
          error_receive_not(ctx);
          break;
        }

        clck_syn(&clck, vec_proc_jd);
        clck_inc(&clck);
      } while (msg.s_header.s_type != DONE);
    }

    int len = sprintf(msg.s_payload, log_received_all_done_fmt,
                      clck_time(&clck), proc_id);

    logs_event_write(ctx, msg.s_payload, len);
  }

  clck_deinit(&clck);

  return STATUS_OK;
}

static void child_configure(ctx_t *ctx, int proc_id) {
  ctx->proc_id = proc_id;
}

static int child_run_all(ctx_t *ctx) {
  for (int id = 1; id <= ctx->args->proc_n; ++id) {
    int pid = fork();

    if (pid == 0) {
      child_configure(ctx, id);
      return child_fn(ctx, ctx->args->balances[id - 1]);
    } else if (pid < 0) {
      return STATUS_BAD_FORK;
    }
  }
  return STATUS_OK;
}

int send(void *self, local_id dst, const Message *msg) {
  ctx_t *ctx = self;

  int wfd = pipes_writer_fd(ctx, dst);
  int s = write(wfd, msg, msg->s_header.s_payload_len + sizeof(MessageHeader));

  return s == -1 ? -1 : STATUS_OK;
}

int send_clck(clck_t *self, local_id dst, Message *msg) {
  clck_msg_send(self, msg);
  return send(self->ctx_rf, dst, msg);
}

int send_multicast(void *self, const Message *msg) {
  ctx_t *ctx = self;
  int n = ctx->args->proc_n;

  int status = STATUS_OK;

  // to children and parent
  for (int id_to = 0; id_to <= n; ++id_to) {
    if (ctx->proc_id == id_to) {
      continue;
    }
    if (send(ctx, id_to, msg) == -1) {
      status = -1;
    }
  }

  return status;
}

int send_multicast_clck(clck_t *self, Message *msg) {
  clck_msg_send(self, msg);
  return send_multicast(self->ctx_rf, msg);
}

int receive(void *self, local_id from, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  rec_ctx_t *rec = self;
  ctx_t *ctx = rec->ctx;
  int rfd = pipes_reader_fd(ctx, from);

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  while (1) {
    int s = read(rfd, msg, sizeof(MessageHeader));

    if (s == -1) {
      if (rec->nio && errno == EAGAIN) {
        return STATUS_EAGAIN;
      }
      nanosleep(&s_ts, NULL);
      continue;
    }
    if (s == 0) {
      return STATUS_EOF;
    }

    if (msg->s_header.s_payload_len) {
      s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
      if (s <= 0) {
        return -1;
      }
    }
    return STATUS_OK;
  }
}

int receive_clck(rec_clck_t *self, local_id from, Message *msg,
                 const local_time_t **vec_o) {
  rec_ctx_t rec = clck_rec_t(self);
  int status = receive(&rec, from, msg);
  clck_msg_receive(msg, status, vec_o);
  return status;
}

int receive_any(void *self, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  rec_ctx_t *rec = self;
  ctx_t *ctx = rec->ctx;
  const int n = ctx->args->proc_n;
  const int n_eofs = n;
  const int id_start = 0;

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  for (int id = id_start, eofs = 0; eofs < n_eofs; ++id) {
    // sleep and loop again
    if (id > n) {
      if (rec->nio) {
        return STATUS_EAGAIN;
      }
      id = id_start;
      eofs = 0;
      nanosleep(&s_ts, NULL);
    }

    if (id == ctx->proc_id) {
      continue;
    }

    int rfd = pipes_reader_fd(ctx, id);
    int s = read(rfd, msg, sizeof(MessageHeader));

    if (s == 0) {
      ++eofs;
      continue;
    }
    if (s == -1) {
      continue;
    }

    if (msg->s_header.s_payload_len) {
      s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
      if (s <= 0) {
        return -1;
      }
    }

    rec->id = id;

    return STATUS_OK;
  }
  return STATUS_EOF;
}

int receive_any_clck(rec_clck_t *self, Message *msg,
                     const local_time_t **vec_o) {
  rec_ctx_t rec = clck_rec_t(self);
  int status = receive_any(&rec, msg);
  clck_msg_receive(msg, status, vec_o);
  return status;
}

void transfer(void *parent_data, local_id src, local_id dst, balance_t amount) {
  ctx_t *ctx = parent_data;

  Message msg = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
              .s_payload_len = sizeof(TransferOrder),
              .s_type = TRANSFER,
          },
  };

  TransferOrder *order_rf = (void *)msg.s_payload;
  order_rf->s_src = src;
  order_rf->s_dst = dst;
  order_rf->s_amount = amount;

  if (send(ctx, src, &msg)) {
    error_send(ctx, &msg);
  }

  int s = receive(&(rec_ctx_t){.ctx = ctx, .nio = 0}, dst, &msg);
  check_receive(ctx, &msg, s, ACK);
}

void total_sum_snapshot(void *parent_data) {
  ctx_t *ctx = parent_data;

  Message msg = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
              .s_payload_len = 0,
              .s_type = BALANCE_STATE,
          },
  };

  local_id min_id = 1;
  local_id max_id = ctx->args->proc_n;

  if (max_id == 0) {
    fprintf(stderr, "error: %d noone to send snapshot request to\n",
            ctx->proc_id);
    return;
  }

  local_id src = rand() % (max_id + 1 - min_id) + min_id;

  if (send(ctx, src, &msg)) {
    error_send(ctx, &msg);
  }
}

int main(int argc, const char **argv) {
  int status;

  args_t args = {
      .proc_n = 0,
      .events_path = events_log,
      .pipes_path = pipes_log,
      .balances = NULL,
  };
  ctx_t ctx = {
      .args = &args,
      .events_f = NULL,
      .pipes_f = NULL,
      .pipes = NULL,
      .proc_id = parent_id(),
  };

  if ((status = args_parse(&args, argc, argv))) {
    return status;
  }

  if ((status = logs_open(&ctx))) {
    goto cleanup;
  }

  if ((status = pipes_open(&ctx))) {
    goto cleanup;
  }

  if ((status = child_run_all(&ctx))) {
    goto cleanup;
  }

  if (parent_is(&ctx)) {
    status = parent_fn(&ctx);
  }

cleanup:
  pipes_close(&ctx);

  logs_close(&ctx);

  args_deinit(&args);

  return status;
}
