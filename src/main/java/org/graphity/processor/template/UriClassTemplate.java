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

package org.graphity.processor.template;

import com.hp.hpl.jena.ontology.OntClass;
import com.sun.jersey.api.uri.UriTemplate;
import java.util.Comparator;
import java.util.Objects;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class UriClassTemplate extends ClassTemplate
{
    final private UriTemplate uriTemplate;

    static public final Comparator<UriClassTemplate> COMPARATOR = new Comparator<UriClassTemplate>()
    {

        @Override
        public int compare(UriClassTemplate template1, UriClassTemplate template2)
        {
            Double diff = template2.getPrecedence() - template1.getPrecedence();
            if (diff != 0) return diff.intValue();
            
            return UriTemplate.COMPARATOR.compare(template1.getUriTemplate(), template2.getUriTemplate());
        }

    };
        
    public UriClassTemplate(OntClass ontClass, Double precedence, UriTemplate uriTemplate)
    {
        super(ontClass, precedence);
        if (uriTemplate == null) throw new IllegalArgumentException("UriTemplate must not be null");
        this.uriTemplate = uriTemplate;
    }

    public final UriTemplate getUriTemplate()
    {
        return uriTemplate;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(getUriTemplate());
        hash = 59 * hash + Objects.hashCode(getPrecedence());
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final UriClassTemplate other = (UriClassTemplate) obj;
        if (!Objects.equals(getUriTemplate(), other.getUriTemplate())) return false;
        if (!Objects.equals(getPrecedence(), other.getPrecedence())) return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[").
        append(getUriTemplate()).
        append(", <").
        append(getOntClass().getURI()).
        append(">, ").
        append(Double.toString(getPrecedence())).
        append("]").
        toString();
    }
    
}