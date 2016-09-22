package model.amazon;

import javax.mail.internet.CachedDataHandler;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.openedit.repository.BaseRepository
import org.openedit.repository.ContentItem
import org.openedit.repository.Repository
import org.openedit.repository.RepositoryException
import org.openedit.repository.filesystem.FileItem
import org.openedit.repository.filesystem.FileRepository;

import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectMetadataRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.openedit.OpenEditException
import com.openedit.util.OutputFiller

public class S3Repository extends FileRepository
{

	private static final Log log = LogFactory.getLog(S3Repository.class);
	
	protected AmazonS3 fieldConnection;
	public static final String SECRET_KEY = "secretkey";
	public static final String ACCESS_KEY = "accesskey";
	public static final String BUCKET = "bucket";
	protected OutputFiller fieldOutputFiller;
	
	public String getSecretKey() 
	{
		return getProperty(SECRET_KEY);
	}

	public void setSecretKey(String inSecretKey) {
		setProperty(SECRET_KEY,inSecretKey);
		
	}

	public String getAccessKey() {
		return getProperty(ACCESS_KEY);
	}

	public void setAccessKey(String inAccessKey) {
		setProperty(ACCESS_KEY,inAccessKey);
	}

	public String getBucket() {
		return getProperty(BUCKET);
	}

	public void setBucket(String inBucket) {
		setProperty(BUCKET,inBucket);
	}

	
	public AmazonS3 getConnection() {
		if (fieldConnection == null) 
		{
			//BasicAWSCredentials credentials = new BasicAWSCredentials(getAccessKey(),getSecretKey());
			
			
			BasicAWSCredentials basicCred = new BasicAWSCredentials(getAccessKey(),getSecretKey());
			
			ClientConfiguration clientCfg = new ClientConfiguration();
			clientCfg.setProtocol(Protocol.HTTPS);
		
			String proxy = System.getProperty("http.proxyHost");
			if( proxy != null)
			{
				clientCfg.setProxyHost(proxy);
				String port = System.getProperty("http.proxyPort");
				clientCfg.setProxyPort(Integer.valueOf(port));
			}
		
			fieldConnection = new AmazonS3Client(basicCred, clientCfg);
			
			//fieldConnection = new AmazonS3Client(credentials);
			if(!fieldConnection.doesBucketExist(getBucket())){
				fieldConnection.createBucket(getBucket());
			}
			log.info("Connected to S3 " + getBucket() );
		}
		//fieldConnection.
		return fieldConnection;
		
		
	}

	public void setConnection(AmazonS3 inConnection) {
		fieldConnection = inConnection;
	}

	
	public ContentItem get(String inPath) throws RepositoryException 
	{
//		String path = inPath.substring(getPath().length());
		if (inPath.length() == 0)
		{
			inPath = "/";
		}
		S3ContentItem item = new S3ContentItem();
		item.setPath(inPath);
		String awspath = trimAwsPath(inPath);
		File cached = new File(getExternalPath() + "/" + awspath);
		if( awspath.isEmpty() && !cached.exists() )
		{
			cached.mkdirs();
		}
		item.setFile( cached );
		if( inPath.endsWith(".xconf") )
		{
			item.existed = false;
			return item;
		}
		//Defaults
		item.existed = true; //Is this cool?
		item.folder = true; //Is this cool?

		//System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
		boolean save = true;
		if( inPath.endsWith("/") || awspath.isEmpty() || cached.isDirectory() )
		{
			return item;
		}
//
		S3Object object = null;
		try
		{
			object = getConnection().getObject(new GetObjectRequest(getBucket(), awspath));
		}
		catch(AmazonServiceException ex )
		{
			if( ex.getStatusCode()  == 404)
			{
				//Might be a folder, dumb api
				return item;
			}
			throw ex;
		}
		try
		{
			item.folder = false;
			
			Date last = object.getObjectMetadata().getLastModified();
			long filemod = 0;
			if( last != null)
			{
				filemod = last.getTime();
				if( cached.exists() )
				{
					long oldtime = cached.lastModified();
					
					filemod = filemod/1000;
					oldtime = oldtime/1000;
					if (filemod == oldtime)
					{
						save = false;
					}
				}
			}
			  
			if( save  ) //S3 tracks the time a file was uploaded, not the original time stamp
			{
				InputStream input = object.getObjectContent();
				cached.getParentFile().mkdirs();
				OutputStream output = null;
				try
				{
					output =  new FileOutputStream(cached);
					log.info("Downloading from S3 to cache " + inPath);
					getOutputFiller().fill( input, output );
					if( filemod > 0)
					{
						cached.setLastModified(filemod);
					}
				}
				catch( IOException ex)
				{
					throw new OpenEditException(ex);
				}
				finally
				{
					getOutputFiller().close(input);
					getOutputFiller().close(output);
				}
				
			}
		}
		finally
		{
			object.close();	
		}
  		return item;
          
          
	}

