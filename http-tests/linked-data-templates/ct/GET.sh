#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL}default-subject" \
| grep -q "${STATUS_OK}"