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

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.PerRequestTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import org.graphity.processor.util.Modifiers;
import org.graphity.processor.model.impl.ModifiersBase;
import org.graphity.processor.vocabulary.GP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
@Provider
public class ModifiersProvider extends PerRequestTypeInjectableProvider<Context, Modifiers> implements ContextResolver<Modifiers>
{

    @Context private UriInfo uriInfo;
    @Context private Providers providers;
    
    public ModifiersProvider()
    {
        super(Modifiers.class);
    }

    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
    @Override
    public Injectable<Modifiers> getInjectable(ComponentContext ic, Context a)
    {
	return new Injectable<Modifiers>()
	{
	    @Override
	    public Modifiers getValue()
	    {
		return getModifiers();
	    }
	};
    }

    @Override
    public Modifiers getContext(Class<?> type)
    {
        return getModifiers();
    }
    
    public OntClass getOntClass()
    {
	ContextResolver<OntClass> cr = getProviders().getContextResolver(OntClass.class, null);
	return cr.getContext(OntClass.class);
    }
    
    public Modifiers getModifiers()
    {
        return new ModifiersBase(getLimit(), getOffset(), getOrderBy(), getDesc());
    }

    public Long getOffset()
    {
        if (getUriInfo().getQueryParameters().containsKey(GP.offset.getLocalName()))
            return Long.parseLong(getUriInfo().getQueryParameters().getFirst(GP.offset.getLocalName()));
        else
        {
            Long defaultOffset = getLongValue(getOntClass(), GP.defaultOffset);
            if (defaultOffset == null) defaultOffset = Long.valueOf(0); // OFFSET is 0 by default
            return defaultOffset;
        }
    }
    
    public Long getLimit()
    {
        if (getUriInfo().getQueryParameters().containsKey(GP.limit.getLocalName()))
            return Long.parseLong(getUriInfo().getQueryParameters().getFirst(GP.limit.getLocalName()));
        else
            return getLongValue(getOntClass(), GP.defaultLimit);
    }
    
    public String getOrderBy()
    {
        if (getUriInfo().getQueryParameters().containsKey(GP.orderBy.getLocalName()))
            return getUriInfo().getQueryParameters().getFirst(GP.orderBy.getLocalName());
        else
            return getStringValue(getOntClass(), GP.defaultOrderBy);
    }

    public Boolean getDesc()
    {
        if (getUriInfo().getQueryParameters().containsKey(GP.desc.getLocalName()))
            return Boolean.parseBoolean(getUriInfo().getQueryParameters().getFirst(GP.orderBy.getLocalName()));
        else
            return getBooleanValue(getOntClass(), GP.defaultDesc) != null; // ORDERY BY is ASC() by default
    }
    
    public Long getLongValue(OntClass ontClass, AnnotationProperty property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("AnnotationProperty cannot be null");

        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getLong();
        
        return null;
    }

    public Boolean getBooleanValue(OntClass ontClass, AnnotationProperty property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("AnnotationProperty cannot be null");

        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getBoolean();
        
        return null;
    }

    public String getStringValue(OntClass ontClass, AnnotationProperty property)
    {
	if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
	if (property == null) throw new IllegalArgumentException("AnnotationProperty cannot be null");
        
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getString();
        
        return null;
    }

}
