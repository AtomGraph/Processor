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
package org.graphity.processor.model;

import com.sun.jersey.api.uri.UriTemplate;
import java.util.Comparator;
import org.apache.jena.ontology.OntResource;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
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
    
            Double priority1 = templateCall1.getTemplate().getPriority() == null ?
                    Double.valueOf(0) : templateCall1.getTemplate().getPriority();
            Double priority2 = templateCall2.getTemplate().getPriority() == null ?
                    Double.valueOf(0) : templateCall2.getTemplate().getPriority();
            diff = priority2 - priority1;
            if (diff != 0) return diff.intValue();                
            
            return UriTemplate.COMPARATOR.compare(templateCall1.getTemplate().getPath(), templateCall2.getTemplate().getPath());
        }

    };
    
    public Template getTemplate();
 
    public Double getPrecedence();
    
}
