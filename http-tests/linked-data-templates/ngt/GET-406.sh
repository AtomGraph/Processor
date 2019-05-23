#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/not-accepted" \
  "${BASE_URL}named-subject" \
| grep -q "${STATUS_NOT_ACCEPTABLE}"