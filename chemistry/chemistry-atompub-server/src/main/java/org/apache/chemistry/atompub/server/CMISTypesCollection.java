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
package org.apache.chemistry.atompub.server;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.text.UrlEncoding;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.chemistry.CMIS;
import org.apache.chemistry.Paging;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.PropertyType;
import org.apache.chemistry.Repository;
import org.apache.chemistry.Type;
import org.apache.chemistry.atompub.AtomPub;
import org.apache.chemistry.atompub.AtomPubCMIS;
import org.apache.chemistry.atompub.abdera.PropertiesElement;

/**
 * CMIS Collection for the Types.
 */
public class CMISTypesCollection extends CMISCollection<Type> {

    protected boolean singleEntry;

    public CMISTypesCollection(String type, String id, Repository repository) {
        super(type, "typechildren", id, repository);
    }

    /*
     * ----- AbstractCollectionAdapter -----
     */

    @Override
    protected Feed createFeedBase(RequestContext request) {
        Factory factory = request.getAbdera().getFactory();
        Feed feed = factory.newFeed();
        feed.declareNS(CMIS.CMIS_NS, CMIS.CMIS_PREFIX);
        feed.declareNS(AtomPubCMIS.CMISRA_NS, AtomPubCMIS.CMISRA_PREFIX);
        feed.declareNS(AtomPub.XSI_NS, AtomPub.XSI_PREFIX);
        feed.setId(getId(request));
        feed.setTitle(getTitle(request));
        feed.addLink(request.getResolvedUri().toString(), "self");
        feed.addAuthor(getAuthor(request));
        feed.setUpdated(new Date()); // XXX fixed date
        return feed;
    }

    @Override
    public String getId(RequestContext request) {
        // TODO when descendants
        return "urn:x-id:types";
    }

    public String getTitle(RequestContext request) {
        return "Types";
    }

    @Override
    public String getAuthor(RequestContext request) {
        return "system";
    }

    /*
     * ----- AbstractEntityCollectionAdapter -----
     */

    @Override
    public ResponseContext getEntry(RequestContext request) {
        singleEntry = true;
        return super.getEntry(request);
    }

