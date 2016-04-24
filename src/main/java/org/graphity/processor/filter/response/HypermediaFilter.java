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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
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
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.model.impl.ConstructorBase;
import org.graphity.core.util.StateBuilder;
import org.graphity.processor.exception.ConstraintViolationException;
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
    
    @Context Application application;
    @Context Providers providers;
    @Context UriInfo uriInfo;
    @Context ServletConfig servletConfig;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        if (getResource() == null || 
                response.getStatusType().getFamily().equals(REDIRECTION) || response.getEntity() == null ||
                (!(response.getEntity() instanceof Model)) && !(response.getEntity() instanceof ConstraintViolationException))
            return response;
        
        Object rulesString = response.getHttpHeaders().getFirst("Rules");
        if (rulesString == null) return response;
        
        Model model;
        if (response.getEntity() instanceof ConstraintViolationException)
            model = ((ConstraintViolationException)response.getEntity()).getModel();
        else
            model = (Model)response.getEntity();
        long oldCount = model.size();

        List<Rule> rules = Rule.parseRules(rulesString.toString());
        Reasoner reasoner = new GenericRuleReasoner(rules);
        InfModel infModel = ModelFactory.createInfModel(reasoner, getOntology().getOntModel(), model);

        Resource resource = infModel.createResource(request.getAbsolutePath().toString());
        // TO-DO: remove dependency on matched class. The filter should operate solely on the in-band RDF response
        resource = applyView(resource, getMatchedOntClass());
        //String limit = request.getQueryParameters(true).getFirst(GP.limit.getLocalName());
        //String offset = request.getQueryParameters(true).getFirst(GP.offset.getLocalName());
        if (resource.hasProperty(RDF.type, GP.Container))
            addPagination(resource, getResource().getLimit(), getResource().getOffset());

        // TO-DO: move constructor logic to Graphity Client
        if (getResource().getForClass() != null && !(response.getEntity() instanceof ConstraintViolationException))
        {
            OntClass forClass = getOntology().getOntModel().createClass(getResource().getForClass().getURI());
            model = addInstance(infModel, forClass);

            StateBuilder.fromResource(resource).
                replaceProperty(GP.forClass, getResource().getForClass()).
                build().
                addProperty(RDF.type, GP.Constructor).
                addProperty(GP.constructorOf, resource);            
        }

        if (log.isDebugEnabled()) log.debug("Added HATEOAS transitions to the response RDF Model for resource: {} # of statements: {}", resource.getURI(), model.size() - oldCount);
        response.setEntity(infModel.getRawModel());
        return response;
    }

    public Model addInstance(Model model, OntClass forClass)
    {
        if (forClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        Property property = SPIN.constructor;
        if (log.isDebugEnabled()) log.debug("Invoking constructor on class {} using property {}", forClass, property);
        new ConstructorBase().construct(forClass, property, model);
        
        return model;
    }
    
    public StateBuilder getStateBuilder(Resource resource)
    {
        StateBuilder sb = StateBuilder.fromUri(resource.getURI(), resource.getModel());
        
        if (getResource().getLimit() != null) sb.replaceLiteral(GP.limit, getResource().getLimit());
        if (getResource().getOffset() != null) sb.replaceLiteral(GP.offset, getResource().getOffset());
        if (getResource().getOrderBy() != null) sb.replaceLiteral(GP.orderBy, getResource().getOrderBy());
        if (getResource().getDesc() != null) sb.replaceLiteral(GP.desc, getResource().getDesc());
        
        return sb;
    }

    public Resource applyView(Resource resource, OntClass matchedOntClass)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        if (matchedOntClass == null) throw new IllegalArgumentException("OntClass cannot be null");

        Resource queryOrTemplate = matchedOntClass.getProperty(GP.query).getResource();
        if (!queryOrTemplate.hasProperty(RDF.type, SP.Query))
        {
            Resource spinTemplate = queryOrTemplate.getProperty(RDF.type).getResource();
            StmtIterator constraintIt = spinTemplate.listProperties(SPIN.constraint);
            StateBuilder viewBuilder = StateBuilder.fromResource(resource);
            try
            {
                while (constraintIt.hasNext())
                {
                    Statement stmt = constraintIt.next();
                    Resource constraint = stmt.getResource();
                    {
                        Resource predicate = constraint.getRequiredProperty(SPL.predicate).getResource();
                        String queryVarName = predicate.getLocalName();                        
                        String queryVarValue = getUriInfo().getQueryParameters(true).getFirst(queryVarName);
                        if (queryVarValue != null)
                        {
                            Node queryVarNode = NodeFactoryExtra.parseNode(queryVarValue);
                            viewBuilder.replaceProperty(ResourceFactory.createProperty(predicate.getURI()),
                                resource.getModel().asRDFNode(queryVarNode));
                        }
                    }
                }
                
                Resource view = viewBuilder.build().
                    addProperty(GP.viewOf, resource);
                
                if (resource.hasProperty(RDF.type, GP.Container))
                    view.addProperty(RDF.type, GP.Container);
                else
                    view.addProperty(RDF.type, GP.Document);
                
                return view;
            }
            finally
            {
                constraintIt.close();
            }
        }
        
        return resource;
    }
    
    public Resource addPagination(Resource container, Long limit, Long offset)
    {
        if (container == null) throw new IllegalArgumentException("Resource cannot be null");
        
	//if (matchedOntClass.equals(GP.Container) || hasSuperClass(matchedOntClass, GP.Container) ||
        Resource page = getStateBuilder(container).build().
            addProperty(GP.pageOf, container).
            addProperty(RDF.type, GP.Page);
        if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} gp:pageOf {}", page, container);

        if (limit != null)
        {
            if (offset == null) offset = Long.valueOf(0);

            if (offset >= limit)
            {
                Resource prev = getStateBuilder(container).
                    replaceLiteral(GP.offset, offset - limit).
                    build().
                    addProperty(GP.pageOf, container).
                    addProperty(RDF.type, GP.Page).
                    addProperty(XHV.next, page);

                if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", page, prev);
                page.addProperty(XHV.prev, prev);
            }

            Resource next = getStateBuilder(container).
                replaceLiteral(GP.offset, offset + limit).
                build().
                addProperty(GP.pageOf, container).
                addProperty(RDF.type, GP.Page).
                addProperty(XHV.prev, page);

            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", page, next);
            page.addProperty(XHV.next, next);
        }
        
        return container;
    }
     
    public boolean hasSuperClass(OntClass subClass, OntClass superClass)
    {
        ExtendedIterator<OntClass> it = subClass.listSuperClasses(false);
        
        try
        {
            while (it.hasNext())
            {
                OntClass nextClass = it.next();
                if (nextClass.equals(superClass) || hasSuperClass(nextClass, superClass)) return true;
            }
        }
        finally
        {
            it.close();
        }
        
        return false;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }
    
    public org.graphity.processor.model.Resource getResource()
    {
        if (!getUriInfo().getMatchedResources().isEmpty())
        {
            Object resource = getUriInfo().getMatchedResources().get(0);
            if (resource instanceof org.graphity.processor.model.Resource) return (org.graphity.processor.model.Resource)resource;
        }
        
        return null;
    }
    
    public OntClass getMatchedOntClass()
    {
        return getResource().getMatchedOntClass();
    }

    public Ontology getOntology()
    {
        return getResource().getOntology();
    }

    public Application getApplication()
    {
        return application;
    }
    
}
