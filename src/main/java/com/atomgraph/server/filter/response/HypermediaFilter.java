/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atomgraph.server.filter.response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import com.atomgraph.core.util.Link;
import com.atomgraph.processor.util.TemplateCall;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.LDTC;
import com.atomgraph.processor.vocabulary.LDTDH;
import com.atomgraph.server.exception.OntClassNotFoundException;
import com.atomgraph.server.vocabulary.XHV;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that adds HATEOAS transitions to the RDF query result.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{
    private static final Logger log = LoggerFactory.getLogger(HypermediaFilter.class);
            
    @Context ServletConfig servletConfig;
    @Context Providers providers;
    @Context UriInfo uriInfo;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        // do not process hypermedia if the response is a redirect or 201 Created
        if (response.getStatusType().getFamily().equals(REDIRECTION) || response.getStatusType().equals(CREATED) ||
                response.getEntity() == null || (!(response.getEntity() instanceof Model)))
            return response;
        
        TemplateCall templateCall = getTemplateCall();
        if (templateCall == null) return response;
            
        Resource state = templateCall.build();
        Resource absolutePath = state.getModel().createResource(request.getAbsolutePath().toString());
        if (!state.equals(absolutePath)) // add hypermedia if there are query parameters
        {
            state.addProperty(LDTC.viewOf, absolutePath).
                addProperty(RDF.type, LDTC.View);

            if (state.hasProperty(LDTDH.limit)) // pages must have limits
            {
                if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} dh:pageOf {}", state, absolutePath);
                state.addProperty(LDTDH.pageOf, absolutePath).
                    addProperty(RDF.type, LDTDH.Page); // do we still need dh:Page now that we have core:View?

                addPrevNextPage(absolutePath, state);
            }
        }

        if (response.getStatusType().getFamily().equals(Response.Status.Family.SUCCESSFUL) &&
                state.getPropertyResourceValue(LDTDH.forClass) != null)
        {
            String forClassURI = state.getPropertyResourceValue(LDTDH.forClass).getURI();
            OntClass forClass = templateCall.getTemplate().getOntModel().getOntClass(forClassURI);
            if (forClass == null) throw new OntClassNotFoundException("OntClass '" + forClassURI + "' not found in sitemap");

            state.addProperty(LDTDH.constructor, addInstance(state.getModel(), forClass)); // connects constructor state to CONSTRUCTed template
        }


        if (log.isDebugEnabled()) log.debug("Added Number of HATEOAS statements added: {}", state.getModel().size());
        response.setEntity(state.getModel().add((Model)response.getEntity()));
        
        return response;
    }
        
    public void addPrevNextPage(Resource absolutePath, Resource state)
    {
        if (absolutePath == null) throw new IllegalArgumentException("Resource cannot be null");
        if (state == null) throw new IllegalArgumentException("Resource cannot be null");
        
        Long limit = state.getProperty(LDTDH.limit).getLong();            
        Long offset = Long.valueOf(0);
        if (state.hasProperty(LDTDH.offset)) offset = state.getProperty(LDTDH.offset).getLong();

        if (offset >= limit)
        {
            com.atomgraph.core.util.StateBuilder prevBuilder = TemplateCall.fromResource(state);
            Resource prev = prevBuilder.replaceProperty(LDTDH.offset, ResourceFactory.createTypedLiteral(offset - limit)).
                build().
                addProperty(LDTDH.pageOf, absolutePath).
                addProperty(RDF.type, LDTDH.Page).
                addProperty(XHV.next, state);

            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", state, prev);
            state.addProperty(XHV.prev, prev);
        }

        com.atomgraph.core.util.StateBuilder nextBuilder = TemplateCall.fromResource(state);
        Resource next = nextBuilder.replaceProperty(LDTDH.offset, ResourceFactory.createTypedLiteral(offset + limit)).
                build().
                addProperty(LDTDH.pageOf, absolutePath).
                addProperty(RDF.type, LDTDH.Page).
                addProperty(XHV.prev, state);

        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", state, next);
        state.addProperty(XHV.next, next);
    }
    
    public Resource addInstance(Model targetModel, OntClass forClass)
    {
        if (log.isDebugEnabled()) log.debug("Invoking constructor on class: {}", forClass);
        addClass(forClass, targetModel); // TO-DO: remove when classes and constraints are cached/dereferencable
        return new ConstructorBase().construct(forClass, targetModel);
    }

    // TO-DO: this method should not be necessary when system ontologies/classes are dereferencable! -->
    public void addClass(OntClass forClass, Model targetModel)
    {
        if (forClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (targetModel == null) throw new IllegalArgumentException("Model cannot be null");    

        String queryString = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
"PREFIX  spin: <http://spinrdf.org/spin#>\n" +
"\n" +
"DESCRIBE ?Class ?Constraint\n" +
"WHERE\n" +
"  { ?Class rdfs:isDefinedBy ?Ontology\n" +
"    OPTIONAL\n" +
"      { ?Class spin:constraint ?Constraint }\n" +
"  }";
        
        // the client needs at least labels and constraints
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add(RDFS.Class.getLocalName(), forClass);
        Query query = new ParameterizedSparqlString(queryString, qsm).asQuery();
        QueryExecution qex = QueryExecutionFactory.create(query, forClass.getOntModel());
        try
        {
            targetModel.add(qex.execDescribe());
        }
        finally
        {
            qex.close();
        }
    }

    public URI getTypeURI(MultivaluedMap<String, Object> headerMap) throws URISyntaxException
    {
        return getLinkHref(headerMap, "Link", RDF.type.getLocalName());
    }

    public URI getOntologyURI(MultivaluedMap<String, Object> headerMap) throws URISyntaxException
    {
        return getLinkHref(headerMap, "Link", LDT.ontology.getURI());
    }

    public URI getLinkHref(MultivaluedMap<String, Object> headerMap, String headerName, String rel) throws URISyntaxException
    {
	if (headerMap == null) throw new IllegalArgumentException("Header Map cannot be null");
	if (headerName == null) throw new IllegalArgumentException("String header name cannot be null");
        if (rel == null) throw new IllegalArgumentException("Property Map cannot be null");
        
        List<Object> links = headerMap.get(headerName);
        if (links != null)
        {
            Iterator<Object> it = links.iterator();
            while (it.hasNext())
            {
                String linkHeader = it.next().toString();
                Link link = Link.valueOf(linkHeader);
                if (link.getRel().equals(rel)) return link.getHref();
            }
        }
        
        return null;
    }
    
    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    public TemplateCall getTemplateCall()
    {
        if (!getUriInfo().getMatchedResources().isEmpty())
            return ((com.atomgraph.server.model.Resource)getUriInfo().getMatchedResources().get(0)).getTemplateCall();
        
        return null;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
 
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}