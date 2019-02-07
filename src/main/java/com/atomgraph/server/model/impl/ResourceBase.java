/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.server.model.impl;

import com.atomgraph.core.MediaTypes;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.sparql.util.Loader;
import org.apache.jena.update.UpdateRequest;
import com.sun.jersey.api.core.ResourceContext;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import com.atomgraph.core.exception.NotFoundException;
import com.atomgraph.core.model.GraphStore;
import com.atomgraph.core.model.SPARQLEndpoint;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.util.Link;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.core.model.impl.QueriedResourceBase;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.util.RulePrinter;
import com.atomgraph.processor.util.TemplateCall;
import java.util.Collections;
import java.util.Iterator;
import static javax.ws.rs.core.Response.Status.OK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.arq.ARQ2SPIN;
import org.spinrdf.vocabulary.SPIN;

/**
 * Base class of generic read-write Linked Data resources.
 * Configured declaratively using sitemap ontology to provide Linked Data layer over a SPARQL endpoint.
 * Supports pagination on containers (implemented using SPARQL query solution modifiers).
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/org/apache/jena/ontology/OntResource.html">OntResource</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#solutionModifiers">15 Solution Sequences and Modifiers</a>
 */
@Path("/")
public class ResourceBase extends QueriedResourceBase implements com.atomgraph.server.model.Resource, com.atomgraph.server.model.QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);
        
    private final com.atomgraph.processor.model.Application application;
    private final Ontology ontology;
    private final TemplateCall templateCall;
    private final OntResource ontResource;
    private final ResourceContext resourceContext;
    private final HttpHeaders httpHeaders;  
    private final QuerySolutionMap querySolutionMap;
    private final Query query;
    private final UpdateRequest update;

    /**
     * Public JAX-RS instance. Suitable for subclassing.
     * If the request URI does not match any URI template in the sitemap ontology, 404 Not Found is returned.
     * 
     * If the matching template extends <code>ldt:Document</code>, this resource becomes a page resource and
     * HATEOS metadata is added (relations to the container and previous/next page resources).
     * 
     * @param application LDT application
     * @param mediaTypes mediaTypes
     * @param uriInfo URI information of the current request
     * @param request current request
     * @param service SPARQL service
     * @param ontology LDT ontology
     * @param templateCall templateCall
     * @param httpHeaders HTTP headers of the current request
     * @param resourceContext resource context
     */
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context MediaTypes mediaTypes,
            @Context Service service, @Context com.atomgraph.processor.model.Application application, @Context Ontology ontology, @Context TemplateCall templateCall,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
        this(uriInfo, request, mediaTypes, uriInfo.getAbsolutePath(),
                service, application, ontology, templateCall,
                httpHeaders, resourceContext);
    }

    protected ResourceBase(final UriInfo uriInfo, final Request request, final MediaTypes mediaTypes, final URI uri,
            final Service service, final com.atomgraph.processor.model.Application application, final Ontology ontology, final TemplateCall templateCall,
            final HttpHeaders httpHeaders, final ResourceContext resourceContext)
    {
        super(uriInfo, request, mediaTypes, uri, service);

        if (templateCall == null)
        {
            if (log.isDebugEnabled()) log.debug("Resource {} has not matched any template, returning 404 Not Found", getURI());
            throw new NotFoundException("Resource has not matched any template");
        }
        if (application == null) throw new IllegalArgumentException("Application cannot be null");
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
        if (resourceContext == null) throw new IllegalArgumentException("ResourceContext cannot be null");

        // we are not making permanent changes to base ontology because OntologyProvider always makes a copy
        this.application = application;
        this.ontology = ontology;
        this.ontResource = ontology.getOntModel().createOntResource(getURI().toString());
        this.templateCall = templateCall;
        this.httpHeaders = httpHeaders;
        this.resourceContext = resourceContext;
        this.querySolutionMap = templateCall.getQuerySolutionMap();
        this.querySolutionMap.add(SPIN.THIS_VAR_NAME, ontResource); // ?this
        this.query = new ParameterizedSparqlString(ARQ2SPIN.getTextOnly(templateCall.getTemplate().getQuery()), querySolutionMap, uriInfo.getBaseUri().toString()).asQuery();
        if (templateCall.getTemplate().getUpdate() != null)
            this.update = new ParameterizedSparqlString(ARQ2SPIN.getTextOnly(templateCall.getTemplate().getUpdate()), querySolutionMap, uriInfo.getBaseUri().toString()).asUpdate();
        else
            this.update = null;

        if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with matched Template: {}", templateCall.getTemplate());
    }
    
    /**
     * Returns sub-resource instance.
     * By default matches any path.
     * 
     * @return resource object
     */
    @Path("{path: .+}")
    @Override
    public Object getSubResource()
    {
        if (getTemplateCall().getTemplate().getLoadClass() != null)
        {
            Resource javaClass = getTemplateCall().getTemplate().getLoadClass();
            if (!javaClass.isURIResource())
            {
                if (log.isErrorEnabled()) log.error("ldt:loadClass value of template '{}' is not a URI resource", getTemplateCall().getTemplate());
                throw new OntologyException("ldt:loadClass value of template '" + getTemplateCall().getTemplate() + "' is not a URI resource");
            }

            Class clazz = Loader.loadClass(javaClass.getURI());
            if (clazz == null)
            {
                if (log.isErrorEnabled()) log.error("Java class with URI '{}' could not be loaded", javaClass.getURI());
                throw new OntologyException("Java class with URI '" + javaClass.getURI() + "' not found");
            }

            if (log.isDebugEnabled()) log.debug("Loading Java class with URI: {}", javaClass.getURI());
            return getResourceContext().getResource(clazz);
        }

        return this;
    }

    /**
     * Handles POST method. Appends the submitted RDF dataset to the Graph Store.
     * 
     * @param dataset the RDF payload
     * @return response
     */
    @Override
    public Response post(Dataset dataset)
    {
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");

        getGraphStore().post(dataset.getDefaultModel(), Boolean.TRUE, null);
        
        Iterator<String> it = dataset.listNames();
        while (it.hasNext())
        {
            String graphName = it.next();
            getGraphStore().post(dataset.getNamedModel(graphName), Boolean.FALSE, URI.create(graphName));
        }
        
        return Response.ok().build();
    }

    /**
     * Handles PUT method. Deletes the resource description and appends the submitted RDF dataset to the Graph Store.
     * 
     * @param dataset RDF payload
     * @return response
     */
    @Override
    public Response put(Dataset dataset)
    {
        Response deleted = delete();
        
        if (deleted.getStatus() != OK.getStatusCode())
        {
            if (log.isErrorEnabled()) log.error("PUT Dataset does not execute DELETE with '{}' as subject", getURI());
            throw new WebApplicationException(new IllegalStateException(deleted.getMetadata().toString()), deleted.getStatus());
        }
        
        return post(dataset);
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
        if (getUpdate() == null) return Response.status(501).build(); // 501 Not Implemented
            
        if (log.isDebugEnabled()) log.debug("DELETE UpdateRequest: {}", getUpdate());
        getSPARQLEndpoint().post(getUpdate(), Collections.<URI>emptyList(), Collections.<URI>emptyList());

        return Response.noContent().build();
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
     * @param dataset response RDF dataset
     * @return response builder
     */
    @Override
    public ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        ResponseBuilder rb = super.getResponseBuilder(dataset);
        
        rb.cacheControl(getCacheControl());

        rb.header("Link", new Link(URI.create(getTemplateCall().getTemplate().getURI()), LDT.template.getURI(), null));
        rb.header("Link", new Link(URI.create(getApplication().getOntology().getURI()), LDT.ontology.getURI(), null));
        rb.header("Link", new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));
        
        Reasoner reasoner = getTemplateCall().getTemplate().getOntModel().getSpecification().getReasoner();
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
     * Returns LDT template that the URI of this resource matches.
     * If the request URI did not match any template, <code>404 Not Found</code> was returned.
     * 
     * @return ontology class
     */
    @Override
    public TemplateCall getTemplateCall()
    {
        return templateCall;
    }
    
    /**
     * Returns the cache control of this resource, if specified.
     * The control value can be specified as <code>ldt:cacheControl</code> on templates in the sitemap ontology.
     * 
     * @return cache control object or null, if not specified
     */
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
        return query;
    }

    /**
     * Returns update used to remove RDF description of this resource.
     * Query solution bindings are applied by default.
     * 
     * @return update object with applied solution bindings
     * @see #getQuerySolutionMap()
     */
    @Override
    public UpdateRequest getUpdate()
    {
        return update;
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
    public com.atomgraph.processor.model.Application getApplication()
    {
        return application;
    }
 
    @Override
    public Ontology getOntology()
    {
        return ontology;
    }
 
    public SPARQLEndpoint getSPARQLEndpoint()
    {
        return getService().getSPARQLEndpoint(getRequest());
    }
    
    public GraphStore getGraphStore()
    {
        return getService().getGraphStore(getRequest());
    }
    
}