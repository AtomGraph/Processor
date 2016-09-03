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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.processor.vocabulary.HTTP;

/**
 * Abstract base class for ExceptionMappers that build responses with exceptions as RDF resources.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
abstract public class ExceptionMapperBase
{

    @Context private Request request;
    @Context private Providers providers;
    @Context private UriInfo uriInfo;
    
    public Resource toResource(Exception ex, Response.Status status, Resource statusResource)
    {
	if (ex == null) throw new IllegalArgumentException("Exception cannot be null");
	if (status == null) throw new IllegalArgumentException("Response.Status cannot be null");
	//if (statusResource == null) throw new IllegalArgumentException("Status Resource cannot be null");

        Resource resource = ModelFactory.createDefaultModel().createResource().
                addProperty(RDF.type, HTTP.Response).
                addLiteral(HTTP.statusCodeValue, status.getStatusCode()).
                addLiteral(HTTP.reasonPhrase, status.getReasonPhrase());
                //addLiteral(ResourceFactory.createProperty("http://atomgraph.com/processor/ns#message"), ex.getStackTrace());

        if (statusResource != null) resource.addProperty(HTTP.sc, statusResource);
        if (ex.getMessage() != null) resource.addLiteral(DCTerms.title, ex.getMessage());
        
        return resource;
    }
    
    public MediaTypes getMediaTypes()
    {
	ContextResolver<MediaTypes> cr = getProviders().getContextResolver(MediaTypes.class, null);
	return cr.getContext(MediaTypes.class);
    }

    public Variant getVariant()
    {
        return getRequest().selectVariant(getVariants());
    }
    
    public List<Variant> getVariants()
    {
        return getVariantListBuilder().add().build();
    }

    public Variant.VariantListBuilder getVariantListBuilder()
    {
        com.atomgraph.core.model.impl.Response response = com.atomgraph.core.model.impl.Response.fromRequest(getRequest());
        return response.getVariantListBuilder(getModelMediaTypes(), getLanguages(), getEncodings());
    }
    
    public List<MediaType> getModelMediaTypes()
    {
        return getMediaTypes().getWritable(Model.class);
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