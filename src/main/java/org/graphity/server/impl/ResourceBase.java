/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.graphity.server.impl;

import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.sparql.util.Loader;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.api.core.ResourceContext;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.graphity.core.MediaTypes;
import org.graphity.core.exception.NotFoundException;
import org.graphity.processor.query.QueryBuilder;
import org.graphity.processor.update.InsertDataBuilder;
import org.graphity.core.util.Link;
import org.graphity.processor.vocabulary.GP;
import org.graphity.core.model.GraphStore;
import org.graphity.core.model.impl.QueriedResourceBase;
import org.graphity.core.model.SPARQLEndpoint;
import org.graphity.core.util.ModelUtils;
import org.graphity.core.vocabulary.G;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.model.TemplateCall;
import org.graphity.processor.query.SelectBuilder;
import org.graphity.processor.update.ModifyBuilder;
import org.graphity.processor.util.RulePrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.NamedGraph;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;

/**
 * Base class of generic read-write Graphity Processor resources.
 * Configured declaratively using sitemap ontology to provide Linked Data layer over a SPARQL endpoint.
 * Supports pagination on containers (implemented using SPARQL query solution modifiers).
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/ontology/OntResource.html">OntResource</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#solutionModifiers">15 Solution Sequences and Modifiers</a>
 */
