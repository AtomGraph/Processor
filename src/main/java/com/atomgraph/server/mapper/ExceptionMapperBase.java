/*
 * Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.server.mapper;

import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.core.util.Link;
import com.atomgraph.core.util.ModelUtils;
import com.atomgraph.processor.util.RulePrinter;
import com.atomgraph.processor.util.TemplateCall;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.vocabulary.HTTP;
import java.net.URI;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.jena.query.Dataset;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

/**
 * Abstract base class for ExceptionMappers that build responses with exceptions as RDF resources.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
abstract public class ExceptionMapperBase
{

    @Context private Request request;
    @Context private Providers providers;
    @Context private UriInfo uriInfo;
    
    public Resource toResource(Exception ex, Response.StatusType status, Resource statusResource)
    {
        if (ex == null) throw new IllegalArgumentException("Exception cannot be null");
        if (status == null) throw new IllegalArgumentException("Response.Status cannot be null");

        Resource resource = ModelFactory.createDefaultModel().createResource().
                addProperty(RDF.type, HTTP.Response).
                addLiteral(HTTP.statusCodeValue, status.getStatusCode()).
                addLiteral(HTTP.reasonPhrase, status.getReasonPhrase());

        if (statusResource != null) resource.addProperty(HTTP.sc, statusResource);
        if (ex.getMessage() != null) resource.addLiteral(DCTerms.title, ex.getMessage());
        
        return resource;
    }
    
    public MediaTypes getMediaTypes()
    {
        ContextResolver<MediaTypes> cr = getProviders().getContextResolver(MediaTypes.class, null);
        return cr.getContext(MediaTypes.class);
    }
    
    public Response.ResponseBuilder getResponseBuilder(Dataset dataset)
    {
        Variant variant = getRequest().selectVariant(getVariants(getMediaTypes().getWritable(Dataset.class)));
        if (variant == null) return getResponseBuilder(dataset.getDefaultModel()); // if quads are not acceptable, fallback to responding with the default graph
        
        Response.ResponseBuilder builder = new com.atomgraph.core.model.impl.Response(getRequest(),
                dataset,
                new EntityTag(Long.toHexString(com.atomgraph.core.model.impl.Response.hashDataset(dataset))),
                variant).
                getResponseBuilder().
            header("Link", new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));
        if (getTemplateCall() != null) builder.header("Link", new Link(URI.create(getTemplateCall().getTemplate().getURI()), LDT.template.getURI(), null));

        if (getOntology() != null)
        {
            builder.header("Link", new Link(URI.create(getOntology().getURI()), LDT.ontology.getURI(), null));

            Reasoner reasoner = getOntology().getOntModel().getSpecification().getReasoner();
            if (reasoner instanceof GenericRuleReasoner)
            {
                List<Rule> rules = ((GenericRuleReasoner)reasoner).getRules();
                builder.header("Rules", RulePrinter.print(rules));
            }
        }
        
        return builder;
    }
    
    public Response.ResponseBuilder getResponseBuilder(Model model)
    {
        Variant variant = getRequest().selectVariant(getVariants(getMediaTypes().getWritable(Dataset.class)));

        Response.ResponseBuilder builder = new com.atomgraph.core.model.impl.Response(getRequest(),
                model,
                new EntityTag(Long.toHexString(ModelUtils.hashModel(model))),
                variant).
                getResponseBuilder().
            header("Link", new Link(getUriInfo().getBaseUri(), LDT.base.getURI(), null));
        if (getTemplateCall() != null) builder.header("Link", new Link(URI.create(getTemplateCall().getTemplate().getURI()), LDT.template.getURI(), null));
        
        if (getOntology() != null)
        {
            builder.header("Link", new Link(URI.create(getOntology().getURI()), LDT.ontology.getURI(), null));

            Reasoner reasoner = getOntology().getOntModel().getSpecification().getReasoner();
            if (reasoner instanceof GenericRuleReasoner)
            {
                List<Rule> rules = ((GenericRuleReasoner)reasoner).getRules();
                builder.header("Rules", RulePrinter.print(rules));
            }
        }
        
        return builder;
    }
    
    /**
     * Builds a list of acceptable response variants.
     * 
     * @param mediaTypes
     * @return supported variants
     */
    public List<Variant> getVariants(List<MediaType> mediaTypes)
    {
        return com.atomgraph.core.model.impl.Response.getVariantListBuilder(mediaTypes, getLanguages(), getEncodings()).build();
    }
    
    public List<Locale> getLanguages()
    {
        return new ArrayList<>();
    }

    public List<String> getEncodings()
    {
        return new ArrayList<>();
    }
        
    public Request getRequest()
    {
        return request;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
    public TemplateCall getTemplateCall()
    {
        ContextResolver<TemplateCall> cr = getProviders().getContextResolver(TemplateCall.class, null);
        return cr.getContext(TemplateCall.class);
    }

    public Ontology getOntology()
    {
        ContextResolver<Ontology> cr = getProviders().getContextResolver(Ontology.class, null);
        return cr.getContext(Ontology.class);
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}