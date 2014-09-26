package model.amazon;

import java.util.List;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

import com.openedit.page.Page;

public class S3RepositoryTest extends BaseEnterMediaTest
{
	
	
	public S3Repository getRepo()
	{
		//MediaArchive archive = getMediaArchive();

//		
//		X509TrustManager tm = new X509TrustManager() {
//			 
//			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//			}
//			 
//			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//			}
//			 
//			public X509Certificate[] getAcceptedIssuers() {
//			return null;
//			}
//			};
		
		//Object obj = archive.getModuleManager().getBean(archive.getCatalogId(), "S3Repository");;
//		System.out.println( obj.class );
//		System.out.println( S3Repository.getClass() );
//		
		
//		 <property name="accesskey">AKIAJ3I5YRDDGXXX</property>
//		  <property name="secretkey">XD1CeXAhP5imBp3wvRKVUUz3xYnVqpiXXX</property>
		
		
//		Repository brepo = (Repository)obj;
		S3Repository repo =  new S3Repository(); 
		//repo.setRoot(getRoot());
		repo.setExternalPath(getRoot().getAbsolutePath() + "/WEB-INF/s3cache" );
		repo.setPath("/WEB-INF/data/test/originals/bucket1");
		repo.setBucket("entermedia_test");
		repo.setAccessKey("AKIAJ3I5YRDDG6XXXX");
		repo.setSecretKey("XD1CeXAhP5imBp3wvRKVUUz3xYnVXXX");
		
		//18QVEMSV7G0J0SYHV602   Access KEY
		//lQjFuUS77WJDmWqwMjBRUlgTy6TjqsqAjRlbuxN3  Secret Key
		
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

		i = repo.get("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server-rack-cabinet-md.png");

		assertNotNull(i);
		assertTrue(i.exists());
		assertTrue(i.getInputStream().available() > 0 );
	

		ContentItem stub = repo.getStub("/WEB-INF/data/test/originals/bucket1/sub1/sub2/server-rack-cabinet-md.png");
		assertNotNull(stub);
		assertTrue(stub.exists());
		assertFalse(stub.isFolder());
		assertTrue(stub.getLastModified() > 0 );
		
		
		String path = i.getAbsolutePath();
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