	@Override
	public ContentItem getStub(String inPath) throws RepositoryException 
	{
	    S3ContentItem item = new S3ContentItem();
		item.setStub(true);
	    item.setPath(inPath);

		String awspath = trimAwsPath(inPath);
		File cached = new File(getExternalPath() + "/" + awspath);
		item.setFile(cached);
		if( awspath.isEmpty() && !cached.exists() )
		{
			cached.mkdirs();
		}		
		if( inPath.endsWith(".xconf") )
		{
			S3ContentItem tmpitem = new S3ContentItem();
			tmpitem.setPath(inPath);
			tmpitem.setFile( cached );
			tmpitem.existed = false;
			return tmpitem;
		}
		//Defaults
		item.folder = true;
		item.existed = true;
		
		if(inPath.endsWith("/") || inPath.equals(getPath()) ||  cached.isDirectory() )
		{
			return item;
		}
		
		try
		{
			//not sure if it is a directory or not
			 ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));

			 if( data.getLastModified() == null)
			 {
				 return item; //Must have been a folder
			 }
			 else
			 {
				 item.folder = false;
				 item.setLastModified(data.getLastModified());
				 item.length = data.getContentLength();
			 }
			 
			 return item;
		}
		catch( ConnectionPoolTimeoutException ex)
		{
			
		}
		catch(AmazonServiceException ex )
		{
			if( ex.getStatusCode()  == 404)
			{
				item.folder = false;
				item.existed = false;
				log.info("S3 Missing this path " + inPath);
				return item;
			}
			throw ex;
		}
		//log.info("returned a file path " + inPath);
		//return item;

	}

	private String trimAwsPath(String inPath)
	{
		String awspath = inPath.substring(getPath().length() );
		if( awspath.startsWith("/"))
		{
			awspath = awspath.substring(1);
		}
		return awspath;
	}

	
	public boolean doesExist(String inPath) throws RepositoryException 
	{
		try
		{
			if( inPath.equals(getPath()))
			{
				return true;
			}
			if( inPath.endsWith(".xconf"))
			{
				return false;
			}
			String awspath = trimAwsPath(inPath);

			if( new File(getExternalPath() + "/" + awspath).exists() )
			{
				return true;
			}
			
			 getConnection().getObjectMetadata(	 new GetObjectMetadataRequest(getBucket(), awspath));
			 return true;
		}
		catch(AmazonServiceException ex )
		{
			if( ex.getStatusCode()  == 404)
			{
				return false;
			}
			throw ex;
		}
	}

	public void move(ContentItem inSource,ContentItem inDest) throws RepositoryException 
	{
		if( inSource instanceof S3ContentItem)
		{
			//move using their API
			throw new RepositoryException("Repo moves not supported");
		}
		String awspath = trimAwsPath(inDest.getPath());
		String root = getExternalPath() + "/" + awspath;
		
		File source = new File(inSource.getAbsolutePath());
		File destination = new File(root);
		
		
		if ( !source.renameTo(root))
		{
			getFileUtils().copyFiles( source, destination );
			getFileUtils().deleteAll(source);
		}
		log.info("Moved cache file to s3 " +awspath);
		//PutObjectRequest req = new PutObjectRequest(getBucket(), awspath, destination);
		getConnection().putObject(getBucket(), awspath, destination);
		
		ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));
		 if( data.getLastModified() != null)
		 {
			 destination.setLastModified(data.getLastModified().getTime());
		 }
		 if( inDest instanceof S3ContentItem)
		 {
			 S3ContentItem ci = (S3ContentItem)inDest;
			 ci.existed = null;
			 ci.folder = null;
			 ci.setFile(destination);
		 }
	}
	
	public void put(ContentItem inContent) throws RepositoryException {
		
		//Should I save a copy to the local cache sure?
		String awspath = trimAwsPath(inContent.getPath());
		File localfile = null;
		if( getExternalPath() != null && !(inContent instanceof FileItem))
		{
			String root = getExternalPath() + "/" + awspath;
			
			//We need a file and we need to store it on a temporary basis
			localfile = new File(root);
			InputStream input = inContent.getInputStream();
			localfile.getParentFile().mkdirs();
			OutputStream output = null;
			try
			{
				output = new FileOutputStream(localfile);
				log.info("Saving Local file to cache " + awspath);
				getOutputFiller().fill( input, output );
			}
			catch( IOException ex)
			{
				throw new OpenEditException(ex);
			}
			finally
			{
				getOutputFiller().close(input);
				getOutputFiller().close(output);
			}
		}
		else
		{
			localfile = ((FileItem)inContent).getFile();
		}
		
		///PutObjectRequest req = new PutObjectRequest(getBucket(), awspath, cached);
		log.info("${getBucket()} | ${awspath} | ${localfile}");
		getConnection().putObject(getBucket(), awspath, localfile);
		log.info("Uploaded from cache to S3 " + awspath);
		
		ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));
		if( data.getLastModified() != null)
		{
			localfile.setLastModified(data.getLastModified().getTime());
		}
		
	}

	@Override
	public void copy(ContentItem inSource, ContentItem inDest) throws RepositoryException {
		
		if( inSource instanceof S3ContentItem)
		{
			//move using their API
			throw new RepositoryException("Repo moves not supported");
		}
		String awspath = trimAwsPath(inDest.getPath());
		String root = getExternalPath() + "/" + awspath;
		
		File source = new File(inSource.getAbsolutePath());
		File destination = new File(root);
		
		getFileUtils().copyFiles( source, destination );
		log.info("Copy to cache file " +awspath);
		//PutObjectRequest req = new PutObjectRequest(getBucket(), awspath, destination);
		getConnection().putObject(getBucket(), awspath, destination);
		
		ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));
		 if( data.getLastModified() != null)
		 {
			 destination.setLastModified(data.getLastModified().getTime());
		 }
		 if( inDest instanceof S3ContentItem)
		 {
			 S3ContentItem ci = (S3ContentItem)inDest;
			 ci.existed = null;
			 ci.folder = null;
			 ci.setFile(destination);
		 }
		
	}


	@Override
	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		//throw new RepositoryException("  not supported");
		move(inSource,inDestination);
	}

	
	public void remove(ContentItem inPath) throws RepositoryException {
	getConnection().deleteObject(getBucket(), inPath.getPath());
		
	}

	@Override
	public List getVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getLastVersion(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getChildrenNames(String inParent) throws RepositoryException 
	{
		try
		{
			String parentpath = inParent;
			if(parentpath.length() > 0 && !parentpath.endsWith("/"))
			{
				parentpath = parentpath + "/";
			}
			
			String awspath = trimAwsPath(parentpath);
			AmazonS3 s3 = getConnection();
			ObjectListing current = s3.listObjects(new ListObjectsRequest()
            .withBucketName(getBucket())
            .withPrefix(awspath)
			.withDelimiter("/")
            );

			//These are the subfolders = 
			List paths = new ArrayList();
			
			List<S3ObjectSummary> keyList = current.getObjectSummaries();
			ObjectListing nextbatch = s3.listNextBatchOfObjects(current);
			keyList.addAll(nextbatch.getObjectSummaries());
			for( String path :  current.getCommonPrefixes())
			{
				paths.add( getPath() + "/" + path );
			}

			
			while (nextbatch.isTruncated()) {
			   current=s3.listNextBatchOfObjects(nextbatch);
			   keyList.addAll(current.getObjectSummaries());
			   nextbatch =s3.listNextBatchOfObjects(current);
				for( String path :  current.getCommonPrefixes())
				{
					paths.add(getPath() +  "/" +  path );						
				}
			}
			keyList.addAll(nextbatch.getObjectSummaries());

			for( S3ObjectSummary summary :  keyList)
			{
				String path = summary.getKey();
				if( !awspath.equals(path)) //Why is the parent included?
				{
					paths.add( getPath() + "/" +  path );
				}
				
			}
			return paths;
		}
		catch(AmazonServiceException ex )
		{
			throw ex;
		}
	}

	@Override
	public void deleteOldVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	
	class S3ContentItem extends FileItem
	{
		protected Boolean existed;
		protected Boolean folder;
		protected long length;
		
		@Override
		public long getLength()
		{
			if( length > 0)
			{
				return length;
			}
			return super.getLength();
		}
		
		public boolean exists()
		{
			if( existed != null)
			{
				return existed;
			}
			if( getFile() != null)
			{
				return getFile().exists();
			}
			return true;
		}

		public boolean isFolder()
		{
			if( folder != null)
			{
				return folder;
			}
			if( getFile() != null)
			{
				return getFile().isDirectory();
			}
			if (getPath().endsWith("/"))
			{
				return true;
			}
			return false;
		}

		public void setLastModified(Date inDate)
		{
			fieldLastModified = inDate;
		}
		
		public String toString()
		{
			return getName();
		}
		
	}


	public URL getPresignedURL(String inString, Date expiration) {
		return getConnection().generatePresignedUrl(getBucket(), inString, expiration);
	}
	
	protected OutputFiller getOutputFiller()
	{
		if( fieldOutputFiller == null)
		{
			fieldOutputFiller = new OutputFiller();
		}
		return fieldOutputFiller;
	}
}




