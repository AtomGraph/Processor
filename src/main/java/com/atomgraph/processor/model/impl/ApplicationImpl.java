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
package com.atomgraph.processor.model.impl;

import com.atomgraph.core.model.Service;
import com.atomgraph.processor.model.Application;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ApplicationImpl implements Application
{
    private final Resource ontology;
    private final Service service;

    public ApplicationImpl(Service service, Resource ontology)
    {
        if (ontology == null) throw new IllegalArgumentException("Resource cannot be null");
        if (service == null) throw new IllegalArgumentException("Service cannot be null");
        this.ontology = ontology;
        this.service = service;
    }
    
    @Override
    public Resource getOntology()
    {
        return ontology;
    }
    
    @Override
    public Service getService()
    {
        return service;
    }
    
}
