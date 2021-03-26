#!/bin/bash

### Signal handlers ###

function handle_signal {
    case "$1" in
      TERM|INT|EXIT)
        if [ -n "$CMD_PID" ]; then
          kill "$CMD_PID" &>/dev/null
          sleep 1
        fi
        
        echo "Exiting ..." >&2
        exit 0
        ;;
      *)
        echo "Terminating abnormally" >&2
        exit 1
        ;;
    esac
}

function ignore_signal {
  log "Caught signal $1 - ignored" >&2
}

trap "handle_signal TERM" "TERM"
trap "handle_signal INT" "INT"
trap "ignore_signal HUP" "HUP"

### Sleeper function ###

# $1 process PID
function wait_to_finish {
    while true; do
        sleep 1 &
        PID=$!

        if ! wait $PID ; then
            kill $PID &>/dev/null
        fi

        if ! ps -p "$1" > /dev/null ; then # process not running anymore
           break; # exit while loop
        fi
    done
}

### Arguments ###

# context variables are used in $CATALINA_HOME/conf/Catalina/localhost/ROOT.xml

if [ -z "$ENDPOINT" ] ; then
    echo '$ENDPOINT not set'
    exit 1
fi
if [ -z "$GRAPH_STORE" ] ; then
    echo '$GRAPH_STORE not set'
    exit 1
fi
if [ -z "$ONTOLOGY" ] ; then
    echo '$ONTOLOGY not set'
    exit 1
fi

# if user-defined location mapping exists, append it to system location mapping

if [ -f "$CUSTOM_LOCATION_MAPPING" ] ; then
    cat "$CUSTOM_LOCATION_MAPPING" >> "$LOCATION_MAPPING"
    cat "$LOCATION_MAPPING"
fi

# set Context variables (which are used in $CATALINA_HOME/conf/Catalina/localhost/ROOT.xml)

if [ -n "$ENDPOINT" ] ; then
    ENDPOINT_PARAM="--stringparam sd:endpoint $ENDPOINT "
fi
if [ -n "$GRAPH_STORE" ] ; then
    GRAPH_STORE_PARAM="--stringparam a:graphStore $GRAPH_STORE "
fi
if [ -n "$ONTOLOGY" ] ; then
    ONTOLOGY_PARAM="--stringparam ldt:ontology $ONTOLOGY "
fi
if [ -n "$AUTH_USER" ] ; then
    AUTH_USER_PARAM="--stringparam a:authUser $AUTH_USER "
fi
if [ -n "$AUTH_PWD" ] ; then
    AUTH_PWD_PARAM="--stringparam a:authPwd $AUTH_PWD "
fi
if [ -n "$PREEMPTIVE_AUTH" ] ; then
    PREEMPTIVE_AUTH_PARAM="--stringparam a:preemptiveAuth $PREEMPTIVE_AUTH "
fi

### Execution ###

# $CATALINA_HOME must be the WORKDIR at this point

transform="xsltproc \
  --output conf/Catalina/localhost/ROOT.xml \
  $ENDPOINT_PARAM \
  $GRAPH_STORE_PARAM \
  $ONTOLOGY_PARAM \
  $AUTH_USER_PARAM \
  $AUTH_PWD_PARAM \
  $PREEMPTIVE_AUTH_PARAM \
  conf/Catalina/localhost/context.xsl \
  conf/Catalina/localhost/ROOT.xml"

eval "$transform"

# run Tomcat process in the background

if [ -z "$JPDA_ADDRESS" ] ; then
    catalina.sh run &
else
    catalina.sh jpda run &
fi

CMD_PID=$!
wait_to_finish $CMD_PID