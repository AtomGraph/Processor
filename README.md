AtomGraph Processor is a server of declarative, read-write Linked Data applications. If you have a triplestore with RDF data that you want to serve Linked Data from, or write RDF over a RESTful HTTP interface, AtomGraph Processor is the only component you need.

What AtomGraph Processor provides for users as out-of-the-box generic features:
* API logic in a single [Linked Data Templates](https://atomgraph.github.io/Linked-Data-Templates/) ontology
* control of RDF input quality with SPARQL-based constraints
* SPARQL endpoint and Graph Store Protocol endpoint
* HTTP content negotiation and caching support

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

The [Fuseki example](https://github.com/AtomGraph/Processor/tree/master/examples/fuseki) shows how to run a local [Fuseki](https://jena.apache.org/documentation/fuseki2/) SPARQL service together with Processor and how to setup [nginx](https://www.nginx.com) as a reverse proxy in front of Processor. Fuseki loads RDF dataset from a file. Processor uses a built-in LDT ontology.
It uses the [`docker-compose`](https://docs.docker.com/compose/) command.

Run the Processor container together with [Fuseki](https://hub.docker.com/r/atomgraph/fuseki) and [nginx](https://hub.docker.com/_/nginx) container:

    cd examples/fuseki
    
    docker-compose up

After that, open one of the following URLs in the browser and you will retrieve RDF descriptions:
* [`http://localhost:8080/`](http://localhost:8080/) - root resource
* [`http://localhost/`](http://localhost/) - root resource where the hostname of the Processor's base URI is rewritten to `example.org`

Alternatively you can run `curl http://localhost:8080/` etc. from shell.

In this setup Processor is also available on `http://localhost/` which is the nginx host.
The internal hostname rewriting is done by nginx and useful in situations when the Processor hostname is different from the application's dataset base URI and SPARQL queries do not match any triples.
The [dataset](https://github.com/AtomGraph/Processor/blob/master/examples/fuseki/dataset.ttl) for this example contains a second `http://example.org/` base URI, which works with the rewritten `example.org` hostname.

#### Custom ontology and a remote SPARQL service

The [Wikidata example](https://github.com/AtomGraph/Processor/tree/master/examples/wikidata) example shows to run Processor with a custom LDT ontology and a remote SPARQL service.
It uses the [`docker run`](https://docs.docker.com/engine/reference/run/) command.

Run the Processor container with the Wikidata example:

    cd examples/wikidata
    
    docker-compose up

After that, open one of the following URLs in the browser and you will retrieve RDF descriptions:
* [`http://localhost:8080/`](http://localhost:8080/) - root resource
* [`http://localhost:8080/birthdays`](http://localhost:8080/birthdays) - 100 people born today
* [`http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581072`](http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581072) - 100 females born today
* [`http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581097`](http://localhost:8080/birthdays?sex=http%3A%2F%2Fwww.wikidata.org%2Fentity%2FQ6581097) - 100 males born today

Alternatively you can run `curl http://localhost:8080/` etc. from shell.

_Note that Wikidata's SPARQL endpoint [`https://query.wikidata.org/bigdata/namespace/wdq/sparql`](https://query.wikidata.org/bigdata/namespace/wdq/sparql) is very popular and therefore often overloaded. An error response received by the SPARQL client from Wikidata will result in `500 Internal Server Error` response by the Processor._

## Maven

Processor is released on Maven central as [`com.atomgraph:processor`](https://search.maven.org/artifact/com.atomgraph/processor/).

# Datasource

AtomGraph Processor does *not* include an RDF datasource. It queries RDF data on the fly from a SPARQL endpoint using [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/) over HTTP. SPARQL endpoints are provided by most RDF [triplestores](http://en.wikipedia.org/wiki/Triplestore).

The easiest way to set up a SPARQL endpoint on an RDF dataset is Apache Jena [Fuseki](https://jena.apache.org/documentation/fuseki2/) as a Docker container using our [fuseki](https://hub.docker.com/r/atomgraph/fuseki) image. There is also a number of of [public SPARQL endpoints](http://sparqles.ai.wu.ac.at).

For a commercial triplestore with SPARQL 1.1 support see [Dydra](https://dydra.com).

# Test suite

Processor includes a basic HTTP [test suite](https://github.com/AtomGraph/Processor/tree/master/http-tests) for Linked Data Templates, SPARQL Protocol and the Graph Store Protocol.

![master](https://github.com/AtomGraph/Processor/workflows/HTTP-tests/badge.svg?branch=master)
![develop](https://github.com/AtomGraph/Processor/workflows/HTTP-tests/badge.svg?branch=develop)

# Support

Please [report issues](https://github.com/AtomGraph/Processor/issues) if you've encountered a bug or have a feature request.

Commercial consulting, development, and support are available from [AtomGraph](https://atomgraph.com).

# Community

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop AtomGraph and declarative Linked Data architecture in general.
