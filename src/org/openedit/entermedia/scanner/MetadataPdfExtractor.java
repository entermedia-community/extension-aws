package org.openedit.entermedia.scanner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class MetadataPdfExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(MetadataPdfExtractor.class);

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		
		String type = PathUtilities.extractPageType(inFile.getPath());
		if (type == null || "data".equals(type.toLowerCase()))
		{
			type = inAsset.get("fileformat");
		}

		if (type != null)
		{
			type = type.toLowerCase();
			if( inAsset.get("fileformat") == null)
			{
				inAsset.setProperty("fileformat", type);
			}
			if (type.equals("pdf"))
			{
				PdfParser parser = new PdfParser();

//				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream in = null;
				try
				{
					in = inFile.getInputStream();
//					try
//					{
//						new OutputFiller().fill(in, out);
//					}
//					finally
//					{
//						FileUtils.safeClose(in);
//					}
//					byte[] bytes = out.toByteArray();
					Parse results = parser.parse(in); //Do we deal with encoding?
					//We need to limit this size
					String fulltext = results.getText();
					inAsset.setProperty("fulltext", fulltext);
					if( inAsset.getInt("width") == 0)
					{
						String val = results.get("width");
						inAsset.setProperty("width", val);
					}
					if( inAsset.getInt("height") == 0)
					{
						String val = results.get("height");
						inAsset.setProperty("height", val);
					}
					inAsset.setProperty("pages", String.valueOf(results.getPages()));
					if (inAsset.getProperty("assettitle") == null)
					{
						String title  = results.getTitle();
						if( title != null && title.length() < 300)
						{
							inAsset.setProperty("assettitle", title);
						}
					}

				}
				catch( Exception ex)
				{
					log.error("cant process" , ex);
					return false;
				}
				finally
				{
					FileUtils.safeClose(in);
				}

				return true;
			}
		}
		return false;
	}

}
