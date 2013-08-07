package cshen.data.collection;

import java.util.*;

import de.nava.informa.core.ItemIF;

public class ManagedFeeds {
	private Map<String, FeedMeta> feeds;
	private Map<String, List<ItemIF>> newItemsOfFeeds = null;
	public Map<String, List<ItemIF>> getNewItemsOfFeeds() {
		return newItemsOfFeeds;
	}

	private PersistentManagedFeeds db;
	
	
	public ManagedFeeds() {
		feeds = new HashMap<String, FeedMeta>();
		newItemsOfFeeds = new HashMap<String, List<ItemIF>>();
	}
	
	public void load() throws Exception {
		feeds = db.loadFeeds();
	}
	
	public void add(String feed, FeedMeta meta) {
		feeds.put(feed, meta);
	}
	
	public void updateMeta(String feed, FeedMeta meta) {
		feeds.put(feed, meta);
	}
	
	public FeedMeta getMeta(String feed) {
		return feeds.get(feed);
	}
	
	public List<String> getFeedList() {
		return new ArrayList<String>(feeds.keySet());
	}
	
	public void setPersistence(PersistentManagedFeeds db) {
		this.db = db;
	}
	
	public boolean save() {
		boolean result = db.saveFeeds(this);
		newItemsOfFeeds.clear();
		return result;
	}
	
	public void addNewItem(String rss, ItemIF item) {
		List<ItemIF> items = newItemsOfFeeds.get(rss);
		if (items == null) {
			items = new ArrayList<ItemIF>();
			newItemsOfFeeds.put(rss, items);
		}
		items.add(item);
	}
	
	static public class FeedMeta {
		private Date lastPubDate;
		private Date lastCheckDate;
		
		
		public FeedMeta(Date lastPubDate, Date lastCheckDate) {
			super();
			this.lastPubDate = lastPubDate;
			this.lastCheckDate = lastCheckDate;
		}


		public Date getLastCheckDate() {
			return lastCheckDate;
		}


		public void setLastCheckDate(Date lastCheckDate) {
			this.lastCheckDate = lastCheckDate;
		}


		public void setLastPubDate(Date lastPubDate) {
			this.lastPubDate = lastPubDate;
		}


		public Date getLastPubDate() {
			return lastPubDate;
		}
		
	}

	
}
