/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

import com.atomgraph.processor.model.Template;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Deprecated
public class TemplateCallImpl // implements TemplateCall // extends OntResourceImpl 
{
    
    private final Template template;
    
    protected TemplateCallImpl(Template template)
    {
        this.template = template;
    }
    
    //@Override
    public final Template getTemplate()
    {
        // SPIN uses Template registry instead:
        // return SPINModuleRegistry.get().getTemplate(s.getResource().getURI(), getModel());
        return template;
    }
    
    /*
    @Override
    public TemplateCall applyArguments(Map<Property, RDFNode> values)
    {
	if (values == null) throw new IllegalArgumentException("Value Map cannot be null");
        
        Iterator<Entry<Property, RDFNode>> entryIt = values.entrySet().iterator();
        
        while (entryIt.hasNext())
        {
            Entry<Property, RDFNode> entry = entryIt.next();
            addProperty(entry.getKey(), entry.getValue());
        }
        
        return this;
    }
    
    @Override
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
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(argName);
            if (queryParams.containsKey(argName))
            {            
                String argValue = queryParams.getFirst(argName);
                addProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            }
            else
                if (!arg.isOptional())
                    throw new ArgumentException(argName, getTemplate()); // TO-DO: throw as required
        }
        
        return this;
    }

    @Override
    public TemplateCall applyArguments(List<NameValuePair> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter list cannot be null");

        // iterate query params to find unrecognized ones
        for (NameValuePair param : queryParams)
        {
            Argument arg = getTemplate().getArgumentsMap().get(param.getName());
            if (arg == null) throw new ArgumentException(param.getName(), getTemplate());
        }
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(argName);
            String argValue = null;
            
            for (NameValuePair param : queryParams)
                if (param.getName().equals(argName))
                    argValue = param.getValue();

            if (argValue != null)
            {
                removeAll(arg.getPredicate());
                addProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            }
            else if (!arg.isOptional())
                throw new ArgumentException(argName, getTemplate()); // TO-DO: throw as required            
        }
        
        return this;
    }
    */
    
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(getTemplate().getURI()).
        append(">").
        append("]").
        toString();
    }

}
