/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.server.provider;

import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.util.TemplateCall;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import java.net.URI;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.apache.jena.rdf.model.ModelFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Provider
public class TemplateCallProvider extends PerRequestTypeInjectableProvider<Context, TemplateCall> implements ContextResolver<TemplateCall>
{

    @Context Providers providers;
    @Context UriInfo uriInfo;
    
    public TemplateCallProvider()
    {
        super(TemplateCall.class);
    }
    
    @Override
    public Injectable<TemplateCall> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<TemplateCall>()
	{
	    @Override
	    public TemplateCall getValue()
	    {
                return TemplateCallProvider.this.getTemplateCall();
	    }
	};
    }

    @Override
    public TemplateCall getContext(Class<?> type)
    {
        return getTemplateCall();
    }
    
    public TemplateCall getTemplateCall()
    {
        if (getTemplate() != null)
            return getTemplateCall(getTemplate(), getUriInfo().getAbsolutePath(), getUriInfo().getQueryParameters());
        
        return null;
    }
    
    public TemplateCall getTemplateCall(Template template, URI absolutePath, MultivaluedMap<String, String> queryParams)
    {
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
        if (absolutePath == null) throw new IllegalArgumentException("URI cannot be null");
        if (queryParams == null) throw new IllegalArgumentException("MultivaluedMap cannot be null");

        TemplateCall templateCall = TemplateCall.fromUri(absolutePath.toString(), ModelFactory.createDefaultModel(), template).
            applyArguments(queryParams). // apply URL query parameters
            applyDefaults().
            validateOptionals(); // validate (non-)optional arguments
        templateCall.build(); // build state URI
        
        return templateCall;
    }
    
    public Template getTemplate()
    {
	return getProviders().getContextResolver(Template.class, null).getContext(Template.class);
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
