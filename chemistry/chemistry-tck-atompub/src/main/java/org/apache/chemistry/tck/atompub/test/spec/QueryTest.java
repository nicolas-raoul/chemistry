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
 *     David Caruana, Alfresco
 *     Gabriele Columbro, Alfresco
 */
package org.apache.chemistry.tck.atompub.test.spec;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.chemistry.abdera.ext.CMISAllowableActions;
import org.apache.chemistry.abdera.ext.CMISCapabilities;
import org.apache.chemistry.abdera.ext.CMISConstants;
import org.apache.chemistry.abdera.ext.CMISObject;
import org.apache.chemistry.tck.atompub.TCKSkipCapabilityException;
import org.apache.chemistry.tck.atompub.TCKTest;
import org.apache.chemistry.tck.atompub.fixture.CMISTree;
import org.apache.chemistry.tck.atompub.fixture.EntryTree;
import org.apache.chemistry.tck.atompub.fixture.GatherRenditionsVisitor;
import org.apache.chemistry.tck.atompub.http.PostRequest;
import org.apache.chemistry.tck.atompub.http.Request;
import org.apache.chemistry.tck.atompub.http.Response;
import org.junit.Assert;


/**
 * CMIS Query Tests
 */
public class QueryTest extends TCKTest {
    
    private EntryTree folder;
    private CMISObject folderObject;
    private EntryTree document1;
    private CMISObject document1Object;
    private EntryTree document2;
    private CMISObject document2Object;
    private EntryTree document3;
    private CMISObject document3Object;

    @Override
    public void setUp() {
        super.setUp();

        try {
            folder = new EntryTree();
            folder.entry = fixture.getTestCaseFolder();
            folder.type = CMISConstants.TYPE_FOLDER;
            folder.children = new LinkedList<EntryTree>();
            folderObject = folder.entry.getExtension(CMISConstants.OBJECT);
            // create documents to query
            document1 = new EntryTree();
            folder.children.add(document1);
            document1.parent = folder.entry;
            document1.entry = fixture.createTestDocument("apple1");
            document1.type = CMISConstants.TYPE_DOCUMENT;
            document1Object = document1.entry.getExtension(CMISConstants.OBJECT);
            String doc2name = "name" + System.currentTimeMillis();
            document2 = new EntryTree();
            folder.children.add(document2);
            document2.parent = folder.entry;
            document2.entry = fixture.createTestDocument(doc2name);
            document2.type = CMISConstants.TYPE_DOCUMENT;
            document2Object = document2.entry.getExtension(CMISConstants.OBJECT);
            document3 = new EntryTree();
            folder.children.add(document3);
            document3.parent = folder.entry;
            document3.entry = fixture.createTestDocument("banana1");
            document3.type = CMISConstants.TYPE_DOCUMENT;
            document3Object = document3.entry.getExtension(CMISConstants.OBJECT);
        } catch (Exception e) {
            // TODO: appropriate exception handling
            throw new RuntimeException(e);
        }
    }

    public void testQueryFolderMetaData() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (!(capability.equals("metadataonly") || capability.equals("bothseperate") || capability.equals("bothcombined"))) {
        	throw new TCKSkipCapabilityException("query", "metadataonly or bothseparate or bothcombined", capability);
        }

        IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        String queryDoc = templates.load("query.cmisquery.xml");

        // meta data only query against folder
        // TODO: use property query name
        String query = 
                "SELECT * FROM cmis:folder " +
                "WHERE cmis:objectId = '" + folderObject.getObjectId().getStringValue() + "'";
        String queryReq = queryDoc.replace("${STATEMENT}", query);
        queryReq = queryReq.replace("${SKIPCOUNT}", "0");
        queryReq = queryReq.replace("${MAXITEMS}", "5");
        queryReq = queryReq.replace("${RENDITIONFILTER}", "cmis:none");

