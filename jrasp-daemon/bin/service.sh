#!/bin/sh

while true
do
  ## issue 41
  process=$(ps aux | grep -E "jrasp-daemon$" | grep -v grep)
  if [ -z "$process" ]; then
    echo "jrasp-daemon is not running"
    ./jrasp-daemon
  else
    sleep 10
    echo "jrasp-daemon is running"
  fi
done
