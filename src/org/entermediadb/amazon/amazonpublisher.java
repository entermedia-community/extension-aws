package org.entermediadb.amazon;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.openedit.Data;

public class amazonpublisher extends BasePublisher 
{

	@Override
	public PublishResult publish(MediaArchive inMediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		// TODO Auto-generated method stub
		return null;
	}
//	private static final Log log = LogFactory.getLog(amazonpublisher.class);
//
//	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data inPreset)
//	{
//		def repo = mediaArchive.getModuleManager().getBean("S3Repository");
//		log.info("Publish asset to Amazon ${asset} for on server: ${destination}" );
//
//		repo.setBucket(destination.bucket);
//		repo.setAccessKey(destination.accesskey);
//		repo.setSecretKey(destination.secretkey);
//
//		//repo.setExternalPath(getRoot().getAbsolutePath() + "/WEB-INF/s3cache" );
//		//repo.setPath("/WEB-INF/data/test/originals/bucket1");
//		
//		
//		PublishResult result = new PublishResult();
//		//open the file and send it
//		Page inputpage = findInputPage(mediaArchive,asset,inPreset);
//		String exportname = inPublishRequest.get("exportname");
//		if( !exportname.startsWith("/"))
//		{
//			exportname = "/" + exportname;
//		}
//		FileItem item = new FileItem();
//		item.setPath( exportname);
//		item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());
//
//		repo.put(item); //copy the file
//		log.info("published  ${exportname} to Amazon s3");
//		result.setComplete(true);
//		return result;
//	}
}

