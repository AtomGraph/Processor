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
* `ENDPOINT` - SPARQL 1.1 Protocol endpoint (URI)
* `GRAPH_STORE` - SPARQL 1.1 Graph Store protocol endpoint (URI)
* `ONTOLOGY` - LDT ontology (URI)
* `AUTH_USER` - SPARQ service HTTP Basic auth user (string, optional)
* `AUTH_PWD` - SPARQ service HTTP Basic auth password (string, optional)
* `PREEMPTIVE_AUTH` - use premptive HTTP Basic auth? (`true`/`false`, optional)
* `SITEMAP_RULES` - [Jena rules](https://jena.apache.org/documentation/inference/#rules) for the LDT ontologies (string, optional, see [default](src/main/webapp/WEB-INF/web.xml#L16))
* `LOCATION_MAPPING` - Jena's [LocationMapper config](https://jena.apache.org/documentation/notes/file-manager.html#the-locationmapper-configuration-file) (path to RDF file, optional, see [default](src/main/resources/location-mapping.n3))

Run the container like this:

     docker run -p 8080:8080 \
       -e ENDPOINT="http://dbpedia.org/sparql" \
       -e GRAPH_STORE="http://dbpedia.org/service" \
       -e ONTOLOGY="https://www.w3.org/ns/ldt/core/templates#" \
       -v "~/WebRoot/AtomGraph/Processor/src/main/resources/log4j.properties":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/log4j.properties" \
       atomgraph/processor

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

Commercial AtomGraph consulting, development, and support are available from [AtomGraph](http://atomgraph.com).

Community
=========

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop AtomGraph and declarative Linked Data architecture in general.
