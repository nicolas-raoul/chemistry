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
 *     Amelie Avramo, EntropySoft
 */
package org.apache.chemistry.atompub.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.stax.FOMFeed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.context.EmptyResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.axiom.om.OMElement;
import org.apache.chemistry.ContentAlreadyExistsException;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Inclusion;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.Paging;
import org.apache.chemistry.Property;
import org.apache.chemistry.RelationshipDirection;
import org.apache.chemistry.Repository;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Tree;
import org.apache.chemistry.UpdateConflictException;
import org.apache.chemistry.atompub.AtomPub;
import org.apache.chemistry.atompub.AtomPubCMIS;
import org.apache.chemistry.atompub.abdera.ChildrenElement;
import org.apache.chemistry.impl.simple.SimpleContentStream;
import org.apache.chemistry.impl.simple.SimpleListPage;
import org.apache.chemistry.impl.simple.SimpleObjectId;

/**
 * CMIS Collection for the children of an object.
 */
public class CMISChildrenCollection extends CMISObjectsCollection {

    protected static final Pattern PAT_PARAM_SKIP_COUNT = Pattern.compile("(.*[?&]"
            + AtomPubCMIS.PARAM_SKIP_COUNT + "=)(-?[0-9]+)(.*)");

    public CMISChildrenCollection(String type, String id, Repository repository) {
        super(type, "children", id, repository);
    }

    /*
     * ----- AbstractEntityCollectionAdapter -----
     */

    @Override
    public ResponseContext getFeed(RequestContext request) {
        SPI spi = getSPI(request);
        try {
            Feed feed;
            if (COLTYPE_DESCENDANTS.equals(getType())
                    || COLTYPE_FOLDER_TREE.equals(getType())) {
                Tree<ObjectEntry> tree = getEntriesTree(request, spi);
                feed = getFeedTree(tree, request, spi);
            } else {
                ListPage<ObjectEntry> entries = getEntries(request, spi);
                feed = createFeedBase(entries, request, spi);
                addFeedDetails(feed, entries, request, spi);
            }
            return buildGetFeedResponse(feed);
        } catch (ResponseContextException e) {
            return createErrorResponse(e);
        } finally {
            spi.close();
        }
    }

    public Feed getFeedTree(Tree<ObjectEntry> tree, RequestContext request,
            SPI spi) throws ResponseContextException {
        // no paging
        SimpleListPage<ObjectEntry> page = new SimpleListPage<ObjectEntry>();
        Feed feed = createFeedBase(page, request, spi);
        addFeedDetails(feed, tree.getChildren(), request, spi);
        return feed;
    }

    protected Feed createFeedBase(ListPage<ObjectEntry> entries,
            RequestContext request, SPI spi) throws ResponseContextException {
        Feed feed = super.createFeedBase(request);

        feed.addLink(getChildrenLink(id, request), AtomPub.LINK_SELF,
                AtomPub.MEDIA_TYPE_ATOM_FEED, null, null, -1);

        // TODO passe known parent id when for a tree, avoid SPI use
        // link to parent children feed, needs parent id
        ObjectEntry entry;
        entry = spi.getProperties(spi.newObjectId(id), null);
        if (entry == null) {
            throw new ResponseContextException("Not found: " + id, 404);
        }
        String pid = (String) entry.getValue(Property.PARENT_ID);
        if (pid != null) {
            feed.addLink(getChildrenLink(pid, request), AtomPub.LINK_UP,
                    AtomPub.MEDIA_TYPE_ATOM_FEED, null, null, -1);
        }

        // TODO don't add descendants links if no children
        feed.addLink(getDescendantsLink(pid, request), AtomPub.LINK_DOWN,
                AtomPubCMIS.MEDIA_TYPE_CMIS_TREE, null, null, -1);
        feed.addLink(getFolderTreeLink(pid, request),
                AtomPubCMIS.LINK_FOLDER_TREE, AtomPub.MEDIA_TYPE_ATOM_FEED,
                null, null, -1);

        // AtomPub paging
        // next
        if (entries.getHasMoreItems()) {
            // find next skipCount
            int skipCount = getParameter(request, AtomPubCMIS.PARAM_SKIP_COUNT,
                    0);
            skipCount += entries.size();
            // compute new URI
            String uri = request.getResolvedUri().toString();
            Matcher m = PAT_PARAM_SKIP_COUNT.matcher(uri);
            if (m.matches()) {
                uri = m.group(1) + skipCount + m.group(3);
            } else {
                char sep = uri.contains("?") ? '&' : '?';
                uri = uri + sep + AtomPubCMIS.PARAM_SKIP_COUNT + '='
                        + skipCount;
            }
            feed.addLink(uri, AtomPub.LINK_NEXT, AtomPub.MEDIA_TYPE_ATOM_FEED,
                    null, null, -1);
        }
        // TODO prev, first, last

        // CMIS paging: numItems
        int numItems = entries.getNumItems();
        if (numItems != -1) {
            FOMFeed fomFeed = (FOMFeed) feed;
            OMElement el = fomFeed.getOMFactory().createOMElement(
                    AtomPubCMIS.NUM_ITEMS);
            el.setText(Integer.toString(numItems));
            fomFeed.addChild(el);
        }

        return feed;
    }

