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

import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application provider.
 * 
 * @see com.atomgraph.processor.model.Application
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
public class ApplicationProvider // extends PerRequestTypeInjectableProvider<Context, Application> implements ContextResolver<Application>
{

    private static final Logger log = LoggerFactory.getLogger(ApplicationProvider.class);
    
    //private final Application application;
    
//    public ApplicationProvider(Application application)
//    {
//        super(Application.class);
//        this.application = application;
//    }
//    
//    @Override
//    public Injectable<Application> getInjectable(ComponentContext ic, Context a)
//    {
//        return new Injectable<Application>()
//        {
//            @Override
//            public Application getValue()
//            {
//                return getApplication();
//            }
//        };
//    }
//
//    @Override
//    public Application getContext(Class<?> type)
//    {
//        return getApplication();
//    }
//    
//    public Application getApplication()
//    {
//        return application;
//    }

}
