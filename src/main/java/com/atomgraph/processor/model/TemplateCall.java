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
package com.atomgraph.processor.model;

import com.sun.jersey.api.uri.UriTemplate;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.http.NameValuePair;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import com.atomgraph.processor.query.QueryBuilder;
import com.atomgraph.processor.update.ModifyBuilder;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public interface TemplateCall extends OntResource, Comparable
{
    
    static public final Comparator<TemplateCall> COMPARATOR = new Comparator<TemplateCall>()
    {

        @Override
        public int compare(TemplateCall templateCall1, TemplateCall templateCall2)
        {
            Double diff = templateCall2.getPrecedence() - templateCall1.getPrecedence();
            if (diff != 0) return diff.intValue();
    
            Double priority1 = templateCall1.getTemplate().getPriority();
            Double priority2 = templateCall2.getTemplate().getPriority();
            diff = priority2 - priority1;
            if (diff != 0) return diff.intValue();                
            
            return UriTemplate.COMPARATOR.compare(templateCall1.getTemplate().getPath(), templateCall2.getTemplate().getPath());
        }

    };
    
    Template getTemplate();
 
    Double getPrecedence();

    /**
     * Gets a Map from ArgumentDescriptors to RDFNodes.
     * @return a Map from ArgumentDescriptors to RDFNodes
     */
    Map<Argument, RDFNode> getArgumentsMap();

    QueryBuilder getQueryBuilder(URI base);
        
    QueryBuilder getQueryBuilder(URI base, Model commandModel);

    ModifyBuilder getModifyBuilder(URI base);
    
    ModifyBuilder getModifyBuilder(URI base, Model commandModel);
 
    TemplateCall applyArguments(MultivaluedMap<String, String> queryParams);

    TemplateCall applyArguments(List<NameValuePair> queryParams);
    
    // StateBuilder applyTemplateCall(StateBuilder sb);
    
}