    // called either with a List of ObjectEntry or a List of Tree<ObjectEntry>
    @SuppressWarnings("unchecked")
    protected void addFeedDetails(Feed feed, List<?> entries,
            RequestContext request, SPI spi) throws ResponseContextException {
        feed.setUpdated(new Date()); // TODO
        if (entries == null) {
            return;
        }
        for (Object ob : entries) {
            ObjectEntry entryObj;
            Tree<ObjectEntry> tree;
            if (ob instanceof ObjectEntry) {
                tree = null;
                entryObj = (ObjectEntry) ob;
            } else {
                tree = (Tree<ObjectEntry>) ob;
                entryObj = tree.getNode();
            }

            Entry e = feed.addEntry();
            IRI feedIri = new IRI(getFeedIriForEntry(entryObj, request));
            addEntryDetails(request, e, feedIri, entryObj);

            if (tree != null) {
                List<Tree<ObjectEntry>> children = tree.getChildren();
                if (children != null && !children.isEmpty()) {
                    CMISChildrenCollection adapter = new CMISChildrenCollection(
                            getType(), entryObj.getId(), repository);
                    Feed subFeed = adapter.getFeedTree(tree, request, spi);
                    e.addExtension(new ChildrenElement(
                            request.getAbdera().getFactory(), subFeed));
                }
            }

            if (isMediaEntry(entryObj, spi)) {
                addMediaContent(feedIri, e, entryObj, request);
            } else {
                addContent(e, entryObj, request);
            }
        }
    }

    @Override
    // unused but abstract
    public ListPage<ObjectEntry> getEntries(RequestContext request) {
        throw new UnsupportedOperationException();
    }

    public ListPage<ObjectEntry> getEntries(RequestContext request, SPI spi)
            throws ResponseContextException {
        ObjectId objectId = spi.newObjectId(id);
        Target target = request.getTarget();
        String properties = target.getParameter(AtomPubCMIS.PARAM_FILTER);
        String renditions = target.getParameter(AtomPubCMIS.PARAM_RENDITION_FILTER);
        String rel = target.getParameter(AtomPubCMIS.PARAM_INCLUDE_RELATIONSHIPS);
        RelationshipDirection relationships = RelationshipDirection.fromInclusion(rel);
        boolean allowableActions = getParameter(request,
                AtomPubCMIS.PARAM_INCLUDE_ALLOWABLE_ACTIONS, false);
        boolean policies = getParameter(request,
                AtomPubCMIS.PARAM_INCLUDE_POLICY_IDS, false);
        boolean acls = getParameter(request, AtomPubCMIS.PARAM_INCLUDE_ACL,
                false);
        Inclusion inclusion = new Inclusion(properties, renditions,
                relationships, allowableActions, policies, acls);
        String orderBy = target.getParameter(AtomPubCMIS.PARAM_ORDER_BY);
        int maxItems = getParameter(request, AtomPubCMIS.PARAM_MAX_ITEMS, 0);
        int skipCount = getParameter(request, AtomPubCMIS.PARAM_SKIP_COUNT, 0);
        Paging paging = new Paging(maxItems, skipCount);
        return spi.getChildren(objectId, inclusion, orderBy, paging);
    }

