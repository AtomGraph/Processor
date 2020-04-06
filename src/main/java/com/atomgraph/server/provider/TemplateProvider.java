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
import org.apache.jena.ontology.Ontology;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import com.atomgraph.processor.util.TemplateMatcher;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template provider.
 * 
 * @see com.atomgraph.processor.model.Template
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
@Deprecated
public class TemplateProvider implements Factory<Template> // extends PerRequestTypeInjectableProvider<Context, Template> implements ContextResolver<Template>
{

    private static final Logger log = LoggerFactory.getLogger(TemplateProvider.class);

    @Context UriInfo uriInfo;
    @Context Providers providers;
    
    @Inject Ontology ontology;
    
    
//    public TemplateProvider()
//    {
//        super(Template.class);
//    }
//    
//    @Override
//    public Injectable<Template> getInjectable(ComponentContext ic, Context a)
//    {
//        return new Injectable<Template>()
//        {
//            @Override
//            public Template getValue()
//            {
//                return getTemplate();
//            }
//        };
//    }
//
//    @Override
//    public Template getContext(Class<?> type)
//    {
//        return getTemplate();
//    }
    

    @Override
    public Template provide()
    {
        return getTemplate();
    }

    @Override
    public void dispose(Template t)
    {
    }

    public Template getTemplate()
    {
        if (getOntology() != null) return getTemplate(getOntology(), getUriInfo());
        
        return null;
    }

    public Template getTemplate(Ontology ontology, UriInfo uriInfo)
    {
        return new TemplateMatcher(ontology).match(uriInfo.getAbsolutePath(), uriInfo.getBaseUri());
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    public Ontology getOntology()
    {
        return ontology;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
    
}
