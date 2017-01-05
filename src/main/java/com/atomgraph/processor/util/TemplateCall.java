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
package com.atomgraph.processor.util;

import com.atomgraph.processor.exception.ArgumentException;
import com.atomgraph.processor.model.Argument;
import com.atomgraph.processor.model.Template;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.spin.model.SPINFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCall extends com.atomgraph.core.util.StateBuilder
{
    
    private final Template template;
    
    protected TemplateCall(Resource resource, Template template)
    {
        super(UriBuilder.fromUri(resource.getURI()), resource.getModel());
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
        this.template = template;        
    }
    
    public static TemplateCall fromUri(String uri, Model model, Template template)
    {
        if (uri == null) throw new IllegalArgumentException("URI String cannot be null");        
        if (model == null) throw new IllegalArgumentException("Model cannot be null");        
        
        return new TemplateCall(model.createResource(uri), template);
    }

    public static TemplateCall fromResource(Resource resource, Template template)
    {
        return new TemplateCall(resource, template);
    }
    
    public final Template getTemplate()
    {
        // SPIN uses Template registry instead:
        // return SPINModuleRegistry.get().getTemplate(s.getResource().getURI(), getModel());
        return template;
    }

    public TemplateCall applyArguments(Map<Property, RDFNode> values)
    {
	if (values == null) throw new IllegalArgumentException("Value Map cannot be null");
        
        Set<Entry<Property, RDFNode>> entries = values.entrySet();        
        for (Entry<Property, RDFNode> entry : entries)
            replaceProperty(entry.getKey(), entry.getValue());
        
        return this;
    }
    
    public TemplateCall applyArguments(MultivaluedMap<String, String> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter map cannot be null");

        // iterate query params to find unrecognized ones
        Set<String> paramNames = queryParams.keySet();
        for (String paramName : paramNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(paramName);
            if (arg == null) throw new ArgumentException(paramName, getTemplate());
        }
        
        // iterate arguments to find those that match query param names
        Set<String> argNames = getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(argName);
            if (queryParams.containsKey(argName))
            {            
                String argValue = queryParams.getFirst(argName);
                // TO-DO: allow multiple query param values?
                replaceProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            }
        }
        
        return this;
    }
    
    public TemplateCall validateOptionals()
    {
        Set<Entry<Property, Argument>> argEntries = getTemplate().getArguments().entrySet();
        for (Entry<Property, Argument> entry : argEntries)
        {
            if (!getResource().hasProperty(entry.getKey()) && !entry.getValue().isOptional())
                throw new ArgumentException(entry.getValue(), getTemplate());
        }
        
        return this;
    }
    
    public QuerySolutionMap getQuerySolutionMap()
    {
        QuerySolutionMap qsm = new QuerySolutionMap();
        
        org.topbraid.spin.model.TemplateCall spinTemplateCall = SPINFactory.asTemplateCall(getTemplate().getQuery());
        if (spinTemplateCall != null)
        {
            qsm = spinTemplateCall.getInitialBinding();
            
            List<org.topbraid.spin.model.Argument> spinArgs = spinTemplateCall.getTemplate().getArguments(false);
            // add SPIN Arguments that match LDT Arguments (by predicate)
            for (org.topbraid.spin.model.Argument spinArg : spinArgs)
                if (getTemplate().getArguments().containsKey(spinArg.getPredicate()) &&
                        getResource().hasProperty(spinArg.getPredicate()))
                {
                    Argument arg = getTemplate().getArguments().get(spinArg.getPredicate());
                    qsm.add(arg.getVarName(), getResource().getProperty(arg.getPredicate()).getObject());
                }
        }
                
        return qsm;
    }
    
    /*
    public QuerySolutionMap getQuerySolutionMap()
    {
        QuerySolutionMap qsm = new QuerySolutionMap();
        
        Set<Entry<String, Argument>> argEntries = getTemplate().getArgumentsMap().entrySet();
        for (Entry<String, Argument> entry : argEntries)
            if (getResource().hasProperty(entry.getValue().getPredicate()))
            {
                RDFNode value = getResource().getProperty(entry.getValue().getPredicate()).getObject();
                qsm.add(entry.getKey(), value);
            }
        
        return qsm;
    }
    */
    
    /*
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(build().getURI()).
        append(">").
        append("[<").
        append(getTemplate().getURI()).
        append(">").
        append("]").
        toString();
    }
    */

}
