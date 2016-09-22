package org.entermediadb.amazon;

import java.util.List;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;

public class S3RepositoryTest extends BaseEnterMediaTest
{
	
	
	public S3Repository getRepo()
	{
		
		S3Repository repo =  new S3Repository(); 
		//repo.setRoot(getRoot());
		repo.setExternalPath(getRoot().getAbsolutePath() + "/WEB-INF/s3cache" );
		repo.setPath("/WEB-INF/data/test/originals/bucket1");
		repo.setBucket("entermedia9");
		repo.setAccessKey("AKIAIWDXNUBEOPD626WQ");
		repo.setSecretKey("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");
		
		
		return repo;
	}
	
	public void testListing() throws Exception
	{
		//requires a mount to be setup in oemounts.xml
		MediaArchive archive = getMediaArchive();
		
		S3Repository repo = getRepo();
		List children = repo.getChildrenNames("/WEB-INF/data/test/originals/bucket1");
		assertTrue(children.size() == 2);

		ContentItem item = repo.getStub((String)children.get(0));
		assertNotNull(item.getName());
		
		ContentItem sitem = repo.get("/WEB-INF/data/test/originals/bucket1/sub1");
		assertTrue( sitem.isFolder() );
		assertTrue( sitem.exists() );

		sitem = repo.getStub("/WEB-INF/data/test/originals/bucket1/sub1");
		assertTrue( sitem.isFolder() );
		assertTrue( sitem.exists() );

		children = repo.getChildrenNames("/WEB-INF/data/test/originals/bucket1/sub1/sub2");
		assertTrue(children.size() == 2);

	}
	
	public void testPutAPI() throws Exception
	{
		//requires a mount to be setup in oemounts.xml
		MediaArchive archive = getMediaArchive();
		
		Page testfile = archive.getPageManager().getPage("/WEB-INF/server.png");
		assertTrue( testfile.exists() );
		S3Repository repo = getRepo();

		
		
		ContentItem itemsave = testfile.getContentItem();
		itemsave.setPath("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server.png");
		repo.put(itemsave);

		ContentItem i = repo.get("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server-rack-cabinet-mdMOVED.png");
		
		Page testfilecopy = archive.getPageManager().getPage("/WEB-INF/server_copy.png");
		archive.getPageManager().copyPage(testfile,testfilecopy);
		repo.move(testfilecopy.getContentItem(), i);

//		i = repo.get("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server-rack-cabinet-md.png");
//
//		assertNotNull(i);
//		assertTrue(i.exists());
		//assertTrue(i.getInputStream().available() > 0 );
	

		ContentItem stub = repo.getStub("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server-rack-cabinet-mdMOVED.png");
		assertNotNull(stub);
		assertTrue(stub.exists());
		assertFalse(stub.isFolder());
		assertTrue(stub.getLastModified() > 0 );
		
		
		String path = stub.getAbsolutePath();
		assertTrue( path.contains("/WEB-INF/s3cache/"));
//		URL url = getRepo().getPresignedURL("/clients/embed/index.html", now.getTime());
//		assertNotNull(url);
//		String urlstring = url.toString();
//		assertNotNull(urlstring);
		
//		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
//        while (true) {
//            String line = reader.readLine();
//            if (line == null) break;
//            assertTrue(line.contains("expired"));
//         
//        }
//        
//        reader.close();
        
//       
//        now.add(Calendar.DAY_OF_YEAR, 2);
//        
//        url = getRepo().getPresignedURL("/clients/embed/index.html", now.getTime());
//		
//    	urlstring = url.toString();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
//        while (true) {
//            String line = reader.readLine();
//            if (line == null) break;
//            assertFalse(line.contains("expired"));
//         
//        }        
		
//		assertTrue(getRepo().doesExist("/clients/embed/index.html"));
		
		
		//Page dest = archive.getPageManager().getPage("/" + archive.getCatalogId() + "/publishing/smartjog/sub/index.html");
//		archive.getPageManager().copyPage(testfile, dest);
//		assertTrue(dest.exists());
//		archive.getPageManager().removePage(dest);
//		assertFalse(dest.exists());
	}
}
