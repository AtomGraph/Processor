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

import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.graphity.core.model.QueriedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class QueriedResourceProvider extends PerRequestTypeInjectableProvider<Context, QueriedResource> implements ContextResolver<QueriedResource>
{
    private static final Logger log = LoggerFactory.getLogger(QueriedResourceProvider.class);

    @Context UriInfo uriInfo;
    @Context ResourceContext resourceContext;

    public QueriedResourceProvider()
    {
        super(QueriedResource.class);
    }

    @Override
    public Injectable<QueriedResource> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<QueriedResource>()
	{
	    @Override
	    public QueriedResource getValue()
	    {
		return getQueriedResource();
	    }
	};
    }

    @Override
    public QueriedResource getContext(Class<?> type)
    {
        return getQueriedResource();
    }

    public QueriedResource getQueriedResource()
    {
        return getResourceContext().matchResource(getUriInfo().getRequestUri(),
                org.graphity.core.model.QueriedResource.class);
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }

    public ResourceContext getResourceContext()
    {
        return resourceContext;
    }
    
}
