AtomGraph Processor is a Java backend for building declarative, read-write Linked Data applications. If you have a triplestore with RDF data that you want to serve Linked Data from, or write RDF to over a RESTful HTTP interface, AtomGraph Processor is the only component you need.

What AtomGraph Processor provides for users as out-of-the-box generic features:
* declarative control of published and accepted data using URI and SPARQL templates
* pagination on container resources, with ordering by property columns
* control of RDF input quality with SPARQL-based constraints
* SPARQL endpoint and Graph Store Protocol endpoint
* HTTP content negotiation and caching
* a separate RDF resource for every application state, following the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) principle

AtomGraph's direct use of semantic technologies results in extemely extensible and flexible design and leads the way towards declarative Web development. You can forget all about broken hyperlinks and concentrate on building great apps on quality data. For more details, see [articles and presentations](../../wiki/Articles-and-presentations) about AtomGraph.

For a compatible frontend framework for end-user applications, see [AtomGraph Web-Client](../../../Web-Client). The Client extends Processor with RDF rendering and form-based input functionality.

Getting started
===============

* [how AtomGraph Processor works](../../wiki/How-Processor-works)
* [installing AtomGraph Processor](../../wiki/Installation)
* [configuring AtomGraph Processor](../../wiki/Configuration)
* [JavaDoc](http://graphity.github.io/graphity-processor/apidocs)

For full documentation, see the [wiki index](../../wiki).

Usage
=====

Docker
------

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
    <dt><code>SITEMAP_RULES</code></dt>
    <dd><a href="https://jena.apache.org/documentation/inference/#rules">Jena rules</a> for the LDT ontologies</dd>
    <dd>string, optional, see <a href="src/main/webapp/WEB-INF/web.xml#L16">default</a></dd>
    <dt><code>LOCATION_MAPPING</code></dt>
    <dd>Jena's <a href="https://jena.apache.org/documentation/notes/file-manager.html#the-locationmapper-configuration-file">LocationMapper config</a> (path to RDF file, optional, see <a href="src/main/resources/location-mapping.n3">default</a></dd>
</dl>

Run the container with Wikidata's example like this (replace `~/WebRoot/...` paths with your own):

    docker run \
       -p 8080:8080 \
      -e ENDPOINT="https://query.wikidata.org/bigdata/namespace/wdq/sparql" \
      -e GRAPH_STORE="https://query.wikidata.org/bigdata/namespace/wdq/service" \ # not an actual GSP endpoint
      -e ONTOLOGY="https://github.com/AtomGraph/Processor/blob/develop/examples/wikidata#" \
      -v "~/WebRoot/AtomGraph/Processor/src/main/resources/log4j.properties":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/log4j.properties" \
      -v "~/WebRoot/AtomGraph/Processor/examples/location-mapping.n3":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/location-mapping.n3" \
      -v "~/WebRoot/AtomGraph/Processor/examples/wikidata.ttl":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/org/wikidata/ldt.ttl" \
      atomgraph/processor

After that, access [http://localhost:8080/birthdays?limit=10] and you will retrieve RDF data with 10 people (or "entities") that have a birthday today.

Maven
-----

Processor 1.1.4 artifact is not yet released on Maven central.

No permanent storage!
---------------------

AtomGraph Processor does *not* include permanent RDF storage. By default it is configured to read the dataset from a file, therefore creating/updating data will have no effect.

In order to store data permanently, you need to set up a [triplestore](http://en.wikipedia.org/wiki/Triplestore) and configure the webapp with its SPARQL endpoint.
For open-source, we recommend trying Apache Jena's [Fuseki](https://jena.apache.org/documentation/fuseki2/); for commercial, see [Dydra](http://dydra.com).

Support
=======

Please [report issues](../../issues) if you've encountered a bug or have a feature request.

Commercial consulting, development, and support are available from [AtomGraph](http://atomgraph.com).

Community
=========

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop AtomGraph and declarative Linked Data architecture in general.
