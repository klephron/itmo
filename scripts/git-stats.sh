#!/bin/bash

days="${1:-1}"

for i in $(seq 1 $days); do
  # Calculate the date range for the current iteration
  after_date=$(date -d "-$(($days + 1 - i)) days" '+%Y-%m-%d')
  before_date=$(date -d "-$(($days - i)) days" '+%Y-%m-%d')

  # Execute the git log command with the calculated dates
  echo "Logs from $after_date to $before_date:"
  git log --stat --oneline --no-decorate \
    --after="$after_date" \
    --before="$before_date" \
    --reverse \
    --pretty=format:"%h | %ad | %s" \
    --date=format-local:'%H:%M:%S' \
    | grep -E '(^[0-f]+\s)|files? changed'
  echo "Difference:"
  git diff --shortstat "@{$(($days + 1 - i)) day ago}" "@{$(($days - i)) day ago}"
  echo "Cumulative difference:"
  git diff --shortstat "@{$(($days + 1 - i)) day ago}"
  echo # Blank line for better readability
done
