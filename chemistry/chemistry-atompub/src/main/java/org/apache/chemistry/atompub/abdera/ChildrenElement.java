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
package org.apache.chemistry.atompub.abdera;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElementWrapper;
import org.apache.abdera.model.Feed;
import org.apache.chemistry.atompub.AtomPubCMIS;

/**
 * Element wrapping for a cmisra:children that itself just wraps an atom:feed.
 */
public class ChildrenElement extends ExtensibleElementWrapper {

    /**
     * Constructor used when parsing XML.
     */
    public ChildrenElement(Element internal) {
        super(internal);
    }

    /**
     * Constructor used when generating XML.
     */
    public ChildrenElement(Factory factory, Feed feed) {
        super(factory, AtomPubCMIS.CHILDREN);
        addExtension(feed);
    }

}
