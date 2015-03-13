package dsto.ia.twiget;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.util.Integers;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

public class UserCorpusCollector extends AbstractTwitterApp
{

  public static final int NO_LIMIT = -1;

  public static void main (String[] args) throws IOException, TwitterException
  {
    if (args.length < 1)
    {
      LOG.info ("Usage: UserCorpusCollector <userID|userHandle> [numTweets:default 200]");
      return;
    }
    Long id = Longs.tryParse (args[0]);
    String handle = (id == null) ? args[0] : null;

    Stopwatch timer = Stopwatch.createStarted ();

    UserCorpusCollector collector = new UserCorpusCollector (handle);
    if (id != null) collector.setId (id);

    collector.initialise ();

    int numTweets = (args.length > 1) ? Integers.parseInt (args[1], 200) : 200;

    LOG.info ("Starting to collect the " + numTweets + " most recent of @" + collector.handle + "'s tweets...");

    List<Status> timeline = collector.collect (numTweets);

    LOG.info ("Grabbed " + timeline.size () + " tweets.");

    LOG.info ("Time taken: " + timer.elapsed (TimeUnit.MINUTES) + " minutes");

    String jsonFile = String.format ("%s-corpus-%s.json", collector.handle, Utils.format (new Date ()));
    BufferedWriter out = Files.newBufferedWriter (Paths.get (jsonFile));
    LOG.info ("Writing corpus to " + jsonFile);

    Utils.persist (timeline, out);

    out.close ();

    LOG.info ("Wrote corpus to " + jsonFile);
  }

  private Long userID;
  private String handle;
  private boolean protectedAccount;
  private TwitterException error;

  public UserCorpusCollector ()
  {
    // do nothing.
  }

  public UserCorpusCollector (String handle)
  {
    this.handle = handle;
  }

  public void setHandle (String handle)
  {
    this.handle = handle;
  }

  public void setId (Long id)
  {
    this.userID = id;
  }

  public boolean isProtectedAccount ()
  {
    return protectedAccount;
  }

  public TwitterException getError ()
  {
    return error;
  }

  public List<Status> collect (int numTweets)
  {
    List<Status> tweets = Lists.newArrayList ();
    try
    {
      this.protectedAccount = false;
      this.error = null;

      initialise ();
      if (userID == null)
      {
        User profile = lookupProfile (handle);

        if (profile == null)
        {
          LOG.warn ("Lookup of user @" + handle + " failed. Check log.");
          return tweets; // lookup failed
        }

        if (profile.isProtected ())
        {
          LOG.info ("Account @" + handle + " is protected. Skipping...");
          this.protectedAccount = true;
          return tweets;
        }

        this.handle = profile.getScreenName ();
        this.userID = profile.getId ();

        LOG.info ("  Looked up @" + makeIdentifier ());
        if (this.userID == null)
        {
          LOG.warn ("UserID is still null");
          return tweets;
        }
      }

      String identifier = makeIdentifier ();
      LOG.info ("Collecting tweets for user " + identifier);

      int pageno = 1;
      if (numTweets == NO_LIMIT) numTweets = Integer.MAX_VALUE;
      while (tweets.size () < numTweets)
      {
        int oldSize = tweets.size ();
        Paging page = new Paging (pageno++, 200);

        ResponseList<Status> userTimelineResponse = twitter.getUserTimeline (userID, page);
        tweets.addAll (userTimelineResponse);

        if (oldSize == tweets.size ()) break;

        Utils.pauseBetweenAPICalls (userTimelineResponse);
      }

    } catch (TwitterException e)
    {
      String identifier = makeIdentifier ();
      LOG.warn ("Failure while collecting @" + identifier + "'s timeline.", e);
      this.error = e;
    }
    return tweets;
  }

  private String makeIdentifier ()
  {
    return (handle != null ? handle : "") + "[" + userID + "]";
  }
}