    @Override
    public String addEntryDetails(RequestContext request, Entry entry,
            IRI feedIri, Type type) throws ResponseContextException {
        boolean includePropertyDefinitions = singleEntry
                || getParameter(request,
                        AtomPubCMIS.PARAM_INCLUDE_PROPERTY_DEFINITIONS, false);
        Factory factory = request.getAbdera().getFactory();

        String tid = type.getId();
        String tpid = type.getParentId();

        entry.setId(getId(type));
        entry.setTitle(getTitle(type));
        entry.setUpdated(getUpdated(type));
        // no authors, feed has one
        String summary = type.getDescription();
        if (summary != null && summary.length() != 0) {
            entry.setSummary(summary);
        }

        String link = getLink(type, feedIri, request);
        entry.addLink(link, AtomPub.LINK_SELF);
        entry.addLink(link, AtomPub.LINK_EDIT);
        // alternate is mandated by Atom when there is no atom:content
        entry.addLink(link, AtomPub.LINK_ALTERNATE);
        // CMIS links
        if (tpid != null) {
            entry.addLink(getTypeLink(tpid, request), AtomPub.LINK_UP,
                    AtomPub.MEDIA_TYPE_ATOM_ENTRY, null, null, -1);
        }
        entry.addLink(getTypeChildrenLink(tid, request), AtomPub.LINK_DOWN,
                AtomPub.MEDIA_TYPE_ATOM_FEED, null, null, -1);
        entry.addLink(getTypeDescendantsLink(tid, request), AtomPub.LINK_DOWN,
                AtomPubCMIS.MEDIA_TYPE_CMIS_TREE, null, null, -1);
        entry.addLink(getTypeLink(type.getBaseType().getId(), request),
                AtomPub.LINK_DESCRIBED_BY, AtomPub.MEDIA_TYPE_ATOM_ENTRY, null,
                null, -1);

        // CMIS-specific
        Element te = factory.newElement(AtomPubCMIS.TYPE, entry);
        te.setAttributeValue(AtomPubCMIS.ID, tid);
        QName schemaType = CMIS.SCHEMA_TYPES.get(type.getBaseType());
        te.setAttributeValue(AtomPub.XSI_TYPE, schemaType.getPrefix() + ':'
                + schemaType.getLocalPart());
        Element el;
        // note: setText is called in a separate statement as JDK 5 has problems
        // compiling when it's on one line (compiler generics bug)
        el = factory.newElement(CMIS.ID, te);
        el.setText(tid);
        el = factory.newElement(CMIS.LOCAL_NAME, te);
        el.setText(type.getLocalName());
        URI localNamespace = type.getLocalNamespace();
        if (localNamespace != null) {
            el = factory.newElement(CMIS.LOCAL_NAMESPACE, te);
            el.setText(localNamespace.toString());
        }
        el = factory.newElement(CMIS.QUERY_NAME, te);
        el.setText(type.getQueryName());
        el = factory.newElement(CMIS.DISPLAY_NAME, te);
        el.setText(type.getDisplayName());
        el = factory.newElement(CMIS.BASE_ID, te);
        el.setText(type.getBaseType().getId());
        el = factory.newElement(CMIS.PARENT_ID, te);
        el.setText(tpid == null ? "" : tpid);
        el = factory.newElement(CMIS.DESCRIPTION, te);
        el.setText(type.getDescription());
        el = factory.newElement(CMIS.CREATABLE, te);
        el.setText(Boolean.toString(type.isCreatable()));
        el = factory.newElement(CMIS.FILEABLE, te);
        el.setText(Boolean.toString(type.isFileable()));
        el = factory.newElement(CMIS.QUERYABLE, te);
        el.setText(Boolean.toString(type.isQueryable()));
        el = factory.newElement(CMIS.CONTROLLABLE_POLICY, te);
        el.setText(Boolean.toString(type.isControllablePolicy()));
        el = factory.newElement(CMIS.CONTROLLABLE_ACL, te);
        el.setText(Boolean.toString(type.isControllableACL()));
        el = factory.newElement(CMIS.FULLTEXT_INDEXED, te);
        el.setText(Boolean.toString(type.isFulltextIndexed()));
        el = factory.newElement(CMIS.INCLUDED_IN_SUPERTYPE_QUERY, te);
        el.setText(Boolean.toString(type.isIncludedInSuperTypeQuery()));
        el = factory.newElement(CMIS.VERSIONABLE, te); // docs only
        el.setText(Boolean.toString(type.isVersionable()));
        el = factory.newElement(CMIS.CONTENT_STREAM_ALLOWED, te); // docs only
        el.setText(type.getContentStreamAllowed().toString()); // TODO null
        // TODO allowedSourceTypes, allowedTargetTypes
        if (includePropertyDefinitions) {
            for (PropertyDefinition pd : type.getPropertyDefinitions()) {
                QName qname;
                switch (pd.getType().ordinal()) {
                case PropertyType.STRING_ORD:
                    qname = CMIS.PROPERTY_STRING_DEFINITION;
                    break;
                case PropertyType.DECIMAL_ORD:
                    qname = CMIS.PROPERTY_DECIMAL_DEFINITION;
                    break;
                case PropertyType.INTEGER_ORD:
                    qname = CMIS.PROPERTY_INTEGER_DEFINITION;
                    break;
                case PropertyType.BOOLEAN_ORD:
                    qname = CMIS.PROPERTY_BOOLEAN_DEFINITION;
                    break;
                case PropertyType.DATETIME_ORD:
                    qname = CMIS.PROPERTY_DATETIME_DEFINITION;
                    break;
                case PropertyType.URI_ORD:
                    qname = CMIS.PROPERTY_URI_DEFINITION;
                    break;
                case PropertyType.ID_ORD:
                    qname = CMIS.PROPERTY_ID_DEFINITION;
                    break;
                case PropertyType.HTML_ORD:
                    qname = CMIS.PROPERTY_HTML_DEFINITION;
                    break;
                default:
                    throw new AssertionError(pd.getType().name());
                }
                Element def = factory.newElement(qname, te);
                el = factory.newElement(CMIS.ID, def);
                el.setText(pd.getId());
                String localName = pd.getLocalName();
                if (localName != null) {
                    el = factory.newElement(CMIS.LOCAL_NAME, def);
                    el.setText(localName);
                }
                URI lns = pd.getLocalNamespace();
                if (lns != null) {
                    el = factory.newElement(CMIS.LOCAL_NAMESPACE, def);
                    el.setText(lns.toString());
                }
                el = factory.newElement(CMIS.QUERY_NAME, def);
                el.setText(pd.getQueryName());
                el = factory.newElement(CMIS.DISPLAY_NAME, def);
                el.setText(pd.getDisplayName());
                el = factory.newElement(CMIS.DESCRIPTION, def);
                el.setText(pd.getDescription());
                el = factory.newElement(CMIS.PROPERTY_TYPE, def);
                el.setText(pd.getType().name());
                el = factory.newElement(CMIS.CARDINALITY, def);
                el.setText(pd.isMultiValued() ? "multi" : "single");
                el = factory.newElement(CMIS.UPDATABILITY, def);
                el.setText(pd.getUpdatability().toString()); // TODO null
                el = factory.newElement(CMIS.INHERITED, def);
                el.setText(pd.isInherited() ? "true" : "false");
                el = factory.newElement(CMIS.REQUIRED, def);
                el.setText(pd.isRequired() ? "true" : "false");
                el = factory.newElement(CMIS.QUERYABLE, def);
                el.setText(pd.isQueryable() ? "true" : "false");
                el = factory.newElement(CMIS.ORDERABLE, def);
                el.setText(pd.isOrderable() ? "true" : "false");
                Serializable defaultValue = pd.getDefaultValue();
                if (defaultValue != null) {
                    Element dv = factory.newElement(CMIS.DEFAULT_VALUE, def);
                    for (String s : PropertiesElement.getStringsForValue(
                            defaultValue, pd.getType(), pd.isMultiValued())) {
                        el = factory.newElement(CMIS.VALUE, dv);
                        el.setText(s);
                    }
                }
                // TODO choices
                switch (pd.getType().ordinal()) {
                case PropertyType.STRING_ORD:
                    // TODO maxLength
                    break;
                case PropertyType.DECIMAL_ORD:
                    // TODO precision
                    break;
                case PropertyType.INTEGER_ORD:
                    // TODO minValue/maxValue
                    break;
                case PropertyType.BOOLEAN_ORD:
                    break;
                case PropertyType.DATETIME_ORD:
                    break;
                case PropertyType.URI_ORD:
                    break;
                case PropertyType.ID_ORD:
                    break;
                case PropertyType.HTML_ORD:
                    break;
                default:
                    throw new AssertionError(pd.getType().name());
                }
                // end property definition
            }
        }
        // end property definitions
        return link;
    }

