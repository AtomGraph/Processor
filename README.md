AtomGraph Processor is a server of declarative, read-write Linked Data applications. If you have a triplestore with RDF data that you want to serve Linked Data from, or write RDF over a RESTful HTTP interface, AtomGraph Processor is the only component you need.

What AtomGraph Processor provides for users as out-of-the-box generic features:
* API logic in a single [Linked Data Templates](https://atomgraph.github.io/Linked-Data-Templates/) ontology
* control of RDF input quality with SPARQL-based constraints
* SPARQL endpoint and Graph Store Protocol endpoint
* HTTP content negotiation and caching support
* a separate RDF resource for every application state, following the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) principle

AtomGraph's direct use of semantic technologies results in extemely extensible and flexible design and leads the way towards declarative Web development. You can forget all about broken hyperlinks and concentrate on building great apps on quality data. For more details, see [articles and presentations](https://github.com/AtomGraph/Processor/wiki/Articles-and-presentations) about AtomGraph.

For a compatible frontend framework for end-user applications, see [AtomGraph Web-Client](https://github.com/AtomGraph/Web-Client).

# Getting started

* [how AtomGraph Processor works](https://github.com/AtomGraph/Processor/wiki/How-Processor-works)
* [Linked Data Templates](https://github.com/AtomGraph/Processor/wiki/Linked-Data-Templates)
* [installing AtomGraph Processor](https://github.com/AtomGraph/Processor/wiki/Installation)

For full documentation, see the [wiki index](https://github.com/AtomGraph/Processor/wiki).

# Usage

## Docker

Processor is available from Docker Hub as [`atomgraph/processor`](https://hub.docker.com/r/atomgraph/processor/) image.
It accepts the following environment variables (that become webapp context parameters):

<dl>
    <dt><code>ENDPOINT</code></dt>
    <dd><a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a> endpoint</dd>
    <dd>URI</dd>
    <dt><code>GRAPH_STORE</code></dt>
    <dd><a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store Protocol</a> endpoint</dd>
    <dd>URI</dd>
    <dt><code>ONTOLOGY</code></dt>
    <dd><a href="https://atomgraph.github.io/Linked-Data-Templates/">Linked Data Templates</a> ontology</dd>
    <dd>URI</dd>
    <dt><code>AUTH_USER</code></dt>
    <dd>SPARQL service HTTP Basic auth username</dd>
    <dd>string, optional</dd>
    <dt><code>AUTH_PWD</code></dt>
    <dd>SPARQL service HTTP Basic auth password</dd>
    <dd>string, optional</dd>
    <dt><code>PREEMPTIVE_AUTH</code></dt>
    <dd>use premptive HTTP Basic auth?</dd>
    <dd><code>true</code>/<code>false</code>, optional</dd>
</dl>

If you want to have your ontologies read from a local file rather than their URIs, you can define a custom [location mapping](https://jena.apache.org/documentation/notes/file-manager.html#the-locationmapper-configuration-file) that will be appended to the system location mapping.
The mapping has to be a file in N3 format and mounted to the `/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/custom-mapping.n3` path. Validate the file syntax beforehand to avoid errors.

To enable logging, mount `log4j.properties` file to `/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/log4j.properties`.

### Examples

The examples show Processor running with combinations of
* default and custom LDT ontologies
* local and remote SPARQL services
* Docker commands

However different combinations are supported as well.

#### Default ontology and a local SPARQL service

The [Fuseki example](https://github.com/AtomGraph/Processor/tree/master/examples/fuseki) shows how to run a local Fuseki SPARQL service together with Processor. Fuseki loads RDF dataset from a file. Processor uses a built-in LDT ontology.
It uses the [`docker-compose`](https://docs.docker.com/compose/) command.

Run the Processor container together with [Fuseki](https://hub.docker.com/r/atomgraph/fuseki) container:

    docker-compose up

After that, open one of the following URLs in the browser and you will retrieve RDF descriptions:
* [`http://localhost:8080/`](http://localhost:8080/) - root resource

Alternatively you can run `curl http://localhost:8080/` etc. from shell.

#### Custom ontology and a remote SPARQL service

The [Wikidata example](https://github.com/AtomGraph/Processor/tree/master/examples/wikidata) example shows to run Processor with a custom LDT ontology and a remote SPARQL service.
It uses the [`docker run`](https://docs.docker.com/engine/reference/run/) command.

Run the Processor container (replace `/c/Users/namedgraph/WebRoot/...` paths with your own; the paths have to be _absolute_):

    docker run --rm \
        -p 8080:8080 \
        -e ENDPOINT="https://query.wikidata.org/bigdata/namespace/wdq/sparql" \
        -e GRAPH_STORE="https://query.wikidata.org/bigdata/namespace/wdq/service" \
        -e ONTOLOGY="https://github.com/AtomGraph/Processor/blob/develop/examples/wikidata#" \
        -v "/c/Users/namedgraph/WebRoot/Processor/src/main/resources/log4j.properties":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/log4j.properties" \
        -v "/c/Users/namedgraph/WebRoot/Processor/examples/wikidata/wikidata.ttl":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/org/wikidata/ldt.ttl" \
        -v "/c/Users/namedgraph/WebRoot/Processor/examples/wikidata/location-mapping.n3":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/custom-mapping.n3" \
        atomgraph/processor

After that, open one of the following URLs in the browser and you will retrieve RDF descriptions:
* [`http://localhost:8080/`](http://localhost:8080/) - root resource
* [`http://localhost:8080/birthdays`](http://localhost:8080/birthdays) - 100 people born today
* [`http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581072`](http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581072) - 100 females born today
* [`http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581097`](http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581097) - 100 males born today

Alternatively you can run `curl http://localhost:8080/` etc. from shell.

## Maven

Processor is released on Maven central as [`com.atomgraph:processor`](https://search.maven.org/artifact/com.atomgraph/processor/).

# Datasource

AtomGraph Processor does *not* include an RDF datasource. It queries RDF data on the fly from a SPARQL endpoint using [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/) over HTTP. SPARQL endpoints are provided by most RDF [triplestores](http://en.wikipedia.org/wiki/Triplestore).

The easiest way to set up a SPARQL endpoint on an RDF dataset is Apache Jena [Fuseki](https://jena.apache.org/documentation/fuseki2/) as a Docker container using our [fuseki](https://hub.docker.com/r/atomgraph/fuseki) image. There is also a number of of [public SPARQL endpoints](http://sparqles.ai.wu.ac.at).

For a commercial triplestore with SPARQL 1.1 support see [Dydra](https://dydra.com).

# Test suite

Processor includes a basic HTTP [test suite](https://github.com/AtomGraph/Processor/tree/master/http-tests) for Linked Data Templates.

[![Build status](https://api.travis-ci.org/AtomGraph/Processor.svg?branch=master)](https://travis-ci.org/AtomGraph/Processor)

# Support

Please [report issues](https://github.com/AtomGraph/Processor/issues) if you've encountered a bug or have a feature request.

Commercial consulting, development, and support are available from [AtomGraph](https://atomgraph.com).

# Community

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop AtomGraph and declarative Linked Data architecture in general.
