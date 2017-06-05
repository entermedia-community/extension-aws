package org.entermediadb.amazon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.push.PushManager;
import org.openedit.Data;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

import model.push.BasePushManager;

public class S3PushManager extends BasePushManager implements PushManager {

	public void toggle(String inCatalogId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pollRemotePublish(MediaArchive inArchive) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDeletedAssets(MediaArchive inArchive, User inUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushAssets(MediaArchive inArchive, List<Asset> inAssetsSaved) {
		S3Connection connection = (S3Connection) inArchive.getBean("S3Connection");
		List tosave = new ArrayList();

		for (Iterator iterator = inAssetsSaved.iterator(); iterator.hasNext();) {
			Asset asset = (Asset) iterator.next();
			tosave.add(asset);
	
			try {
				String mask = inArchive.getCatalogSettingValue("s3pushmask");
				String generatedmask = inArchive.getCatalogSettingValue("s3generatedmask");

				HashMap map = new HashMap();
				for (Iterator iterator2 = asset.getProperties().keySet().iterator(); iterator2.hasNext();) {
					String key = (String) iterator2.next();
					Object val = asset.getValue(key);
					map.put(key, val);
				}
				map.put("asset", asset);

				Collection presets = inArchive.getPresetManager().getPushPresets(inArchive,
						inArchive.getMediaRenderType(asset));
				for (Iterator iterator2 = presets.iterator(); iterator2.hasNext();) {
					Data preset = (Data) iterator2.next();
					map.put("preset", preset);
					// ${asset.name}

					if ("0".equals(preset.getId())) {
						String filepath = inArchive.getAssetImporter().getAssetUtilities()
								.createSourcePathFromMask(inArchive, null, asset.getMediaName(), mask, map);
						ContentItem item = inArchive.getOriginalContent(asset);
						connection.put(filepath, item);

					}

					else {
						String filepath = inArchive.getAssetImporter().getAssetUtilities()
								.createSourcePathFromMask(inArchive, null, asset.getMediaName(), generatedmask, map);
						String path = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + asset.getSourcePath()
								+ "/" + preset.get("outputfile");
						ContentItem item = inArchive.getContent(path);
						connection.put(filepath, item);

					}

				}
				asset.setValue("pusheddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));

			} catch (Exception e) {
				asset.setValue("pusherrordetails", e.toString());
			}


		}
		inArchive.getAssetSearcher().saveAllData(tosave, null);


	}

}
