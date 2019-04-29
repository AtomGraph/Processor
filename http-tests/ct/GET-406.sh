#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/not-accepted" \
  "${BASE_URL}default-subject" \
| egrep -q "${STATUS_NOT_ACCEPTABLE}"