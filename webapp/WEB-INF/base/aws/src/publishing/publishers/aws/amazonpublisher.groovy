package publishing.publishers.aws;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.publishing.Publisher
import org.openedit.repository.filesystem.FileItem

import publishing.publishers.basepublisher

import com.openedit.page.Page

public class amazonpublisher extends basepublisher implements Publisher 
{
	private static final Log log = LogFactory.getLog(amazonpublisher.class);

	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data inPreset)
	{
		def repo = mediaArchive.getModuleManager().getBean("S3Repository");
		log.info("Publish asset to Amazon ${asset} for on server: ${destination}" );

		repo.setBucket(destination.bucket);
		repo.setAccessKey(destination.accesskey);
		repo.setSecretKey(destination.secretkey);

		PublishResult result = new PublishResult();
		//open the file and send it
		Page inputpage = findInputPage(mediaArchive,asset,inPreset);
		String exportname = inPublishRequest.get("exportname");
		if( !exportname.startsWith("/"))
		{
			exportname = "/" + exportname;
		}
		FileItem item = new FileItem();
		item.setPath( exportname);
		item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());

		repo.put(item); //copy the file
		log.info("published  ${exportname} to Amazon s3");
		result.setComplete(true);
		return result;
	}
}

