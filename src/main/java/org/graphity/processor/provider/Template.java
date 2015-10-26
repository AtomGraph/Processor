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

package org.graphity.processor.provider;

import com.hp.hpl.jena.ontology.OntClass;
import com.sun.jersey.api.uri.UriTemplate;
import java.util.Comparator;
import java.util.Objects;
import org.graphity.processor.vocabulary.GP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Template implements Comparable
{

    final private OntClass ontClass;
    final private UriTemplate uriTemplate;
    final private Double precedence;

    static public final Comparator<Template> COMPARATOR = new Comparator<Template>()
    {

        @Override
        public int compare(Template template1, Template template2)
        {
            Double diff = template1.getPrecedence() - template2.getPrecedence();
            return diff.intValue();
        }

    };

    public Template(OntClass ontClass, Double precedence)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (precedence == null) throw new IllegalArgumentException("Double cannot be null");
        
        this.ontClass = ontClass;
        this.uriTemplate = new UriTemplate(ontClass.getPropertyValue(GP.uriTemplate).asLiteral().getString());
        this.precedence = precedence;
    }

    public final OntClass getOntClass()
    {
        return ontClass;
    }

    public final Double getPrecedence()
    {
        return precedence;
    }

    public final UriTemplate getUriTemplate()
    {
        return uriTemplate;
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

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(getOntClass());
        hash = 61 * hash + Objects.hashCode(getPrecedence());
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Template other = (Template) obj;
        if (!Objects.equals(getOntClass(), other.getOntClass())) return false;

        return Objects.equals(getPrecedence(), other.getPrecedence());
    }

    @Override
    public int compareTo(Object obj)
    {
        Template template = (Template)obj;
        return COMPARATOR.compare(this, template);
    }
    
}
