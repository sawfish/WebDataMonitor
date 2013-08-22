package cshen.data.collection;
/**
 * 
 */


import java.util.*;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.json.DataObjectFactory;

/**
 * This is an abstract class to read twitter stream for event summarization app. 
 * A built-in {@link #buffer} holds crawled but unread tweets. 
 * Function {@link #read()} called by event summarization app.
 * @author shc2pal
 * 
 */
public class TwitterStreamReader {
	public static void main(String[] args) throws Exception {
		TwitterStreamReader streamReader = new TwitterStreamReader();
		streamReader.setQuery(new String[] {"lmao"});
		
		if (streamReader.start()) {
			while(true) {
				List<String> tweets = streamReader.read();
				for(int i = 0; i < tweets.size() && i < 2; i++) {
					System.err.println(tweets.get(i)); 
				}
				if (tweets.size() > 2) 
					System.err.println("And other " + (tweets.size() - 2) + "tweets\n");
				
				Thread.sleep(5000);
			}
		}
	}
	protected boolean stop;
	/**
	 * buffer to hold unread tweets
	 */
	protected Queue<String> buffer = new LinkedList<String>();
	
	Thread loadThread = null;
	
	FilterQuery query;
	TwitterStream twitterStream;
	
	// put preprocessor here is because we do not need to wait long time for preprocessing in debug
	// finally it can be put out, or leave it here.
	public TwitterStreamReader() throws Exception {
	}
	
	public boolean end() {
		twitterStream.shutdown();
		stop = true;
		return true;
	}
	
	public boolean isFinished() {
		return !loadThread.isAlive();
	}
	protected void load() {
		StatusListener listener = new StatusListener() {
			@Override
	        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
			@Override
	        public void onException(Exception ex) {
	            ex.printStackTrace();
	        }
			@Override
			public void onScrubGeo(long arg0, long arg1) {}
			@Override
			public void onStallWarning(StallWarning arg0) {}
			@Override
	        public void onStatus(Status status) {
				String rawJSON = DataObjectFactory.getRawJSON(status);
				synchronized (buffer) {
					buffer.add(rawJSON);
				}
	        }
			@Override
	        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
	    };
	    twitterStream = new TwitterStreamFactory().getInstance();
	    twitterStream.addListener(listener);
	    // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
	    twitterStream.filter(query);
	}
	/**
	 * function to read tweets
	 * @return
	 */
	public List<String> read() {
		List<String> ret = null;
		synchronized (buffer) {
			ret = new ArrayList<String>(buffer.size());
			ret.addAll(buffer);
			buffer.clear();
		}
		return ret;
	}
	
	public void setQuery(String[] keywords) {
		query = new FilterQuery(0, null, keywords);
	}
	
	public boolean start() {
		if (loadThread != null) {
			return false;
		}
		
		stop = false;
		loadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				load();
			}
		});
		loadThread.start();
		return true;
	}
}