    @Override
    public Iterable<Type> getEntries(RequestContext request)
            throws ResponseContextException {
        boolean includePropertyDefinitions = getParameter(request,
                AtomPubCMIS.PARAM_INCLUDE_PROPERTY_DEFINITIONS, false);
        if (CMISObjectsCollection.COLTYPE_DESCENDANTS.equals(getType())) {
            int depth = getParameter(request, AtomPubCMIS.PARAM_DEPTH, -1);
            return repository.getTypeDescendants(id, depth,
                    includePropertyDefinitions);
        } else { // children
            int maxItems = getParameter(request, AtomPubCMIS.PARAM_MAX_ITEMS, 0);
            int skipCount = getParameter(request, AtomPubCMIS.PARAM_SKIP_COUNT,
                    0);
            return repository.getTypeChildren(id, includePropertyDefinitions,
                    new Paging(maxItems, skipCount));
        }
    }

    @Override
    public Type postEntry(String title, IRI id, String summary, Date updated,
            List<Person> authors, Content content, RequestContext request)
            throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putEntry(Type type, String title, Date updated,
            List<Person> authors, String summary, Content content,
            RequestContext request) throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEntry(String resourceName, RequestContext request)
            throws ResponseContextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getContent(Type type, RequestContext request)
            throws ResponseContextException {
        return null;
    }

    @Override
    public Type getEntry(String typeId, RequestContext request)
            throws ResponseContextException {
        return repository.getType(typeId);
    }

    @Override
    public String getResourceName(RequestContext request) {
        String resourceName = request.getTarget().getParameter("typeid");
        return UrlEncoding.decode(resourceName);
    }

    @Override
    protected String getLink(Type type, IRI feedIri, RequestContext request) {
        return getTypeLink(type.getId(), request);
    }

    @Override
    public String getName(Type type) {
        throw new UnsupportedOperationException(); // unused
    }

    @Override
    public String getId(Type type) {
        return "urn:x-tid:" + type.getId();
    }

    @Override
    public String getTitle(Type type) {
        return type.getDisplayName();
    }

    @Override
    public Date getUpdated(Type type) {
        // XXX TODO mandatory field
        return new Date();
    }

}
