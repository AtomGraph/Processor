#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL}named-subject" \
| grep -q "${STATUS_OK}"