/*
 * Important: Be sure to fill in your AWS access credentials in the
 *            AwsCredentials.properties file before you try to run this
 *            sample.
 * http://aws.amazon.com/security-credentials
 */
//AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
//        S3Sample.class.getResourceAsStream("AwsCredentials.properties")));
//
//String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
//String key = "MyObjectKey";
//
//System.out.println("===========================================");
//System.out.println("Getting Started with Amazon S3");
//System.out.println("===========================================\n");
//
//try {
//    /*
//     * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
//     * so once a bucket name has been taken by any user, you can't create
//     * another bucket with that same name.
//     *
//     * You can optionally specify a location for your bucket if you want to
//     * keep your data closer to your applications or users.
//     */
//    System.out.println("Creating bucket " + bucketName + "\n");
//    s3.createBucket(bucketName);
//
//    /*
//     * List the buckets in your account
//     */
//    System.out.println("Listing buckets");
//    for (Bucket bucket : s3.listBuckets()) {
//        System.out.println(" - " + bucket.getName());
//    }
//    System.out.println();
//
//    /*
//     * Upload an object to your bucket - You can easily upload a file to
//     * S3, or upload directly an InputStream if you know the length of
//     * the data in the stream. You can also specify your own metadata
//     * when uploading to S3, which allows you set a variety of options
//     * like content-type and content-encoding, plus additional metadata
//     * specific to your applications.
//     */
//    System.out.println("Uploading a new object to S3 from a file\n");
//    s3.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));
//
//    /*
//     * Download an object - When you download an object, you get all of
//     * the object's metadata and a stream from which to read the contents.
//     * It's important to read the contents of the stream as quickly as
//     * possibly since the data is streamed directly from Amazon S3 and your
//     * network connection will remain open until you read all the data or
//     * close the input stream.
//     *
//     * GetObjectRequest also supports several other options, including
//     * conditional downloading of objects based on modification times,
//     * ETags, and selectively downloading a range of an object.
//     */
//    System.out.println("Downloading an object");
//    S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
//    System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
//    displayTextInputStream(object.getObjectContent());
//
//    /*
//     * List objects in your bucket by prefix - There are many options for
//     * listing the objects in your bucket.  Keep in mind that buckets with
//     * many objects might truncate their results when listing their objects,
//     * so be sure to check if the returned object listing is truncated, and
//     * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
//     * additional results.
//     */
//    System.out.println("Listing objects");
//    ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
//            .withBucketName(bucketName)
//            .withPrefix("My"));
//    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//        System.out.println(" - " + objectSummary.getKey() + "  " +
//                           "(size = " + objectSummary.getSize() + ")");
//    }
//    System.out.println();
//
//    /*
//     * Delete an object - Unless versioning has been turned on for your bucket,
//     * there is no way to undelete an object, so use caution when deleting objects.
//     */
//    System.out.println("Deleting an object\n");
//    s3.deleteObject(bucketName, key);
//
//    /*
//     * Delete a bucket - A bucket must be completely empty before it can be
//     * deleted, so remember to delete any objects from your buckets before
//     * you try to delete them.
//     */
//    System.out.println("Deleting bucket " + bucketName + "\n");
//    s3.deleteBucket(bucketName);
//} catch (AmazonServiceException ase) {
//    System.out.println("Caught an AmazonServiceException, which means your request made it "
//            + "to Amazon S3, but was rejected with an error response for some reason.");
//    System.out.println("Error Message:    " + ase.getMessage());
//    System.out.println("HTTP Status Code: " + ase.getStatusCode());
//    System.out.println("AWS Error Code:   " + ase.getErrorCode());
//    System.out.println("Error Type:       " + ase.getErrorType());
//    System.out.println("Request ID:       " + ase.getRequestId());
//} catch (AmazonClientException ace) {
//    System.out.println("Caught an AmazonClientException, which means the client encountered "
//            + "a serious internal problem while trying to communicate with S3, "
//            + "such as not being able to access the network.");
//    System.out.println("Error Message: " + ace.getMessage());
//}