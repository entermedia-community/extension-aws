package model.amazon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.FileItem;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.openedit.OpenEditException;
import com.openedit.util.OutputFiller;

public class S3Repository extends BaseRepository
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
			BasicAWSCredentials credentials = new BasicAWSCredentials(getAccessKey(),getSecretKey());
			fieldConnection = new AmazonS3Client(credentials);
			if(!fieldConnection.doesBucketExist(getBucket())){
				fieldConnection.createBucket(getBucket());
			}
		}

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
		FileItem item = new FileItem();
		item.setPath(inPath);
		String awspath = trimAwsPath(inPath);
		File cached = new File(getExternalPath() + "/" + awspath);
		item.setFile( cached );
		//System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
		
		boolean save = true;
		if( item.isFolder() || inPath.endsWith("/") || inPath.endsWith(".xconf") )
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
				return item;
			}
			throw ex;
		}
		long filemod = object.getObjectMetadata().getLastModified().getTime();
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
		  
		if( save  )
		{
			InputStream input = object.getObjectContent();
			cached.getParentFile().mkdirs();
			OutputStream output = null;
			try
			{
				output =  new FileOutputStream(cached);
				log.info("Caching " + inPath);
				getOutputFiller().fill( input, output );
				cached.setLastModified(filemod);
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
		item.folder = cached.isDirectory();
		
		if( inPath.endsWith(".xconf") )
		{
			FileItem tmpitem = new FileItem();
			tmpitem.setPath(inPath);
			tmpitem.setFile( cached );
			return tmpitem;
		}
		
		if(inPath.endsWith("/") || item.folder || inPath.equals(getPath()) )
		{
			item.folder = true;
			item.existed = true;
			return item;
		}

		
		
	    item.existed = cached.exists();
		item.folder = cached.isDirectory();
		
		if(inPath.endsWith("/") || item.folder )
		{
			item.folder = true;
		    return item;
		}
		try
		{
			 ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));
			 
			 item.setLastModified(data.getLastModified());
			 
			 return item;
		}
		catch(AmazonServiceException ex )
		{
			if( ex.getStatusCode()  == 404)
			{
				item.folder = true;
				log.info("Must be a folder " + inPath);
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
			String awspath = trimAwsPath(inPath);

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

	
	public void put(ContentItem inContent) throws RepositoryException {
		
		//Should I save a copy to the local cache sure?
		String awspath = trimAwsPath(inContent.getPath());
		String root = getExternalPath() + "/" + awspath;
		
		File cached = new File(root);

		InputStream input = inContent.getInputStream();
		cached.getParentFile().mkdirs();
		OutputStream output = null;
		try
		{
			output = new FileOutputStream(cached);
			log.info("Saving " + awspath);
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
		
		PutObjectRequest req = new PutObjectRequest(getBucket(), awspath, cached);
		getConnection().putObject(req);
		
	}

	@Override
	public void copy(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
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
			if( inParent.startsWith("/"))
			{
				inParent = inParent.substring(1);
			}
			if(inParent.length() > 0 && !inParent.endsWith("/"))
			{
				inParent = inParent + "/";
			}
			
			AmazonS3 s3 = getConnection();
			ObjectListing current = s3.listObjects(new ListObjectsRequest()
            .withBucketName(getBucket())
            .withPrefix(inParent)
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
				if( !inParent.equals(path))
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

	
	class S3ContentItem extends InputStreamItem
	{
		protected Boolean existed;
		protected Boolean folder;
		
		public InputStream getInputStream() throws RepositoryException
		{
			// S3Object object = getConnection().getObject(new GetObjectRequest(getBucket(), getPath()));
			// return object.getObjectContent();
			//TODO: Call the normal get( method
			return null;
		}

		public boolean exists()
		{
			return existed;
		}

		public boolean isFolder()
		{
			if (folder || getPath().endsWith("/"))
			{
				return true;
			}
			return false;
		}

		public void setLastModified(Date inDate)
		{
			fieldLastModified = inDate;
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