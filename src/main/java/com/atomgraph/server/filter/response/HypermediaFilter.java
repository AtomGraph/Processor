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

import org.apache.jena.ontology.Ontology;
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
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.jena.ext.com.google.common.base.Charsets;
import org.apache.jena.rdf.model.ModelFactory;
import com.atomgraph.core.util.Link;
import com.atomgraph.processor.exception.ArgumentException;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.server.provider.OntologyProvider;
import com.atomgraph.processor.util.RDFNodeFactory;
import com.atomgraph.processor.util.StateBuilder;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.processor.vocabulary.LDTC;
import com.atomgraph.processor.vocabulary.LDTDH;
import com.atomgraph.server.exception.OntClassNotFoundException;
import com.atomgraph.server.vocabulary.XHV;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import javax.ws.rs.ext.Providers;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.model.Argument;

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
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        // do not process hypermedia if the response is a redirect or 201 Created
        if (response.getStatusType().getFamily().equals(REDIRECTION) || response.getStatusType().equals(CREATED) ||
                response.getEntity() == null || (!(response.getEntity() instanceof Model)))
            return response;
        
        MultivaluedMap<String, Object> headerMap = response.getHttpHeaders();
        try
        {
            URI ontologyHref = getOntologyURI(headerMap);
            URI typeHref = getTypeURI(headerMap);
            if (ontologyHref == null || typeHref == null) return response;
            Object rulesString = response.getHttpHeaders().getFirst("Rules");
            if (rulesString == null) return response;

            Ontology ontology = new OntologyProvider(getServletConfig()).getOntology(ontologyHref.toString());
            if (ontology == null) throw new OntologyException("Ontology resource '" + ontologyHref.toString() + "'not found in ontology graph");
            Template template = ontology.getOntModel().getOntClass(typeHref.toString()).as(Template.class);
            
            List<NameValuePair> queryParams = URLEncodedUtils.parse(request.getRequestUri(), Charsets.UTF_8.name());
            // TO-DO: inject TemplateCall?
            TemplateCall templateCall = ontology.getOntModel().createIndividual(LDT.TemplateCall).
                addProperty(LDT.template, template).
                as(TemplateCall.class).applyArguments(queryParams);

            Model model = ModelFactory.createDefaultModel();
            Resource absolutePath = model.createResource(request.getAbsolutePath().toString());
            Resource requestUri = model.createResource(request.getRequestUri().toString());


            StateBuilder viewBuilder = StateBuilder.fromResource(absolutePath);
            Resource view = applyArguments(viewBuilder, templateCall, queryParams).build();
            if (!view.equals(absolutePath)) // add hypermedia if there are query parameters
            {
                view.addProperty(LDTC.viewOf, absolutePath).
                    addProperty(RDF.type, LDTC.View);

                if (templateCall.hasProperty(LDTDH.limit)) // pages must have limits
                {
                    if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} dh:pageOf {}", view, absolutePath);
                    view.addProperty(LDTDH.pageOf, absolutePath).
                    addProperty(RDF.type, LDTDH.Page); // do we still need dh:Page now that we have core:View?

                    addPrevNextPage(absolutePath, viewBuilder, templateCall);
                }
            }
        
            if (response.getStatusType().getFamily().equals(Response.Status.Family.SUCCESSFUL) &&
                    templateCall.getPropertyResourceValue(LDTDH.forClass) != null)
            {
                String forClassURI = templateCall.getPropertyResourceValue(LDTDH.forClass).getURI();
                OntClass forClass = templateCall.getOntModel().getOntClass(forClassURI);
                if (forClass == null) throw new OntClassNotFoundException("OntClass '" + forClassURI + "' not found in sitemap");

                // TO-DO: check if Rules still necessary or does SPIN handle spin:constructor inheritance
                requestUri.addProperty(LDTDH.constructor, addInstance(model, forClass)); // connects constructor state to CONSTRUCTed template
            }

            
            if (log.isDebugEnabled()) log.debug("Added Number of HATEOAS statements added: {}", model.size());
            response.setEntity(model.add((Model)response.getEntity()));
        }
        catch (URISyntaxException ex)
        {
            return response;
        }
        
        return response;
    }
    
    public StateBuilder applyArguments(StateBuilder stateBuilder, TemplateCall templateCall, List<NameValuePair> params)
    {
        if (stateBuilder == null) throw new IllegalArgumentException("Resource cannot be null");
        if (templateCall == null) throw new IllegalArgumentException("Templatecall cannot be null");
        if (params == null) throw new IllegalArgumentException("Param List cannot be null");
        
        Iterator <NameValuePair> it = params.iterator();
        while (it.hasNext())
        {
            NameValuePair pair = it.next();
            String paramName = pair.getName();
            String paramValue = pair.getValue();

            Argument arg = templateCall.getTemplate().getArgumentsMap().get(paramName);
            if (arg == null) throw new ArgumentException(paramName, templateCall.getTemplate());

            stateBuilder.property(arg.getPredicate(), RDFNodeFactory.createTyped(paramValue, arg.getValueType()));
        }
        
        return stateBuilder;
    }
    
    public void addPrevNextPage(Resource absolutePath, StateBuilder pageBuilder, TemplateCall pageCall)
    {
        if (absolutePath == null) throw new IllegalArgumentException("Resource cannot be null");
        if (pageBuilder == null) throw new IllegalArgumentException("StateBuilder cannot be null");
        if (pageCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
        
        //if (pageCall.hasProperty(LDT.limit))
        {
            Resource page = pageBuilder.build();
            Long limit = pageCall.getProperty(LDTDH.limit).getLong();            
            Long offset = Long.valueOf(0);
            if (pageCall.hasProperty(LDTDH.offset)) offset = pageCall.getProperty(LDTDH.offset).getLong();
            
            if (offset >= limit)
            {
                TemplateCall prevCall = pageCall.removeAll(LDTDH.offset).
                    addLiteral(LDTDH.offset, offset - limit).
                    as(TemplateCall.class);
                Resource prev = pageBuilder.apply(prevCall).build().
                    addProperty(LDTDH.pageOf, absolutePath).
                    addProperty(RDF.type, LDTDH.Page).
                    addProperty(XHV.next, page);

                if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                page.addProperty(XHV.prev, prev);
            }

            TemplateCall nextCall = pageCall.removeAll(LDTDH.offset).
                addLiteral(LDTDH.offset, offset + limit).
                as(TemplateCall.class);
            Resource next = pageBuilder.apply(nextCall).build().
                addProperty(LDTDH.pageOf, absolutePath).
                addProperty(RDF.type, LDTDH.Page).
                addProperty(XHV.prev, page);

            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
            page.addProperty(XHV.next, next);
        }
        
        //return container;
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
    
}