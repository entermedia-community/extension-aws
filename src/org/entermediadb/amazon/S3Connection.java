package org.entermediadb.amazon;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.ModuleManager;
import org.openedit.repository.ContentItem;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;


public class S3Connection 
{

	private static final Log log = LogFactory.getLog(S3Connection.class);
	
	protected AmazonS3 fieldConnection;
	protected String fieldCatalogId;
	
	
	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	public MediaArchive getMediaArchive() {
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager fieldModuleManager) {
		this.fieldModuleManager = fieldModuleManager;
	}



	protected ModuleManager fieldModuleManager;

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

	private String getSecretKey() {
	return getMediaArchive().getCatalogSettingValue("awssecretkey");
	}

	private String getAccessKey() {
		return getMediaArchive().getCatalogSettingValue("awsaccesskey");
	}

	public void setConnection(AmazonS3 inConnection) {
		fieldConnection = inConnection;
	}

	
	
//	
//	public boolean doesExist(String inPath) throws RepositoryException 
//	{
//		try
//		{
//			if( inPath.equals(getPath()))
//			{
//				return true;
//			}
//			if( inPath.endsWith(".xconf"))
//			{
//				return false;
//			}
//			String awspath = trimAwsPath(inPath);
//
//			if( new File(getExternalPath() + "/" + awspath).exists() )
//			{
//				return true;
//			}
//			
//			 getConnection().getObjectMetadata(	 new GetObjectMetadataRequest(getBucket(), awspath));
//			 return true;
//		}
//		catch(AmazonServiceException ex )
//		{
//			if( ex.getStatusCode()  == 404)
//			{
//				return false;
//			}
//			throw ex;
//		}
//	}


	
	public void put(String awspath, ContentItem inContent)  {
		
		//Should I save a copy to the local cache sure?
		
		File localfile = new File(inContent.getAbsolutePath());
		log.info(getBucket()+ "PAth: " +  awspath );
		try {
			getConnection().putObject(getBucket(), awspath, localfile);
		} catch (Exception e) {
			
			setConnection(null);
			getConnection().putObject(getBucket(), awspath, localfile);			
		}
		log.info("Uploaded from cache to S3 " + awspath);
		
		ObjectMetadata data = getConnection().getObjectMetadata( new GetObjectMetadataRequest(getBucket(), awspath));
		if( data.getLastModified() != null)
		{
			localfile.setLastModified(data.getLastModified().getTime());
		}
		
	}

	private String getBucket() {
		return getMediaArchive().getCatalogSettingValue("awsbucket");
	}

	
	
	

	

}