package org.openedit.entermedia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;

import com.openedit.hittracker.HitTracker;

public class BaseCompositeData extends BaseData implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected Searcher fieldSearcher;
	protected HitTracker fieldInitialSearchResults; 
	protected HitTracker fieldSelectedResults; 
	protected List<String> fieldRemovedCategories;
	protected List<String> fieldRemovedKeywords;
	protected Map<String,String> fieldPropertiesSet;
	protected List<Integer> fieldSelections;
	protected PropertyDetails fieldPropertyDetails;
	protected String fieldId;
	
	public BaseCompositeData(Searcher inSearcher, HitTracker inHits)
	{
		setSearcher(inSearcher);
		setInitialSearchResults(inHits);
		reloadData();
	}
	public HitTracker getInitialSearchResults() 
	{
		return fieldInitialSearchResults;
	}

	public void setInitialSearchResults(HitTracker inInitialSearchResults) 
	{
		fieldInitialSearchResults = inInitialSearchResults;
	}

	
	public HitTracker getSelectedResults() 
	{
		if( fieldSelectedResults == null)
		{
			reloadData();
		}
		return fieldSelectedResults;
	}

	public void setSelectedResults(HitTracker inCurrentSearchResults) 
	{
		fieldSelectedResults = inCurrentSearchResults;
	}


	