@Path("/")
public class ResourceBase extends QueriedResourceBase implements org.graphity.server.Resource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);
        
    private final GraphStore graphStore;
    private final Ontology ontology;
    private final TemplateCall templateCall;
    private final OntResource ontResource;
    private final ResourceContext resourceContext;
    private final HttpHeaders httpHeaders;  
    private QuerySolutionMap querySolutionMap;
    private QueryBuilder queryBuilder;
    private ModifyBuilder modifyBuilder;

    /**
     * Public JAX-RS constructor. Suitable for subclassing.
     * If the request URI does not match any URI template in the sitemap ontology, 404 Not Found is returned.
     * 
     * If the matching ontology class is a subclass of <code>gp:Page</code>, this resource becomes a page resource and
     * HATEOS metadata is added (relations to the container and previous/next page resources).
     * 
     * @param uriInfo URI information of the current request
     * @param request current request
     * @param servletConfig webapp context
     * @param mediaTypes supported media types
     * @param endpoint SPARQL endpoint of this resource
     * @param graphStore Graph Store of this resource
     * @param ontology principal ontology
     * @param templateCall templateCall
     * @param httpHeaders HTTP headers of the current request
     * @param resourceContext resource context
     */
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletConfig servletConfig,
            @Context MediaTypes mediaTypes, @Context SPARQLEndpoint endpoint, @Context GraphStore graphStore,
            @Context Ontology ontology, @Context TemplateCall templateCall,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
	super(uriInfo, request, servletConfig, mediaTypes, endpoint);

        if (templateCall == null)
        {
            if (log.isDebugEnabled()) log.debug("Resource {} has not matched any template Template, returning 404 Not Found", getURI());
            throw new NotFoundException("Resource has not matched any template");
        }
	if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");        
	if (graphStore == null) throw new IllegalArgumentException("GraphStore cannot be null");
        if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
	if (resourceContext == null) throw new IllegalArgumentException("ResourceContext cannot be null");

        // we are not making permanent changes to base ontology because OntologyProvider always makes a copy
        this.ontResource = ontology.getOntModel().createOntResource(getURI().toString());
        this.ontology = ontology;
        this.templateCall = templateCall.applyArguments(uriInfo.getQueryParameters());
        this.graphStore = graphStore;
	this.httpHeaders = httpHeaders;
        this.resourceContext = resourceContext;
        this.querySolutionMap = new QuerySolutionMap();
	this.querySolutionMap.add(SPIN.THIS_VAR_NAME, ontResource); // ?this

        if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with matched Template: {}", templateCall.getTemplate());
    }

    /**
     * Post-construct initialization. Subclasses need to call super.init() first, just like with super() constructor.
     */
    @Override
    public void init()
    {        
        if (getRequest().getMethod().equalsIgnoreCase("PUT") || getRequest().getMethod().equalsIgnoreCase("DELETE"))
            modifyBuilder = getTemplateCall().getModifyBuilder(getUriInfo().getBaseUri(), ModelFactory.createDefaultModel());
        else
        {
            queryBuilder = getTemplateCall().getQueryBuilder(getUriInfo().getBaseUri(), ModelFactory.createDefaultModel());
            if (getTemplateCall().getTemplate().equals(GP.Container) || hasSuperClass(getTemplateCall().getTemplate(), GP.Container))
                queryBuilder = getPageQueryBuilder(queryBuilder, getTemplateCall());
        }        
    }
    
    /**
     * Returns sub-resource instance.
     * By default matches any path.
     * 
     * @return resource object
     */
    @Path("{path: .+}")
    public Object getSubResource()
    {
        if (getTemplateCall().getTemplate().getLoadClass() != null)
        {
            Resource javaClass = getTemplateCall().getTemplate().getLoadClass();
            if (!javaClass.isURIResource())
            {
                if (log.isErrorEnabled()) log.error("gp:loadClass value of class '{}' is not a URI resource", getTemplateCall().getURI());
                throw new SitemapException("gp:loadClass value of class '" + getTemplateCall().getURI() + "' is not a URI resource");
            }

            Class clazz = Loader.loadClass(javaClass.getURI());
            if (clazz == null)
            {
                if (log.isErrorEnabled()) log.error("Java class with URI '{}' could not be loaded", javaClass.getURI());
                throw new SitemapException("Java class with URI '" + javaClass.getURI() + "' not found");
            }

            if (log.isDebugEnabled()) log.debug("Loading Java class with URI: {}", javaClass.getURI());
            return getResourceContext().getResource(clazz);
        }

        return this;
    }
        
    /**
     * Handles POST method, stores the submitted RDF model in the default graph of default SPARQL endpoint, and returns response.
     * 
     * @param model the RDF payload
     * @return response
     */
    @Override
    public Response post(Model model)
    {
	return post(model, null);
    }
    
    /**
     * Handles POST method, stores the submitted RDF model in the specified named graph of the specified SPARQL endpoint, and returns response.
     * 
     * @param model the RDF payload
     * @param graphURI target graph name
     * @return response
     */
    public Response post(Model model, URI graphURI)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (log.isDebugEnabled()) log.debug("POSTed Model: {} to GRAPH URI: {}", model, graphURI);

	Resource created = getURIResource(model, RDF.type, GP.Document);
	if (created == null)
	{
	    if (log.isDebugEnabled()) log.debug("POSTed Model does not contain statements with URI as subject and type '{}'", GP.Document.getURI());
	    throw new WebApplicationException(Response.Status.BAD_REQUEST);
	}

        UpdateRequest insertDataRequest;
	if (graphURI != null) insertDataRequest = InsertDataBuilder.fromData(graphURI, model).build();
	else insertDataRequest = InsertDataBuilder.fromData(model).build();

        insertDataRequest.setBaseURI(getUriInfo().getBaseUri().toString());
        if (log.isDebugEnabled()) log.debug("INSERT DATA request: {}", insertDataRequest);

	getSPARQLEndpoint().post(insertDataRequest, null, null);
	
	URI createdURI = UriBuilder.fromUri(created.getURI()).build();
	if (log.isDebugEnabled()) log.debug("Redirecting to POSTed Resource URI: {}", createdURI);
	// http://stackoverflow.com/questions/3383725/post-redirect-get-prg-vs-meaningful-2xx-response-codes
	// http://www.blackpepper.co.uk/posts/201-created-or-post-redirect-get/
	//return Response.created(createdURI).entity(model).build();
	return Response.seeOther(createdURI).build();
    }

    public Resource getURIResource(Model model, Property property, Resource object)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");
	if (object == null) throw new IllegalArgumentException("Object Resource cannot be null");

        ResIterator it = model.listSubjectsWithProperty(property, object);

	try
	{
	    while (it.hasNext())
	    {
		Resource resource = it.next();

		if (resource.isURIResource()) return resource;
	    }
	}
	finally
	{
	    it.close();
	}
	
	return null;
    }

    /**
     * Handles PUT method, stores the submitted RDF model in the default graph of default SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    @Override
    public Response put(Model model)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (log.isDebugEnabled()) log.debug("PUT Model: {}", model);

	if (!model.containsResource(getOntResource()))
	{
	    if (log.isDebugEnabled()) log.debug("PUT Model does not contain statements with request URI '{}' as subject", getURI());
	    throw new WebApplicationException(Response.Status.BAD_REQUEST);
	}
	
	Model description = describe();	
	
	if (!description.isEmpty()) // remove existing representation
	{
	    EntityTag entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
	    Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	    if (rb != null)
	    {
		if (log.isDebugEnabled()) log.debug("PUT preconditions were not met for resource: {} with entity tag: {}", this, entityTag);
		return rb.build();
	    }
        }
        
        UpdateRequest deleteInsertRequest = getUpdateRequest(model);
        if (log.isDebugEnabled()) log.debug("DELETE/INSERT UpdateRequest: {}", deleteInsertRequest);
	getSPARQLEndpoint().post(deleteInsertRequest, null, null);
	if (description.isEmpty()) return Response.created(getURI()).build();
	else return getResponse(model);
    }

    /**
     * Handles DELETE method, deletes the RDF representation of this resource from the default SPARQL endpoint, and
     * returns response.
     * 
     * @return response
     */
    @Override
    public Response delete()
    {	
        UpdateRequest request = getUpdateRequest((Model)null);
        if (log.isDebugEnabled()) log.debug("DELETE UpdateRequest: {}", request);
	getSPARQLEndpoint().post(request, null, null);
	
	return Response.noContent().build();
    }
    
    public final boolean hasSuperClass(OntClass subClass, OntClass superClass)
    {
        ExtendedIterator<OntClass> extIt = subClass.listSuperClasses(false);
        
        while (extIt.hasNext())
        {
            OntClass nextClass = extIt.next();
            if (nextClass.equals(superClass) || hasSuperClass(nextClass, superClass)) return true;
        }

        return false;
    }

    /**
     * Returns variable bindings for description query.
     * 
     * @return map with variable bindings
     */
    public QuerySolutionMap getQuerySolutionMap()
    {
        return querySolutionMap;
    }
        
    /**
     * Adds matched sitemap class as affordance metadata in <pre>Link</pre> header.
     * 
     * @param model response model
     * @return response builder
     */
    @Override
    public ResponseBuilder getResponseBuilder(Model model)
    {
        ResponseBuilder rb = super.getResponseBuilder(model);
        
        //rb.header("Query", getQuery().toString());
        
        Link classLink = new Link(URI.create(getTemplateCall().getTemplate().getURI()), RDF.type.getLocalName(), null);
        rb.header("Link", classLink.toString());
        
        Link ontologyLink = new Link(URI.create(getOntology().getURI()), GP.ontology.getURI(), null);
        rb.header("Link", ontologyLink.toString());

        Link baseLink = new Link(getUriInfo().getBaseUri(), G.baseUri.getURI(), null);
        rb.header("Link", baseLink.toString());
        
        Reasoner reasoner = getTemplateCall().getOntModel().getSpecification().getReasoner();
        if (reasoner instanceof GenericRuleReasoner)
        {
            GenericRuleReasoner grr = (GenericRuleReasoner)reasoner;
            rb.header("Rules", RulePrinter.print(grr.getRules())); // grr.getRules().toString() - prevented by JENA-1030 bug
        }
        
        return rb;
    }
        
    @Override
    public List<Locale> getLanguages()
    {
        return getTemplateCall().getTemplate().getLanguages();
    }
        
    /**
     * Returns this resource as ontology resource.
     * 
     * @return ontology resource
     */
    public OntResource getOntResource()
    {
	return ontResource;
    }

    /**
     * Returns ontology class that this resource matches.
     * If the request URI did not match any ontology class, <code>404 Not Found</code> was returned.
     * 
     * @return ontology class
     */
    @Override
    public TemplateCall getTemplateCall()
    {
	return templateCall;
    }

    /**
     * Returns the principal ontology.
     * 
     * @return ontology
     */
    @Override
    public Ontology getOntology()
    {
        return ontology;
    }

    /**
     * Returns the cache control of this resource, if specified.
     * The control value can be specified as a <code>gp:cacheControl</code> value restriction on an ontology class in
     * the sitemap ontology.
     * 
     * @return cache control object or null, if not specified
     */
    @Override
    public CacheControl getCacheControl()
    {
	return getTemplateCall().getTemplate().getCacheControl();
    }
    
    /**
     * Returns query used to retrieve RDF description of this resource.
     * Query solution bindings are applied by default.
     * 
     * @return query object with applied solution bindings
     * @see #getQuerySolutionMap()
     */
    @Override
    public Query getQuery()
    {
        return getQuery(getQueryBuilder().build().toString(), getQuerySolutionMap(), getUriInfo().getBaseUri().toString());
    }

    public QueryBuilder getPageQueryBuilder(QueryBuilder builder, TemplateCall templateCall)
    {
	if (builder == null) throw new IllegalArgumentException("QueryBuilder cannot be null");
	if (templateCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
                
        if (builder.getSubSelectBuilders().isEmpty())
        {
            if (log.isErrorEnabled()) log.error("QueryBuilder '{}' does not contain a sub-SELECT", queryBuilder);
            throw new SitemapException("Sub-SELECT missing in QueryBuilder: " + queryBuilder + "'");
        }

        SelectBuilder subSelectBuilder = builder.getSubSelectBuilders().get(0);
        if (log.isDebugEnabled()) log.debug("Found main sub-SELECT of the query: {}", subSelectBuilder);

        if (templateCall.hasProperty(GP.offset))
        {
            Long offset = templateCall.getProperty(GP.offset).getLong();
            if (log.isDebugEnabled()) log.debug("Setting OFFSET on container sub-SELECT: {}", offset);
            subSelectBuilder.replaceOffset(offset);
        }

        if (templateCall.hasProperty(GP.limit))
        {
            Long limit = templateCall.getProperty(GP.limit).getLong();
            if (log.isDebugEnabled()) log.debug("Setting LIMIT on container sub-SELECT: {}", limit);
            subSelectBuilder.replaceLimit(limit);
        }

        /*
        try
        {
        */
            if (templateCall.hasProperty(GP.orderBy))
            {
                try
                {
                    String orderBy = templateCall.getProperty(GP.orderBy).getString();
                    
                    Boolean desc = false; // ORDERY BY is ASC() by default
                    if (templateCall.hasProperty(GP.desc)) desc = templateCall.getProperty(GP.desc).getBoolean();
                        
                    if (log.isDebugEnabled()) log.debug("Setting ORDER BY on container sub-SELECT: ?{} DESC: {}", orderBy, desc);
                    subSelectBuilder.replaceOrderBy(null). // any existing ORDER BY condition is removed first
                        orderBy(orderBy, desc);
                }
                catch (IllegalArgumentException ex)
                {
                    if (log.isWarnEnabled()) log.warn(ex.getMessage(), ex);
                }
            }
        /*
        }
        catch (NumberFormatException ex)
        {
            throw new WebApplicationException(ex);
        }
        */
        
        return builder;
    }
    
     /**
     * Returns query used to retrieve RDF description of this resource
     * 
     * @param command query string
     * @param qsm query solution map to be applied
     * @param baseUri base URI of the query
     * @return query object
     */    
    public Query getQuery(String command, QuerySolutionMap qsm, String baseUri)
    {
	if (command == null) throw new IllegalArgumentException("Command String cannot be null");
     
        return new ParameterizedSparqlString(command, qsm, baseUri).asQuery();        
    }

    /**
     * Returns Graph Store of this resource.
     * 
     * @return graph store object
     */
    public GraphStore getGraphStore()
    {
        return graphStore;
    }

    /**
     * Returns query builder, which is used to build SPARQL query to retrieve RDF description of this resource.
     * 
     * @return query builder
     */
    @Override
    public QueryBuilder getQueryBuilder()
    {        
	return queryBuilder;
    }

    @Override
    public ModifyBuilder getModifyBuilder()
    {        
	return modifyBuilder;
    }

    public ModifyBuilder getModifyBuilderWithData(ModifyBuilder builder, Model model)
    {
	if (builder == null) throw new IllegalArgumentException("ModifyBuilder cannot be null");

        // inject ground triples into DELETE named graph, if it is present in the Modify
        Resource deletePattern = builder.getPropertyResourceValue(SP.deletePattern);
        if (deletePattern != null)
        {
            RDFNode deleteListHead = deletePattern.as(RDFList.class).getHead();
            if (deleteListHead.canAs(NamedGraph.class))
            {
                NamedGraph deleteNamedGraph = deleteListHead.as(NamedGraph.class);
                NamedGraph insertNamedGraph = SPINFactory.createNamedGraph(builder.getModel(), deleteNamedGraph.getNameNode(), builder.createDataList(model));
                return builder.insertPattern(builder.getModel().createList().with(insertNamedGraph));
            }
        }

        // otherwise, inject ground triples into the default graph
        return builder.insertPattern(model);
    }
    
    public UpdateRequest getUpdateRequest(Model model)
    {
        if (model != null && !model.isEmpty())
            return new ParameterizedSparqlString(getModifyBuilderWithData(getModifyBuilder(), model).build().toString(),
                    getQuerySolutionMap(), getUriInfo().getBaseUri().toString()).asUpdate();
            
        return new ParameterizedSparqlString(getModifyBuilder().build().toString(),
                getQuerySolutionMap(), getUriInfo().getBaseUri().toString()).asUpdate();
    }
    
    /**
     * Returns HTTP headers of the current request.
     * 
     * @return header object
     */
    public HttpHeaders getHttpHeaders()
    {
	return httpHeaders;
    }

    public ResourceContext getResourceContext()
    {
        return resourceContext;
    }
    
    @Override
    public Model getCommandModel()
    {
        return getQueryBuilder().getModel();
    }
    
}