#!/bin/bash
## 仅限linux环境
ps -ef|grep jrasp-daemon|grep -v grep|cut -c 9-15|xargs kill -9
exit 0