//	protected Map<String,String> fieldPropertiesPreviouslySaved;
//	
//	public Map<String,String> getPropertiesPreviouslySaved() 
//	{
//		if (fieldPropertiesPreviouslySaved == null) 
//		{
//			fieldPropertiesPreviouslySaved = new HashMap<String,String>();
//		}
//		return fieldPropertiesPreviouslySaved;
//	}

	
	public PropertyDetails getPropertyDetails() 
	{
		return getSearcher().getPropertyDetails();
	}

	public Map getPropertiesSet()
	{
		if (fieldPropertiesSet == null)
		{
			fieldPropertiesSet = new HashMap();
		}
		return fieldPropertiesSet;
	}

	public void setPropertiesSet(Map inPropertiesSet)
	{
		fieldPropertiesSet = inPropertiesSet;
	}

	public List getRemovedCategories()
	{
		if (fieldRemovedCategories == null)
		{
			fieldRemovedCategories = new ArrayList();
		}

		return fieldRemovedCategories;
	}

	public void setRemovedCategories(List inRemovedCategories)
	{
		fieldRemovedCategories = inRemovedCategories;
	}

	public List getRemovedKeywords()
	{
		if (fieldRemovedKeywords == null)
		{
			fieldRemovedKeywords = new ArrayList();
		}

		return fieldRemovedKeywords;
	}

	public void setRemovedKeywords(List inRemovedKeywords)
	{
		fieldRemovedKeywords = inRemovedKeywords;
	}

	/**
	 * This gets called quite often. 1st when we load the landing page
	 * 2nd when we click on a link to edit something
	 * 3rd when we go to save, we reload it again
	 * 4th when we click to edit something again
	 * Option is to store it in a session field but then it might get out of date 
	 */
	protected void reloadData() 
	{
		HitTracker existing = getInitialSearchResults();
		HitTracker selecteddata = getSearcher().search(existing.getSearchQuery());
		if( existing.isAllSelected() )
		{
			//rerun the search
			selecteddata.selectAll();
		}
		else
		{
			selecteddata.setSelections(existing.getSelections());
			selecteddata.setShowOnlySelected(true);
			selecteddata.setHitsPerPage(10000);
		}
		setSelectedResults(selecteddata);			
		getProperties().clear();
	}

	public int size()
	{
		return getSelectedResults().size();
	}
	

	protected void checkSave(List<Data> inTosave)
	{
		if( inTosave.size() > 99 )
		{
			getSearcher().saveAllData(inTosave,null);
			inTosave.clear();
		}
	}

	public String getProperty(String inKey) 
	{
		return get(inKey);
	}
	public String get(String inId)
	{	
		if (size() > 0)
		{
			String val = (String)getPropertiesSet().get(inId); //set by the user since last save
//			if( val == null && fieldPropertiesPreviouslySaved != null)
//			{
//				val = (String)getPropertiesPreviouslySaved().get(inId);
//			}
			if( val == null)
			{
				val = super.get(inId); //set by looking over the cached results
			}
			if( val != null ) //already set to a value
			{
				if( val.length() == 0 )
				{
					return null; //set to empty
				}
				return val;
			}
//			if( val == null ) 
//			{
//				return null;
//			}
			//return something only if all the values match the first record
			val = getValueFromResults(inId);
			//getPropertiesPreviouslySaved().put(inId, val);
			super.setProperty(inId, val);
			return val;
		}
		
		return null;
	}
	protected String getValueFromResults(String inKey) 
	{
		String val;
		val = ((Data)getSelectedResults().first()).get(inKey);
		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String dataval = data.get(inKey);
			if( val == null )
			{
				if( val != dataval )
				{
					val = "";
					break;
				}
			}
			else if (val.length() > 0 && !val.equals(dataval))
			{
				//Maybe just out of order?
				boolean multi = isMulti(inKey);
				
				if( dataval != null && multi )
				{
					String[] vals = VALUEDELMITER.split(val);
					val = "";
					for (int i = 0; i < vals.length; i++) 
					{
						if( dataval.contains(vals[i]) ) //vals are in an array
						{
							if( val.length() == 0 )
							{
								val = vals[i];
							}
							else
							{
								val = val + " | " + vals[i];
							}
						}
					}
				}
				else
				{
					val = "";
					break;
				}
			}
		}
		if(	val == null )
		{
			val = "";
		}
		return val;
	}
	public void setProperty(String inKey, String inValue)
	{
		if( inValue == null )
		{
			inValue = "";
		}
		//getProperties().put(inKey, inValue);
		getPropertiesSet().put(inKey,inValue);
	}

	public String getId()
	{
		return fieldId;
	}

	public String getName()
	{
		return "Multiple Data";
	}
	
	public void setName(String inName)
	{
		//Nothing to do here
	}

	public void setId(String inNewid)
	{
		fieldId = inNewid;
	}


	public String getSourcePath()
	{
		if( size() > 0)
		{
			Data first = (Data)getSelectedResults().first();
			return first.getSourcePath() + "multi" + size();
		}
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		
	}
	
	public Iterator iterator()
	{
		return new DataIterator(getSelectedResults().iterator());
	}
	
	public Searcher getSearcher()
	{
		return fieldSearcher;
	}

	public void setSearcher(Searcher inSearcher)
	{
		fieldSearcher = inSearcher;
	}

	/**
	 * Do not call this more than once!
	 * Because we use the hit results to check on previous saved 
	 */
	public void saveChanges() 
	{
		//compare keywords, categories and data. 
		List tosave = new ArrayList(100);
		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Data inloopasset = null;
			
			for (Iterator iterator2 = getPropertiesSet().keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				if( "id".equals(key))
				{
					continue;
				}
					
				String value = (String)getPropertiesSet().get(key);
				String datavalue = data.get(key);
				
				if( datavalue == value )
				{
					continue;
				}
				if( datavalue != null && datavalue.equals(value) )
				{
					continue;
				}
				Data loaded = loadData( inloopasset, data, tosave);
				if( loaded != null )
				{
					boolean multi = isMulti(key);

					if( multi )
					{
						//Need to add any that are set by user in value 
						Set added = collect(value);
						Set existing = collect(datavalue);
						Set previousCommonOnes = collect(getValueFromResults(key)); 
						saveMultiValues((MultiValued)loaded, key, added, existing,previousCommonOnes);
					}
					else
					{
						loaded.setProperty(key, value);
					}
					inloopasset = loaded;
				}
			}
			if( tosave.size() > 1000)
			{
				getSearcher().saveAllData(tosave, null);
				tosave.clear();
			}
		}
		getSearcher().saveAllData( tosave, null);
		//getPropertiesPreviouslySaved().putAll(getPropertiesSet());
		setSelectedResults(null);
	}

	/**
	 * @param asset
	 * @param key
	 * @param added Ones that needs to be added
	 * @param existing Ones that are already on the asset
	 * @param old Ones that need to be removed?
	 */
	protected void saveMultiValues(MultiValued asset, String key, Set added, Set existing, Set previousCommonOnes) 
	{
		existing.addAll(added);
		
		//Need to remove any that are missing from combined
		previousCommonOnes.removeAll(added);
		existing.removeAll(previousCommonOnes);
		asset.setValues(key, existing);
		//System.out.println("Saving old value:" + datavalue + " saved: " + existing);
	}
	protected boolean isMulti(String key) 
	{
		if( key.equals( "libraries") )
		{
			return true;
		}
		PropertyDetail detail = getPropertyDetails().getDetail(key);
		
		boolean multi = detail != null && detail.isMultiValue();
		return multi;
	}

	protected Set collect(String existingvalue) 
	{
		if( existingvalue == null || existingvalue.length() == 0)
		{
			return new HashSet();
		}
		String[] vals = VALUEDELMITER.split(existingvalue);
		Set set = new HashSet(vals.length);
		for (int i = 0; i < vals.length; i++) {
			set.add(vals[i]);
		}
		return set;
	}
	//
	class DataIterator implements Iterator
	{
		Iterator fieldDataIterator;
		
		public DataIterator(Iterator inHitsIterator)
		{
			fieldDataIterator = inHitsIterator;
		}
		
		public boolean hasNext()
		{
			return fieldDataIterator.hasNext();
		}

		@Override
		public Object next()
		{
			Data next = (Data)fieldDataIterator.next();
			return getSearcher().searchById(next.getId());
		}

		public void remove()
		{
		}
		
		
	}
	
	protected Data loadData(Data inFieldCurrentAsset, Data inData, List toSave)
	{
		if( inFieldCurrentAsset == null )
		{
			inFieldCurrentAsset =  (Data)getSearcher().searchById(inData.getId());
		}
		else
		{
			return inFieldCurrentAsset;
		}
		if( inFieldCurrentAsset != null )
		{
			toSave.add(inFieldCurrentAsset);
			checkSave(toSave);
		}
		return inFieldCurrentAsset;
	}
	
	public String toString()
	{
		return getId();
	}
	
	public void refresh()
	{
		getPropertiesSet().clear();
		
	}
	@Override
	public Collection<String> getValues(String inPreference) {
		String currentlist =getValueFromResults(inPreference); 
		return collect(currentlist);
		
	}
	
	
}