    public Tree<ObjectEntry> getEntriesTree(RequestContext request, SPI spi)
            throws ResponseContextException {
        ObjectId objectId = spi.newObjectId(id);
        Target target = request.getTarget();
        String properties = target.getParameter(AtomPubCMIS.PARAM_FILTER);
        String renditions = target.getParameter(AtomPubCMIS.PARAM_RENDITION_FILTER);
        String rel = target.getParameter(AtomPubCMIS.PARAM_INCLUDE_RELATIONSHIPS);
        RelationshipDirection relationships = RelationshipDirection.fromInclusion(rel);
        boolean allowableActions = getParameter(request,
                AtomPubCMIS.PARAM_INCLUDE_ALLOWABLE_ACTIONS, false);
        boolean policies = getParameter(request,
                AtomPubCMIS.PARAM_INCLUDE_POLICY_IDS, false);
        boolean acls = getParameter(request, AtomPubCMIS.PARAM_INCLUDE_ACL,
                false);
        Inclusion inclusion = new Inclusion(properties, renditions,
                relationships, allowableActions, policies, acls);
        int depth = getParameter(request, AtomPubCMIS.PARAM_DEPTH, -1);
        Tree<ObjectEntry> tree;
        if (COLTYPE_DESCENDANTS.equals(getType())) {
            String orderBy = target.getParameter(AtomPubCMIS.PARAM_ORDER_BY);
            tree = spi.getDescendants(objectId, depth, orderBy, inclusion);
        } else { // COLTYPE_FOLDER_TREE
            tree = spi.getFolderTree(objectId, depth, inclusion);
        }
        return tree;
    }

    @Override
    public ResponseContext putMedia(RequestContext request) {
        try {
            String id = getResourceName(request);
            ObjectEntry object = getEntry(id, request);
            putMedia(object, request.getContentType(), request.getSlug(),
                    request.getInputStream(), request);
            return buildCreateMediaResponse(getMediaLink(object.getId(),
                    request));
        } catch (IOException e) {
            return new EmptyResponseContext(500);
        } catch (ResponseContextException e) {
            return createErrorResponse(e);
        }
    }

    @Override
    public void putMedia(ObjectEntry entry, MimeType contentType, String slug,
            InputStream in, RequestContext request)
            throws ResponseContextException {
        SPI spi = getSPI(request);
        try {
            ContentStream cs = new SimpleContentStream(in,
                    contentType.toString(), slug);
            spi.setContentStream(entry, cs, true);
        } catch (IOException e) {
            throw new ResponseContextException(e.toString(), 500);
        } catch (UpdateConflictException e) {
            throw new ResponseContextException(e.toString(), 409); // Conflict
        } catch (ContentAlreadyExistsException e) {
            // cannot happen, overwrite = true
            throw new ResponseContextException(e.toString(), 409); // Conflict
        } finally {
            spi.close();
        }
    }

    protected ResponseContext buildCreateMediaResponse(String mediaLink) {
        EmptyResponseContext rc = new EmptyResponseContext(200);
        rc.setLocation(mediaLink);
        rc.setContentLocation(mediaLink);
        rc.setStatus(201);
        return rc;
    }

    @Override
    public void deleteMedia(String resourceName, RequestContext request)
            throws ResponseContextException {
        SPI spi = getSPI(request);
        try {
            String id = getResourceName(request);
            spi.deleteContentStream(new SimpleObjectId(id));
        } catch (UpdateConflictException e) {
            throw new ResponseContextException(e.toString(), 409); // Conflict
        } finally {
            spi.close();
        }
    }
}
