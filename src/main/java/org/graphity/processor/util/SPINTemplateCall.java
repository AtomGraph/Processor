/*
 * Copyright 2016 Martynas Jusevičius <martynas@graphity.org>.
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
package org.graphity.processor.util;

import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.http.NameValuePair;
import org.graphity.processor.exception.SPINArgumentException;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.TemplateCall;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class SPINTemplateCall
{
    
    private TemplateCall templateCall;
    
    public SPINTemplateCall(TemplateCall templateCall)
    {
	if (templateCall == null) throw new IllegalArgumentException("Templatecall cannot be null");        
        this.templateCall = templateCall;
    }
    
    public TemplateCall applyArguments(MultivaluedMap<String, String> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter map cannot be null");

        // iterate query params to find unrecognized ones
        Set<String> paramNames = queryParams.keySet();
        for (String paramName : paramNames)
        {
            Argument arg = templateCall.getTemplate().getArgumentsMap().get(paramName);
            if (arg == null) throw new SPINArgumentException(paramName, templateCall.getTemplate());
        }
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = templateCall.getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = templateCall.getTemplate().getArgumentsMap().get(argName);
            if (queryParams.containsKey(argName))
            {            
                String argValue = queryParams.getFirst(argName);
                templateCall.addProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            }
            else
                if (!arg.isOptional())
                    throw new SPINArgumentException(argName, templateCall.getTemplate()); // TO-DO: throw as required
        }
        
        return templateCall;
    }

    public TemplateCall applyArguments(List<NameValuePair> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter list cannot be null");

        // iterate query params to find unrecognized ones
        //Set<String> paramNames = queryParams.keySet();
        for (NameValuePair param : queryParams)
        {
            Argument arg = templateCall.getTemplate().getArgumentsMap().get(param.getName());
            if (arg == null) throw new SPINArgumentException(param.getName(), templateCall.getTemplate());
        }
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = templateCall.getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = templateCall.getTemplate().getArgumentsMap().get(argName);
            String argValue = null;
            
            for (NameValuePair param : queryParams)
                if (param.getName().equals(argName))
                    argValue = param.getValue();

            if (argValue != null)
                templateCall.addProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            else if (!arg.isOptional())
                throw new SPINArgumentException(argName, templateCall.getTemplate()); // TO-DO: throw as required            
        }
        
        return templateCall;
    }
    
}
