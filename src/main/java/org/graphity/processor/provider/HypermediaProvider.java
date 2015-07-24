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

package org.graphity.processor.provider;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Ontology;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.model.HypermediaBase;
import org.graphity.processor.util.Modifiers;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class HypermediaProvider extends PerRequestTypeInjectableProvider<Context, HypermediaBase> implements ContextResolver<HypermediaBase>
{

    @Context private ServletConfig servletConfig;
    @Context private UriInfo uriInfo;
    @Context private Providers providers;

    public HypermediaProvider()
    {
        super(HypermediaBase.class);
    }

    @Override
    public Injectable<HypermediaBase> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<HypermediaBase>()
	{
	    @Override
	    public HypermediaBase getValue()
	    {
		return getHypermedia();
	    }
	};
    }

    @Override
    public HypermediaBase getContext(Class<?> type)
    {
        return getHypermedia();
    }

    public HypermediaBase getHypermedia()
    {
        return new HypermediaBase(getServletConfig(), getUriInfo(), getModifiers(), getOntology(), getMatchedOntClass());
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    public Providers getProviders()
    {
        return providers;
    }
    
    public Modifiers getModifiers()
    {
	ContextResolver<Modifiers> cr = getProviders().getContextResolver(Modifiers.class, null);
	return cr.getContext(Modifiers.class);
    }

    public OntClass getMatchedOntClass()
    {
	ContextResolver<OntClass> cr = getProviders().getContextResolver(OntClass.class, null);
	return cr.getContext(OntClass.class);
    }

    public Ontology getOntology()
    {
	ContextResolver<Ontology> cr = getProviders().getContextResolver(Ontology.class, null);
	return cr.getContext(Ontology.class);
    }
    
}