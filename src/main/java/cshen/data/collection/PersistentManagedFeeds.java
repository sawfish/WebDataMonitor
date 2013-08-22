package cshen.data.collection;

import java.util.Map;

import cshen.data.collection.ManagedFeeds.FeedMeta;

public interface PersistentManagedFeeds {

	public abstract Map<String, FeedMeta> loadFeeds() throws Exception;

	public abstract boolean saveFeeds(ManagedFeeds mf);

}