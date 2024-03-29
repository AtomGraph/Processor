/*
 * Copyright 2020 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.processor.model;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public interface TemplateCall
{
    
    Template getTemplate();

    TemplateCall applyArguments(MultivaluedMap<String, String> queryParams);

    TemplateCall applyDefaults();
    
    StmtIterator listArguments();
    
    boolean hasArgument(Property predicate);
    
    Resource getArgument(Property predicate);
    
    boolean hasArgument(String varName, RDFNode object);
    
    Resource getArgument(String varName, RDFNode object);
    
    Statement getArgumentProperty(Property predicate);
    
    TemplateCall arg(Parameter param, RDFNode value);
    
    TemplateCall arg(Resource arg);
    
    TemplateCall validateOptionals();
    
    QuerySolutionMap getQuerySolutionMap();
    
    Resource build();
    
}
