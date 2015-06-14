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
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.model.Hypermedia;
import org.graphity.processor.model.Modifiers;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class HypermediaProvider extends PerRequestTypeInjectableProvider<Context, Hypermedia> implements ContextResolver<Hypermedia>
{

    @Context private UriInfo uriInfo;
    @Context private Providers providers;

    public HypermediaProvider()
    {
        super(Hypermedia.class);
    }

    @Override
    public Injectable<Hypermedia> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<Hypermedia>()
	{
	    @Override
	    public Hypermedia getValue()
	    {
		return getHypermedia();
	    }
	};
    }

    @Override
    public Hypermedia getContext(Class<?> type)
    {
        return getHypermedia();
    }

    public Hypermedia getHypermedia()
    {
        return new Hypermedia(getUriInfo(), getModifiers(), getMatchedOntClass());
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
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

}
