/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.server.factory;

import java.util.Optional;
import javax.ws.rs.ext.Provider;
import org.apache.jena.ontology.Ontology;
import org.glassfish.hk2.api.Factory;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
@Provider
public class OntologyFactory implements Factory<Optional<Ontology>>
{

    private final Ontology ontology;
    
    public OntologyFactory(Ontology ontology)
    {
        this.ontology = ontology;
    }
    
    @Override
    public Optional<Ontology> provide()
    {
        return getOntology();
    }

    @Override
    public void dispose(Optional<Ontology> t)
    {
    }
    
    protected Optional<Ontology> getOntology()
    {
        return Optional.of(ontology);
    }
    
}
