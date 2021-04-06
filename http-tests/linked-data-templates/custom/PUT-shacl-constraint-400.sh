#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# expect 400 Bad Request because missing <http://example.com/constrained-predicate> fails SHACLConstrainedType constraint validation

(
curl -w "%{http_code}\n" -f -s \
  -X PUT \
  -H "Accept: application/n-triples" \
  -H "Content-Type: application/n-triples" \
  --data-binary @- \
  "${BASE_URL_WRITABLE}default-subject" <<EOF
<${BASE_URL_WRITABLE}default-subject-post> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://github.com/AtomGraph/Processor/blob/develop/http-tests/custom#SHACLConstrainedType> .
EOF
) \
| grep -q "${STATUS_BAD_REQUEST}"