package cshen.data.collection;

import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;
import java.util.Map.*;

import org.apache.log4j.Logger;

import cshen.data.collection.ManagedFeeds.FeedMeta;
import de.nava.informa.core.ItemIF;

public class PersistentManagedFeeds {
	Logger log = Logger.getLogger(PersistentManagedFeeds.class);
	public PersistentManagedFeeds() {
		
	}
	
	private String fileOfFeeds;
	private String fileOfItems;
	static private final String DATE_FORMAT = "yy-MM-dd HH:mm:ss Z";
	
	public void setFileOfFeeds(String filename) {
		this.fileOfFeeds = filename;
	}
	
	public void setFileOfItems(String filename) {
		this.fileOfItems = filename;
	}
	
	public Map<String, FeedMeta> loadFeeds() throws Exception {
		Map<String, FeedMeta> feeds = new HashMap<String, FeedMeta>();
		BufferedReader br = new BufferedReader(new FileReader(fileOfFeeds));
		String line;
		while((line = br.readLine()) != null) {
			String[] flds = line.split("\t");
			ManagedFeeds.FeedMeta meta = null;
			if (flds.length == 3) {
				try {
					meta = new FeedMeta(new SimpleDateFormat(DATE_FORMAT).parse(flds[1]), 
							new SimpleDateFormat(DATE_FORMAT).parse(flds[2]));
				} catch (Exception e) {
					log.error("failed to parse the line " + line);
				}
			}
			feeds.put(flds[0], meta);
		}
		br.close();
		return feeds;
	}
	
	
	public boolean saveFeeds(ManagedFeeds mf) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileOfFeeds));
			for(String feed : mf.getFeedList()) {
				bw.write(feed);
				FeedMeta meta = mf.getMeta(feed);
				if (meta != null) {
					bw.write("\t" + new SimpleDateFormat(DATE_FORMAT).format(meta.getLastPubDate()) 
							+ "\t" + new SimpleDateFormat(DATE_FORMAT).format(meta.getLastCheckDate()));
				}
				bw.write("\n");
			}
			bw.close();
			
			bw = new BufferedWriter(new FileWriter(fileOfItems, true));
			for(Entry<String, List<ItemIF>> entry : mf.getNewItemsOfFeeds().entrySet()) {
				String feed = entry.getKey();
				List<ItemIF> items = entry.getValue();
				for(ItemIF item : items) {
					String url = "";
					if (item.getGuid() != null)
						url = item.getGuid().getLocation();
					if (url.equals("") || !url.startsWith("http")) 
						url = item.getLink().toString();
					bw.write(feed + "\t" + url + "\t" + item.getTitle().replaceAll("\\r|\\n|\\t", " ").replaceAll("  +", " ").trim() + "\t" + new SimpleDateFormat(DATE_FORMAT).format(item.getDate()) 
							+ "\t" + item.getDescription().replaceAll("\\r|\\n|\\t", " ").replaceAll("  +", " ").trim() + "\n");
				}
			}
			bw.close();
			return true;
		} catch (Exception e) {
			log.error("failed to save: " + e.getMessage());
			return false;
		}

	}
	
}
