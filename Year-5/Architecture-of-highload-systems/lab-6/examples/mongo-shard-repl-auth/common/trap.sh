#!/bin/bash

handle_sigterm() {
  echo "Received SIGTERM. Performing graceful shutdown..."

  if [ -n "$MAIN_PID" ]; then
    kill -TERM "$MAIN_PID" 2>/dev/null
  fi

  exit 0
}

trap handle_sigterm SIGTERM
