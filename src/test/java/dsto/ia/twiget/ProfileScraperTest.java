package dsto.ia.twiget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MultiMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import twitter4j.Status;
import twitter4j.TwitterException;

import com.google.common.base.Stopwatch;

public class ProfileScraperTest
{

  private Stopwatch testTimer = Stopwatch.createUnstarted ();

  @Before
  public void setUp ()
  {
    // Logger log = LogManager.getLogger ("dsto.ia.twiget");
    testTimer.reset ();
    testTimer.start ();
  }

  @After
  public void tearDown ()
  {
    testTimer.stop ();
    System.out.println ("Test took " + testTimer.elapsed (TimeUnit.MINUTES) + " minutes.");
  }

  @Ignore
  @Test
  public void testFindWeberdc () throws IOException
  {
    String handle = /* "wasabi_team3"; */"weberdc"; // "julianburnside";
    ProfileScraper scraper = new ProfileScraper (handle);
    System.out.println ('@' + handle + " has " + scraper.getNumFollowers () + " followers");
    scraper.getFollowerIds ();
    System.out.println ('@' + handle + " has id " + scraper.getTargetId ());
    scraper.getFriendIds ();
    System.out.println ('@' + handle + " has " + scraper.getNumFriends () + " friends");
    System.out.println ('@' + handle + " has posted " + scraper.getUser ().getStatusesCount () + " tweets");
    List<Status> tweets = scraper.fetchTweets (200);// ProfileScraper.NO_TWEET_LIMIT);
    for (Status tweet : tweets)
    {
      System.out.println ("  " + tweet.getText ());
    }
    File file = File.createTempFile ("twinfo", ".json");
    Writer out = new PrintWriter (new FileWriter (file), true);
    System.out.println ("Persisting file to\n" + file.getAbsolutePath ());
    Utils.persist (scraper.getSnapshot (), out);
  }

  @Ignore
  @Test
  public void testFindWasabaTeam1Network () throws IOException
  {
    Stopwatch timer = Stopwatch.createStarted ();
    String seedHandle = "extra_dcw";// "weberdc";// "wasabi_team1";
    String handle = '@' + seedHandle;
    ProfileWalker walker = new ProfileWalker (seedHandle);
    walker.setSkipFollowersThreshold (800);
    walker.setSkipFriendsThreshold (500);
    long seedId = walker.getId (seedHandle);
    System.out.println (seedId);
    System.out.println (handle + " has " + walker.getNumFollowers (seedHandle) + " followers");

    walker.buildFollowerNetwork (seedId);
    @SuppressWarnings ("unused")
    Map<Long, ProfileScraper> profilesById = walker.getProfilesById ();
    System.out.println ("Test took " + timer.elapsed (TimeUnit.MINUTES) + " minutes.");
    timer = Stopwatch.createStarted ();
    File file = new File ("extra_dcw_user_network.json");
    Writer out = new PrintWriter (new FileWriter (file), true);
    Utils.persist (walker.getSnapshot (), out);
    System.out.println ("Persisting results took " + timer.elapsed (TimeUnit.MINUTES) + " minutes.");
  }

  @SuppressWarnings ("unchecked")
  @Test
  public void testSnowballIDCollect () throws IOException, TwitterException
  {
    String seedHandle = "weberdc";// "wasabi_team1";// "extra_dcw";
    SnowballCollector collector = new SnowballCollector ();
    collector.initialise ();

    Neighbourhood n = collector.collectNeighbourhoodOf (seedHandle); // profile, follower IDs, followee IDs

    System.out.printf ("Profile %s (%d)\n", n.profile.getName (), n.profile.getId ());
    System.out.println ("Followers: " + n.followerIDs.size ());
    System.out.println ("Followees: " + n.followeeIDs.size ());

    writeToJSONFile (n.makeSnapshot (), seedHandle + "_neighbourhood-");

    MultiMap followerNetwork = collector.collectNetworkFrom (seedHandle, 1);

    writeToJSONFile (followerNetwork, seedHandle + "_network1-");

    MultiMap followerNetwork2 = collector.collectNetworkFrom (seedHandle, true, 2);

    writeToJSONFile (followerNetwork2, seedHandle + "_network2-");

    System.out.printf ("Profile %s (%d)\n", n.profile.getName (), n.profile.getId ());
    System.out.println ("Followers: " + n.followerIDs.size ());
    System.out.println ("Followees: " + n.followeeIDs.size ());
    System.out.println ("Done!");
  }

  private File writeToJSONFile (Map<? extends Object, Object> data, String filePrefix) throws IOException
  {
    File file = File.createTempFile (filePrefix, ".json");
    Utils.persist (data, new FileWriter (file));
    System.out.println ("Profile neighbourhood written to: " + file.getAbsolutePath ());
    return file.getAbsoluteFile ();
  }
}
