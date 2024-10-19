#!/bin/bash

days_back=10

for i in $(seq 1 $days_back); do
  # Calculate the date range for the current iteration
  after_date=$(date -d "-$(($days_back + 1 - i)) days" '+%Y-%m-%d')
  before_date=$(date -d "-$(($days_back - i)) days" '+%Y-%m-%d')

  # Execute the git log command with the calculated dates
  echo "Logs from $after_date to $before_date:"
  git log --stat --oneline --no-decorate \
    --reverse \
    --after="$after_date" \
    --before="$before_date" \
    | grep -E '(^[0-f]+\s)|files? changed'
  echo "Difference:"
  git diff --shortstat "@{$(($days_back + 1 - i)) day ago}" "@{$(($days_back - i)) day ago}"
  echo "Difference from Today:"
  git diff --shortstat "@{$(($days_back + 1 - i)) day ago}"
  echo # Blank line for better readability
done
