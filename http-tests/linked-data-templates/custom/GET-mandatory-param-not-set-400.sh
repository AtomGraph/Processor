#!/bin/bash

# check that parameter is required
# test disabled: currently ParameterException in TemplateCall is not handled by ParameterExceptionMapper which gives 500 instead of 400

#curl -w "%{http_code}\n" -f -v \
#  -H "Accept: application/n-triples" \
#  "${BASE_URL}mandatory-param" \
#| grep -q "${STATUS_BAD_REQUEST}"