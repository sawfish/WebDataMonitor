package cshen.data.collection;

import java.util.*;
import java.net.*;

import org.apache.log4j.Logger;

import cshen.data.collection.ManagedFeeds.FeedMeta;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;


public class FeedReader {
	static private Logger log = Logger.getLogger(FeedReader.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		PersistentManagedFeeds db = new PersistentManagedFeeds();
		db.setFileOfFeeds("tmp.txt");
		db.setFileOfItems("tmp.items.txt");
		ManagedFeeds mf = new ManagedFeeds();
		mf.setPersistence(db);
		FeedReader fr = new FeedReader();
		fr.setManagedFeeds(mf);
		fr.start();
	}
	
	private ManagedFeeds mf;
	private Thread thread;
	private boolean toStop;

	public void setManagedFeeds(ManagedFeeds mf) {
		this.mf = mf;
	}
	
	public FeedReader() {
	}
	
	public void start() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!toStop) {
					try {
						mf.load();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					System.err.println("checking updates");
					updateChannel();
					mf.save();
					int maxiter = 5;
					for(int iter = 0; !toStop && iter < maxiter; iter++) {
						try {
							System.err.println("to check updated in " + (maxiter - iter) + " minutes");
							Thread.sleep(60 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		thread.setDaemon(false);
		thread.start();
	}
	
	public void updateChannel() {
		for (String rss : mf.getFeedList()) {
			try {
				FeedMeta newMeta = updateChannel(rss, mf.getMeta(rss));
				mf.updateMeta(rss, newMeta);
			} catch (Exception e) {
				log.error("failed to update " + rss);
			}
		}
	}
	
	private FeedMeta updateChannel(String rss, FeedMeta feedMeta) throws Exception {
		ChannelIF channel = FeedParser.parse(new ChannelBuilder(), new URL(rss));
		List<ItemIF> items = new ArrayList<ItemIF>(channel.getItems());
		Collections.sort(items, new Comparator<ItemIF>() {
			@Override
			public int compare(ItemIF arg0, ItemIF arg1) {
				return (int)(arg1.getDate().getTime() - arg0.getDate().getTime()); 
			}
		});
		
		Date lastPubDate = channel.getPubDate();
		if (lastPubDate == null) {
			lastPubDate = channel.getItems().iterator().next().getDate();
		}
		
		if (feedMeta != null) {
			Date savedLastPubDate = feedMeta.getLastPubDate();
			if (savedLastPubDate != null && !lastPubDate.after(savedLastPubDate)) {
				return new FeedMeta(savedLastPubDate, new Date());
			}
		}
		
		for (ItemIF item : items) {
			if (feedMeta == null || item.getDate().after(feedMeta.getLastPubDate())) {
				mf.addNewItem(rss, item);
			} else {
				break;
			}
		}
		
		return new FeedMeta(lastPubDate, new Date());
	}
	
	public void stop() {
		toStop = true;
	}

}
