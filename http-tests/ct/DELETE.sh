#!/bin/bash

# re-initialize writable dataset

cat ../dataset-write.trig \
| curl -f -s \
  -X PUT \
  --data-binary @- \
  -H "Content-Type: application/trig" \
  "${ENDPOINT_URL_WRITABLE}data" > /dev/null

# delete resource

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}default-subject" \
| grep -q "${STATUS_NO_CONTENT}"

# check that deleted resource is really gone

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL_WRITABLE}default-subject" \
| grep -q "${STATUS_NOT_FOUND}"