package org.openedit.entermedia.modules;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.profile.UserProfile;

import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;

public class BaseMediaModule extends BaseModule
{
	public EnterMedia getEnterMedia(String inApplicationId)
	{
		EnterMedia matt = (EnterMedia) getModuleManager().getBean(inApplicationId, "enterMedia");
		matt.setApplicationId(inApplicationId);
		return matt;
	}

	public EnterMedia getEnterMedia(WebPageRequest inReq)
	{
		String appid = inReq.findValue("applicationid");
		EnterMedia matt = getEnterMedia(appid);
		inReq.putPageValue("enterMedia", matt); //do not use
		inReq.putPageValue("entermedia", matt);
		inReq.putPageValue("applicationid", appid);
		inReq.putPageValue("apphome", "/" + appid);
		
		String prefix = inReq.getContentProperty("themeprefix");
		UserProfile profile = inReq.getUserProfile();
		if( profile != null)
		{
			prefix = profile.replaceUserVariable(prefix);
		}
		inReq.putPageValue("themeprefix", prefix);

		return matt;
	}

	public String loadApplicationId(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");
		inReq.putPageValue("applicationid", applicationid);
		inReq.putPageValue("apphome", "/" + applicationid);

		String prefix = inReq.getContentProperty("themeprefix");
		UserProfile profile = inReq.getUserProfile();
		if( profile != null)
		{
			prefix = profile.replaceUserVariable(prefix);
		}
		inReq.putPageValue("themeprefix", prefix);
		return applicationid;
	}

	public MediaArchive getMediaArchive(String inCatalogid)
	{
		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
	}

	public MediaArchive getMediaArchive(WebPageRequest inReq)
	{
		MediaArchive archive = (MediaArchive)inReq.getPageValue("mediaarchive");
		if( archive != null)
		{
			return archive;
		}
		String catalogid = inReq.findValue("catalogid");
		if (catalogid == null || "$catalogid".equals(catalogid))
		{
			return null;
		}
		archive = getMediaArchive(catalogid);
		inReq.putPageValue("mediaarchive", archive);
		inReq.putPageValue("cataloghome", archive.getCatalogHome());
		inReq.putPageValue("catalogid", catalogid); // legacy
		return archive;
	}
	public SearcherManager getSearcherManager()
	{
		return (SearcherManager)getModuleManager().getBean("searcherManager");
	}
	
	
	public Asset getAsset(WebPageRequest inReq)
	{
		Object found = inReq.getPageValue("asset");
		if( found instanceof Asset)
		{
			return (Asset)found;
		}
		
		String sourcePath = inReq.getRequestParameter("sourcepath");
		
		MediaArchive archive = getMediaArchive(inReq);
		
		Asset asset = null;
		if (sourcePath != null)
		{
			//asset = archive.getAssetArchive().getAssetBySourcePath(sourcePath, true);
			asset = archive.getAssetSearcher().getAssetBySourcePath(sourcePath, true);
		}
		String assetid = null;
		if( asset == null)
		{
			assetid = inReq.getRequestParameter("assetid");
			
			if( assetid != null && assetid.startsWith("multiedit:") )
			{
//				Data data = (Data)inReq.getSessionValue(assetid);
				Asset data = archive.getAsset(assetid, inReq);
				inReq.putPageValue("asset", data);
				inReq.putPageValue("data", data);
				return (Asset) data;
			}

		}
		if (asset == null && archive != null)
		{
			asset = archive.getAssetBySourcePath(inReq.getContentPage());
			if (asset == null)
			{
				if (assetid != null)
				{
					asset = archive.getAsset(assetid);
				}
			}
		}
		if( inReq.getParent() != null)
		{
			inReq.getParent().putPageValue("asset", asset);
		}
		else
		{
			inReq.putPageValue("asset", asset);
		}
		return asset;
	}
	
	public Searcher loadSearcher(WebPageRequest inReq) throws Exception
	{
		// Load by url
		// catalogid/type.html
		inReq.putPageValue("searcherManager", getSearcherManager());
		String fieldname = resolveSearchType(inReq);
		if (fieldname == null)
		{
			return null;
		}
		String catalogId = resolveCatalogId(inReq);

		org.openedit.data.Searcher searcher = getSearcherManager().getSearcher(catalogId, fieldname);
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("detailsarchive", searcher.getPropertyDetailsArchive());
		return searcher;
	}

	protected String resolveCatalogId(WebPageRequest inReq)
	{
		String catalogId = inReq.getRequestParameter("catalogid");
		if (catalogId == null || catalogId.startsWith("$"))
		{
			catalogId = inReq.findValue("catalogid");
		}
		if( catalogId == null)
		{
			catalogId = inReq.findValue("applicationid");
		}
		inReq.putPageValue("catalogid", catalogId);
		return catalogId;
	}

	protected String resolveSearchType(WebPageRequest inReq)
	{
		String searchtype = inReq.findValue("searchtype");

		inReq.putPageValue("searchtype", searchtype);
		return searchtype;
	}

	
}
