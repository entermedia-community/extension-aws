package org.entermediadb.amazon;

import java.util.Date;

import org.openedit.repository.filesystem.FileItem;



public class S3ContentItem extends FileItem
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