        Request postReq = new PostRequest(queryHREF.toString(), queryReq,  CMISConstants.MIMETYPE_CMIS_QUERY);
        Response queryRes = client.executeRequest(postReq, 201);
        Assert.assertNotNull(queryRes);
        Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
        Assert.assertNotNull(queryFeed);
        Assert.assertEquals(1, queryFeed.getEntries().size());
        Assert.assertNotNull(queryFeed.getEntry(folder.entry.getId().toString()));
        CMISObject result1 = queryFeed.getEntry(folder.entry.getId().toString()).getExtension(CMISConstants.OBJECT);
        Assert.assertEquals(folderObject.getName().getStringValue(), result1.getName().getStringValue());
        Assert.assertEquals(folderObject.getObjectId().getStringValue(), result1.getObjectId().getStringValue());
        Assert.assertEquals(folderObject.getObjectTypeId().getStringValue(), result1.getObjectTypeId().getStringValue());
    }

    public void testQueryDocumentMetaData() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (!(capability.equals("metadataonly") || capability.equals("bothseperate") || capability.equals("bothcombined"))) {
        	throw new TCKSkipCapabilityException("query", "metadataonly or bothseparate or bothcombined", capability);
        }

        IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        String queryDoc = templates.load("query.cmisquery.xml");

        // meta data only query against document
        // TODO: use property query name
        String query =
                "SELECT * FROM cmis:document " +
                "WHERE IN_FOLDER('" + folderObject.getObjectId().getStringValue() + "') " + 
                "AND cmis:name = 'apple1'";
        String queryReq = queryDoc.replace("${STATEMENT}", query);
        queryReq = queryReq.replace("${SKIPCOUNT}", "0");
        queryReq = queryReq.replace("${MAXITEMS}", "5");
        queryReq = queryReq.replace("${RENDITIONFILTER}", "cmis:none");

        Request postReq = new PostRequest(queryHREF.toString(), queryReq, CMISConstants.MIMETYPE_CMIS_QUERY);
        Response queryRes = client.executeRequest(postReq, 201);
        Assert.assertNotNull(queryRes);
        Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
        Assert.assertNotNull(queryFeed);
        Assert.assertEquals(1, queryFeed.getEntries().size());
        Assert.assertNotNull(queryFeed.getEntry(document1.entry.getId().toString()));
        CMISObject result1 = queryFeed.getEntry(document1.entry.getId().toString()).getExtension(CMISConstants.OBJECT);
        Assert.assertEquals(document1Object.getName().getStringValue(), result1.getName().getStringValue());
        Assert.assertEquals(document1Object.getObjectId().getStringValue(), result1.getObjectId().getStringValue());
        Assert.assertEquals(document1Object.getObjectTypeId().getStringValue(), result1.getObjectTypeId().getStringValue());
    }

    public void testQueryDocumentFullText() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (!(capability.equals("fulltextonly") || capability.equals("bothseperate") || capability.equals("bothcombined"))) {
        	throw new TCKSkipCapabilityException("query", "fulltextonly or bothseparate or bothcombined", capability);
        }

        IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        String queryDoc = templates.load("query.cmisquery.xml");

        // full text only query
        // TODO: use property query name
        String fullText = document2Object.getName().getStringValue();
        String query = 
                "SELECT cmis:objectId, cmis:objectTypeId, cmis:name FROM cmis:document " + 
                "WHERE CONTAINS('" + fullText + "')";
        String queryReq = queryDoc.replace("${STATEMENT}", query);
        queryReq = queryReq.replace("${SKIPCOUNT}", "0");
        queryReq = queryReq.replace("${MAXITEMS}", "5");
        queryReq = queryReq.replace("${RENDITIONFILTER}", "cmis:none");

        Request postReq = new PostRequest(queryHREF.toString(), queryReq, CMISConstants.MIMETYPE_CMIS_QUERY);
        Response queryRes = client.executeRequest(postReq, 201);
        Assert.assertNotNull(queryRes);
        Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
        Assert.assertNotNull(queryFeed);
        Assert.assertEquals(1, queryFeed.getEntries().size());
        Assert.assertNotNull(queryFeed.getEntry(document2.entry.getId().toString()));
        CMISObject result1 = queryFeed.getEntry(document2.entry.getId().toString()).getExtension(CMISConstants.OBJECT);
        Assert.assertEquals(document2Object.getName().getStringValue(), result1.getName().getStringValue());
        Assert.assertEquals(document2Object.getObjectId().getStringValue(), result1.getObjectId().getStringValue());
        Assert.assertEquals(document2Object.getObjectTypeId().getStringValue(), result1.getObjectTypeId().getStringValue());
    }

    public void testQueryDocumentMetaDataAndFullText() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (!capability.equals("bothcombined")) {
        	throw new TCKSkipCapabilityException("query", "bothcombined", capability);
        }

        IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        String queryDoc = templates.load("query.cmisquery.xml");

        // combined meta data and full text
        // TODO: use property query name
        String query = 
                "SELECT cmis:objectId, cmis:objectTypeId, cmis:name FROM cmis:document " + 
                "WHERE IN_FOLDER('" + folderObject.getObjectId().getStringValue() + "') " +
                "AND cmis:name = 'apple1' " +
                "AND CONTAINS('apple1')";
        String queryReq = queryDoc.replace("${STATEMENT}", query);
        queryReq = queryReq.replace("${SKIPCOUNT}", "0");
        queryReq = queryReq.replace("${MAXITEMS}", "5");
        queryReq = queryReq.replace("${RENDITIONFILTER}", "cmis:none");

        Request postReq = new PostRequest(queryHREF.toString(), queryReq, CMISConstants.MIMETYPE_CMIS_QUERY);
        Response queryRes = client.executeRequest(postReq, 201);
        Assert.assertNotNull(queryRes);
        Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
        Assert.assertNotNull(queryFeed);
        Assert.assertEquals(1, queryFeed.getEntries().size());
        Assert.assertNotNull(queryFeed.getEntry(document1.entry.getId().toString()));
        CMISObject result1 = queryFeed.getEntry(document1.entry.getId().toString()).getExtension(CMISConstants.OBJECT);
        Assert.assertEquals(document1Object.getName().getStringValue(), result1.getName().getStringValue());
        Assert.assertEquals(document1Object.getObjectId().getStringValue(), result1.getObjectId().getStringValue());
        Assert.assertEquals(document1Object.getObjectTypeId().getStringValue(), result1.getObjectTypeId().getStringValue());
    }

    public void testQueryDocumentRenditions() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (!capability.equals("bothcombined")) {
                throw new TCKSkipCapabilityException("query", "bothcombined", capability);
        }

        final IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        final String queryDoc = templates.load("query.cmisquery.xml");

        // combined meta data and full text
        // TODO: use property query name
        final String query = 
                "SELECT cmis:objectId, cmis:objectTypeId, cmis:name FROM cmis:document " + 
                "WHERE IN_FOLDER('" + folderObject.getObjectId().getStringValue() + "') " +
                "AND cmis:name = 'apple1' " +
                "AND CONTAINS('apple1')";
        
        GatherRenditionsVisitor visitor = new GatherRenditionsVisitor(client);
        visitor.testRenditions(folder, new GatherRenditionsVisitor.EntryGenerator(){

            public EntryTree getEntries(String renditionFilter) throws Exception {
                String queryReq = queryDoc.replace("${STATEMENT}", query);
                queryReq = queryReq.replace("${SKIPCOUNT}", "0");
                queryReq = queryReq.replace("${MAXITEMS}", "5");
                queryReq = queryReq.replace("${RENDITIONFILTER}", renditionFilter);

                Request postReq = new PostRequest(queryHREF.toString(), queryReq, CMISConstants.MIMETYPE_CMIS_QUERY);
                Response queryRes = client.executeRequest(postReq, 201);
                Assert.assertNotNull(queryRes);
                Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
                Assert.assertNotNull(queryFeed);
                Assert.assertEquals(1, queryFeed.getEntries().size());
                Assert.assertNotNull(queryFeed.getEntry(document1.entry.getId().toString()));
                CMISObject result1 = queryFeed.getEntry(document1.entry.getId().toString()).getExtension(CMISConstants.OBJECT);
                Assert.assertEquals(document1Object.getName().getStringValue(), result1.getName().getStringValue());
                Assert.assertEquals(document1Object.getObjectId().getStringValue(), result1.getObjectId().getStringValue());
                Assert.assertEquals(document1Object.getObjectTypeId().getStringValue(), result1.getObjectTypeId().getStringValue());
                return new CMISTree(folder, queryFeed);
            }});
    }

    public void testQueryAllowableActions() throws Exception {
        CMISCapabilities capabilities = client.getCapabilities();
        String capability = capabilities.getQuery();
        if (capability.equals("none")) {
        	throw new TCKSkipCapabilityException("query", "anything other than none", capability);
        }

        // retrieve query collection
        IRI queryHREF = client.getQueryCollection(client.getWorkspace());
        String queryDoc = templates.load("queryallowableactions.cmisquery.xml");

        // construct structured query
        String query = 
                "SELECT * FROM cmis:document " + 
                "WHERE IN_FOLDER('" + folderObject.getObjectId().getStringValue() + "') ";
        String queryReq = queryDoc.replace("${STATEMENT}", query);
        queryReq = queryReq.replace("${INCLUDEALLOWABLEACTIONS}", "true");
        queryReq = queryReq.replace("${SKIPCOUNT}", "0");
        queryReq = queryReq.replace("${MAXITEMS}", "5");
        queryReq = queryReq.replace("${RENDITIONFILTER}", "cmis:none");

        // issue structured query
        Request postReq = new PostRequest(queryHREF.toString(), queryReq, CMISConstants.MIMETYPE_CMIS_QUERY);
        Response queryRes = client.executeRequest(postReq, 201);
        Assert.assertNotNull(queryRes);
        Feed queryFeed = model.parseFeed(new StringReader(queryRes.getContentAsString()), null);
        Assert.assertNotNull(queryFeed);
        Assert.assertEquals(3, queryFeed.getEntries().size());

        for (Entry child : queryFeed.getEntries()) {
            // extract allowable actions from child
            CMISObject childObject = child.getExtension(CMISConstants.OBJECT);
            Assert.assertNotNull(childObject);
            CMISAllowableActions childAllowableActions = childObject.getExtension(CMISConstants.ALLOWABLE_ACTIONS);
            Assert.assertNotNull(childAllowableActions);

            // retrieve allowable actions from link
            Map<String, String> args = new HashMap<String, String>();
            args.put("includeAllowableActions", "true");
            Entry entry = client.getEntry(child.getSelfLink().getHref(), args);
            CMISObject entryObject = entry.getExtension(CMISConstants.OBJECT);
            Assert.assertNotNull(entryObject);
            CMISAllowableActions entryAllowableActions = entryObject.getExtension(CMISConstants.ALLOWABLE_ACTIONS);

            // compare the two
            AllowableActionsTest.compareAllowableActions(childAllowableActions, entryAllowableActions);
        }
    }

}
