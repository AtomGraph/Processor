#!/bin/bash

curl -f -s \
  -H "Accept: application/n-quads" --head \
  "${BASE_URL}default-subject" \
| grep -q "${STATUS_OK}"