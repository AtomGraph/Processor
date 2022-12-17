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

package com.atomgraph.processor.util;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import java.net.URI;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.uri.UriComponent;

/**
 * Builds application state as RDF resource from URL query parameters.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://en.wikipedia.org/wiki/HATEOAS">HATEOAS</a>
 * @see <a href="https://atomgraph.github.io/Linked-Data-Templates/#func-state">LDT state function</a>
 */
public class StateBuilder
{
    private Resource resource;
    private final UriBuilder uriBuilder;
    
    protected StateBuilder(UriBuilder uriBuilder, Model model)
    {
        if (uriBuilder == null) throw new IllegalArgumentException("UriBuilder cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
        resource = model.createResource();
        this.uriBuilder = uriBuilder;
    }
    
    public static StateBuilder fromUri(URI uri, Model model)
    {
        return new StateBuilder(UriBuilder.fromUri(uri), model);
    }

    public static StateBuilder fromUri(String uri, Model model)
    {
        return new StateBuilder(UriBuilder.fromUri(uri), model);
    }

    public static StateBuilder fromResource(Resource resource)
    {
        return new StateBuilder(UriBuilder.fromUri(resource.getURI()), resource.getModel());
    }
    
    protected Resource getResource()
    {
        return resource;
    }
    
    protected UriBuilder getUriBuilder()
    {
        return uriBuilder;
    }

    public StateBuilder property(Property property, RDFNode value)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        if (value == null) throw new IllegalArgumentException("RDFNode cannot be null");

        getResource().addProperty(property, value);
        String encodedValue = value.toString(); // not a reliable serialization
        // we URI-encode values ourselves because Jersey 1.x fails to do so: https://java.net/jira/browse/JERSEY-1717
        if (value.isURIResource()) encodedValue = UriComponent.encode(value.asResource().getURI(), UriComponent.Type.UNRESERVED);
        if (value.isLiteral()) encodedValue = UriComponent.encode(value.asLiteral().getString(), UriComponent.Type.UNRESERVED);
        getUriBuilder().queryParam(property.getLocalName(), encodedValue);
        
        return this;
    }

    public StateBuilder replaceProperty(Property property, RDFNode value)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");

        getResource().removeAll(property);
        getUriBuilder().replaceQueryParam(property.getLocalName(), (Object[])null);
        
        if (value != null) property(property, value);
        
        return this;
    }
        
    public Resource build()
    {
        resource = ResourceUtils.renameResource(getResource(), getUriBuilder().build().toString());
        return resource;
    }

    @Override
    public String toString()
    {
        return getResource().listProperties().toList().toString();
    }
    
}