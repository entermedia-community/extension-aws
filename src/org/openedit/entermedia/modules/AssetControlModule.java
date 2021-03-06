package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class AssetControlModule extends BaseMediaModule 
{
	
	private static final Log log = LogFactory.getLog(AssetControlModule.class);

	/**
	 * This is a funny action that actually checks the permissions of the assets
	 * directory
	 * 
	 * @param inReq
	 * @return
	 * @throws Exception
	 */
	
	public void loadAssetPermissions(WebPageRequest inReq) throws Exception 
	{
		// look in the assets xconf and check those permissions
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);

		if (sourcepath != null) 
		{
			archive.loadAssetPermissions(sourcepath, inReq);
		}
		else
		{	
			log.error("No sourcepath passed in " + inReq);
		}
	}
	
	public Boolean canViewAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset"); 
		if(asset == null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			String ispublic = archive.getCatalogSettingValue("catalogassetviewispublic");
			if( Boolean.parseBoolean(ispublic) )
			{
				return true;
			}
		}
		MediaArchive archive = getMediaArchive(inReq);
		if( asset == null )
		{
			asset = archive.getAssetBySourcePath(inReq.getPage());
		}
		//MediaArchive inArchive, User inUser, UserProfile inProfile, String inType, Asset inAsset
		Boolean cando = archive.getAssetSecurityArchive().canDo(archive,inReq.getUser(),inReq.getUserProfile(),"view",asset);
		return cando;
	}
	public Boolean canEditAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset"); 
		if(asset == null)
		{
			return false;
		}
		MediaArchive archive = getMediaArchive(inReq);
		Boolean cando = archive.getAssetSecurityArchive().canDo(archive,inReq.getUser(),inReq.getUserProfile(),"edit",asset);
		return cando;
	}
	/*
	public void loadAllAssetPermissions(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);
		if (sourcepath == null) {
			sourcepath = "";
		}
		archive.loadAllAssetPermissions(sourcepath, inReq);
	}
	*/
	/*
	 * protected String findSourcePath(WebPageRequest inReq) throws Exception {
	 * if(!(inReq.getPageValue("asset") instanceof Asset)) { return null; }
	 * Asset asset = (Asset) inReq.getPageValue("asset");
	 * 
	 * if (asset != null) { return asset.getSourcePath(); } MediaArchive archive
	 * = getMediaArchive(inReq); String sourcePath =
	 * �
	 * archive.getSourcePathForPage(inReq);
	 * 
	 * if( sourcePath == null) { String assetid =
	 * inReq.getRequestParameter("assetid");
	 * 
	 * //look for if (assetid != null) { return
	 * archive.getAssetSearcher().idToPath(assetid); }
	 * 
	 * } return sourcePath; }
	 */
	public List<User> listAssetViewPermissions(WebPageRequest inReq) throws Exception {
		Asset asset = getAsset(inReq);
		MediaArchive mediaArchive = getMediaArchive(inReq);

		// this is failing, getAccessList is throwing NUll
		List userNames = mediaArchive.getAssetSecurityArchive().getAccessList(mediaArchive, asset);
		List<User> users = findUsersByName(userNames);
		
		inReq.putPageValue("peoples", users);

		
		List<Group> groups = findGroupByIds(userNames);
		Collections.sort(groups);

		inReq.putPageValue("groups", groups);

		return users;
	}

	public List<User> findUsersByName(List<String> inUserNames)
	{
		List<User> users = new ArrayList<User>();
		UserManager mgr = getUserManager();
		for (String name : inUserNames)
		{
			if(name.contains("user_")){
				name = name.substring(5, name.length());
			}
			
					
			User user = mgr.getUser(name);
			if( user != null)
			{
				users.add(user);
			}
		}
		return users;
	}
	protected List<Group> findGroupByIds(List<String> inIds)
	{
		List<Group> groups = new ArrayList<Group>();
		UserManager mgr = getUserManager();
		for (String id: inIds)
		{
			if( id.startsWith("group_" ))
			{
				id = id.substring(6);
				Group group = mgr.getGroup(id);
				if( group != null)
				{
					groups.add(group);
				}
			}
		}
		return groups;
	}

	public boolean checkFolderMatchesUserName(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		if (archive != null) {
			String sourcePath = archive.getSourcePathForPage(inReq);
			if (sourcePath != null
					&& inReq.getUser() != null
					&& sourcePath.startsWith("users/" + inReq.getUser().getId()
							+ "/")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean checkAssetOwnership(WebPageRequest inReq) {
		Asset asset = getAsset(inReq);
		if (asset != null && inReq.getUser() != null) {
			if(inReq.getUser().getId().equalsIgnoreCase(asset.get("owner")))
			{
				return true;
			}
		}
		return true;
	}

	public void openAssetViewPermissions(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
//		String path = "/" + asset.getCatalogId() + "/assets/"
//				+ asset.getSourcePath() + "/";
//		archive.loadAllAssetPermissions(asset.getSourcePath(), inReq);
//		Boolean viewasset = (Boolean) inReq.getPageValue("canviewasset");
//		if (viewasset != null && viewasset.booleanValue()) {
//			//Page page = getPageManager().getPage(path);
			archive.getAssetSecurityArchive().grantAllAccess(archive, asset);
			archive.getAssetSearcher().updateIndex(asset);
//		} else {
//			throw new OpenDataException("You do not have viewasset permission "
//					+ path);
//		}
	}
	
	public void grantGroupAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String groupid = inReq.getRequestParameter("groupid");
		archive.getAssetSecurityArchive().grantGroupViewAccess(archive, groupid, asset);
	}
	
	public void grantUserAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String userid = inReq.getRequestParameter("userid");
		archive.getAssetSecurityArchive().grantViewAccess(archive, userid, asset);
	}

	public void revokeGroupAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String groupid = inReq.getRequestParameter("groupid");
		archive.getAssetSecurityArchive().revokeGroupViewAccess(archive, groupid, asset);
	}
	
	public void revokeUserAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String userid = inReq.getRequestParameter("userid");
		archive.getAssetSecurityArchive().revokeViewAccess(archive, userid, asset);
	}
	
	public void grantAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		
		Set<String> existingGroupIDs = new HashSet(archive.getAssetSecurityArchive().getAccessList(archive, asset));
		Collection<Group> groups = user.getGroups();
		List<String> addedGroups = new ArrayList<String>();
		
		for (Group group : groups)
		{
			if (group!=null&&group.getId()!=null&&!existingGroupIDs.contains(group.getId()))
			{
				addedGroups.add(group.getId());
			}
		}
		if (addedGroups.size()>0)
		{
			archive.getAssetSecurityArchive().grantGroupViewAccess(archive, addedGroups, asset);
		}
	}
	
	public void revokeAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		Collection<Group> groups = user.getGroups();
		//groups = getUserManager().getGroups();
		for (Group group : groups) {
			archive.getAssetSecurityArchive().revokeGroupViewAccess(archive, group.getId(), asset);
		}
	}
	
	public void grantAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.getAssetSecurityArchive().grantAllAccess(archive, asset);
	}
	
	public void revokeAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.getAssetSecurityArchive().clearAssetPermissions(archive, asset);
	}
	
	public void isAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		Collection<Group> groups = new ArrayList<Group>(user.getGroups());
		List groupids = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		List<Group> allowedgroups = findGroupByIds(groupids);
		
		groups.removeAll(allowedgroups);
		if(groups.size() == 0)
		{
			inReq.putPageValue("isallgroups", true);
		}
		else
		{
			inReq.putPageValue("isallgroups", false);
		}
	}
	
	public void isAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//TODO: Make this simpler:  inAsset.isPropertyTrue("public")
		List<String> users = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		for( String permission : users)
		{
			if("true".equals(permission))
			{
				inReq.putPageValue("isall", true);
				return;
			}
		}
		inReq.putPageValue("isall", false);
	}
}
