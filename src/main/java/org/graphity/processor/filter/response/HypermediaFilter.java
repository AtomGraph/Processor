/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.filter.response;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import javax.ws.rs.ext.Provider;
import org.graphity.core.util.Link;
import org.graphity.core.util.StateBuilder;
import org.graphity.processor.exception.ConstraintViolationException;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.vocabulary.GP;
import org.graphity.processor.vocabulary.XHV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * A filter that adds HATEOAS transitions to the RDF query result.
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{
    private static final Logger log = LoggerFactory.getLogger(HypermediaFilter.class);
            
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        if (response.getStatusType().getFamily().equals(REDIRECTION) || response.getEntity() == null ||
                (!(response.getEntity() instanceof Model)) && !(response.getEntity() instanceof ConstraintViolationException))
            return response;
        
        MultivaluedMap<String, Object> headerMap = response.getHttpHeaders();
        try
        {
            URI ontologyHref = getOntologyURI(headerMap);
            URI typeHref = getTypeURI(headerMap);
            if (ontologyHref == null || typeHref == null) return response;
            Object rulesString = response.getHttpHeaders().getFirst("Rules");
            if (rulesString == null) return response;

            OntModelSpec ontModelSpec = getOntModelSpec(Rule.parseRules(rulesString.toString()));
            OntModel ontModel = OntDocumentManager.getInstance().getOntology(ontologyHref.toString(), ontModelSpec);
            Ontology ontology = ontModel.getOntology(ontologyHref.toString());
            if (ontology == null) throw new SitemapException("Ontology resource '" + ontologyHref.toString() + "'not found in ontology graph");
            OntClass template = ontModel.getOntClass(typeHref.toString());

            Model model;
            if (response.getEntity() instanceof ConstraintViolationException)
                model = ((ConstraintViolationException)response.getEntity()).getModel();
            else
                model = (Model)response.getEntity();
            long oldCount = model.size();

            InfModel infModel = ModelFactory.createInfModel(ontModelSpec.getReasoner(), ontModel, model);
            Resource resource = infModel.createResource(request.getAbsolutePath().toString());        

            // we need this check to avoid building state for gp:SPARQLEndpoint and other system classes
            if (resource.hasProperty(RDF.type, GP.Container) || resource.hasProperty(RDF.type, GP.Document))
            {
                // transition to a URI of another application state (HATEOAS)
                //Resource state = getStateBuilder(resource, request.getQueryParameters(), template).
                Resource state = getStateBuilder(model.createResource(request.getRequestUri().toString()),
                        request.getQueryParameters(), template).
                        build();
                if (!state.getURI().equals(request.getRequestUri().toString()))
                {
                    if (log.isDebugEnabled()) log.debug("Redirecting to a state transition URI: {}", state.getURI());
                    response.setResponse(Response.seeOther(URI.create(state.getURI())).build());
                    return response;
                }                    
            }

            if (resource.hasProperty(RDF.type, GP.Container))
                addPagination(resource, request.getQueryParameters(), template);

            if (log.isDebugEnabled()) log.debug("Added HATEOAS transitions to the response RDF Model for resource: {} # of statements: {}", resource.getURI(), model.size() - oldCount);
            response.setEntity(infModel.getRawModel());
        }
        catch (URISyntaxException ex)
        {
            return response;
        }
        
        return response;
    }
    
    public StateBuilder getStateBuilder(Resource resource, MultivaluedMap<String, String> queryParams, OntClass template)
    {
        StateBuilder sb = StateBuilder.fromUri(resource.getURI().toString(), resource.getModel());

        if (resource.hasProperty(RDF.type, GP.Container))
        {
            final Long offset;
            if (queryParams.containsKey(GP.offset.getLocalName()))
                offset = Long.parseLong(queryParams.getFirst(GP.offset.getLocalName()));
            else
            {
                Long defaultOffset = getLongValue(template, GP.defaultOffset);
                if (defaultOffset != null) offset = defaultOffset;
                else offset = Long.valueOf(0);
            }
            if (offset != null) sb.replaceProperty(GP.offset, ResourceFactory.createTypedLiteral(offset));

            final Long limit;
            if (queryParams.containsKey(GP.limit.getLocalName()))
                limit = Long.parseLong(queryParams.getFirst(GP.limit.getLocalName()));
            else limit = getLongValue(template, GP.defaultLimit);
            if (limit != null) sb.replaceProperty(GP.limit, ResourceFactory.createTypedLiteral(limit));

            final String orderBy;
            if (queryParams.containsKey(GP.orderBy.getLocalName()))
                orderBy = queryParams.getFirst(GP.orderBy.getLocalName());
            else orderBy = getStringValue(template, GP.defaultOrderBy);
            if (orderBy != null) sb.replaceProperty(GP.orderBy, ResourceFactory.createTypedLiteral(orderBy));

            final Boolean desc;
            if (queryParams.containsKey(GP.desc.getLocalName()))
                desc = Boolean.parseBoolean(queryParams.getFirst(GP.orderBy.getLocalName()));
            else desc = getBooleanValue(template, GP.defaultDesc);        
            if (desc != null) sb.replaceProperty(GP.desc, ResourceFactory.createTypedLiteral(desc));
        }
        
        Resource queryOrTemplate = template.getProperty(GP.query).getResource();
        if (!queryOrTemplate.hasProperty(RDF.type, SP.Query))
        {
            Resource spinTemplate = queryOrTemplate.getProperty(RDF.type).getResource();
            StmtIterator constraintIt = spinTemplate.listProperties(SPIN.constraint);
            try
            {
                while (constraintIt.hasNext())
                {
                    Statement stmt = constraintIt.next();
                    Property predicate = stmt.getResource().getPropertyResourceValue(SPL.predicate).as(Property.class);
                    if (queryParams.containsKey(predicate.getLocalName()))
                    {
                        String value = queryParams.getFirst(predicate.getLocalName());
                        Resource valueType = stmt.getResource().getPropertyResourceValue(SPL.valueType);
                        if (valueType != null && valueType.equals(RDFS.Resource))
                            sb.replaceProperty(predicate, ResourceFactory.createResource(value));
                        else
                            sb.replaceProperty(predicate, ResourceFactory.createTypedLiteral(value, XSDDatatype.XSDstring));
                    }
                }
            }
            finally
            {
                constraintIt.close();
            }
        }
        
        return sb;
    }
    
    public Resource addPagination(Resource container, MultivaluedMap<String, String> queryParams, OntClass template)
    {
        if (container == null) throw new IllegalArgumentException("Resource cannot be null");
        if (template == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (queryParams == null) throw new IllegalArgumentException("MultivaluedMap cannot be null");
    
        final Long limit, offset;

        if (queryParams.containsKey(GP.offset.getLocalName()))
            offset = Long.parseLong(queryParams.getFirst(GP.offset.getLocalName()));
        else
        {
            Long defaultOffset = getLongValue(template, GP.defaultOffset);
            if (defaultOffset != null) offset = defaultOffset;
            else offset = Long.valueOf(0);
        }

        if (queryParams.containsKey(GP.limit.getLocalName()))
            limit = Long.parseLong(queryParams.getFirst(GP.limit.getLocalName()));
        else limit = getLongValue(template, GP.defaultLimit);
        
        Resource page = getStateBuilder(container, queryParams, template).build().
            addProperty(GP.pageOf, container).
            addProperty(RDF.type, GP.Page);
        if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} gp:pageOf {}", page, container);

        if (limit != null)
        {
            if (offset >= limit)
            {
                Resource prev = getStateBuilder(container, queryParams, template).
                    replaceProperty(GP.offset, ResourceFactory.createTypedLiteral(offset - limit)).
                    build().
                    addProperty(GP.pageOf, container).
                    addProperty(RDF.type, GP.Page).
                    addProperty(XHV.next, page);

                if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                page.addProperty(XHV.prev, prev);
            }

            Resource next = getStateBuilder(container, queryParams, template).
                replaceProperty(GP.offset, ResourceFactory.createTypedLiteral(offset + limit)).
                build().
                addProperty(GP.pageOf, container).
                addProperty(RDF.type, GP.Page).
                addProperty(XHV.prev, page);

            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
            page.addProperty(XHV.next, next);
        }
        
        return container;
    }

    public final Long getLongValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getLong();
        
        return null;
    }

    public final Boolean getBooleanValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getBoolean();
        
        return null;
    }

    public final String getStringValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getString();
        
        return null;
    }
    
    public URI getTypeURI(MultivaluedMap<String, Object> headerMap) throws URISyntaxException
    {
        return getLinkHref(headerMap, "Link", RDF.type.getLocalName());
    }

    public URI getOntologyURI(MultivaluedMap<String, Object> headerMap) throws URISyntaxException
    {
        return getLinkHref(headerMap, "Link", GP.ontology.getURI());
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
    
    public OntModelSpec getOntModelSpec(List<Rule> rules)
    {
        OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
        
        if (rules != null)
        {
            Reasoner reasoner = new GenericRuleReasoner(rules);
            //reasoner.setDerivationLogging(true);
            //reasoner.setParameter(ReasonerVocabulary.PROPtraceOn, Boolean.TRUE);
            ontModelSpec.setReasoner(reasoner);
        }
        
        return ontModelSpec;
    }
    
}
