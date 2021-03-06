/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 *     Florent Guillaume, Nuxeo
 */
package org.apache.chemistry.impl.simple;

import java.io.Serializable;

import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Updatability;

/**
 * A live property of an object.
 */
public class SimpleProperty implements Property {

    protected static final String CONTENT_BYTES_KEY = "__content__";

    protected final ObjectEntry entry;

    protected final String id;

    private final PropertyDefinition propertyDefinition;

    public SimpleProperty(ObjectEntry entry, String id,
            PropertyDefinition propertyDefinition) {
        this.entry = entry;
        this.id = id;
        this.propertyDefinition = propertyDefinition;
    }

    public PropertyDefinition getDefinition() {
        return propertyDefinition;
    }

    public Serializable getValue() {
        return entry.getValue(id);
    }

    public void setValue(Serializable value) {
        if (propertyDefinition.getUpdatability() == Updatability.READ_ONLY) {
            throw new RuntimeException("Read-only property: " + id);
        }
        entry.setValue(id, value);
    }

}
