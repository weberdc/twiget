package dsto.ia.twiget;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import twitter4j.IDs;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Utils
{
  private static Logger LOG = LogManager.getLogger ("dsto.ia.twiget");
  private static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat ("yyyyMMdd-HHmm");

  /**
   * Persists a {@link Map} of <String, Object> as JSON to the given {@link Writer}. <em>NB</em> It's the responsibility
   * of the caller to call {@link Writer#close()}.
   *
   * @param map The {@link Map} of data to write out as JSON.
   * @param out The {@link Writer} to write the data to.
   */
  public static void persist (/* Map<? extends Object, Object> */Object map, Writer out)
  {
    ObjectMapper mapper = new ObjectMapper ();

    try
    {
      mapper.writeValue (out, map);
      // out.close (); // this is the responsibility of the owner of the Writer

    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace ();
    }
  }

  public static void pauseBetweenAPICalls (RateLimitStatus rateLimitStatus)
  {
    try
    {
      // always keep a few for testing the rate limit
      if (rateLimitStatus == null || rateLimitStatus.getRemaining () < 3)
      {
        int secondsUntilReset = rateLimitStatus != null ? rateLimitStatus.getSecondsUntilReset () : 15 * 60;
        LOG.info ("Waiting " + secondsUntilReset + "s at " + nowStr () + "...");
        Thread.sleep ((secondsUntilReset + 2) * 1000);
      } else
      {
        LOG.info ("Waiting " + Utils.DEFAULT_DELAY + "ms ...");
        Thread.sleep (Utils.DEFAULT_DELAY);
      }
    } catch (InterruptedException e)
    {
      LOG.warn ("Interrupted while pausing between API calls", e);
    }
  }

  private static SimpleDateFormat HMS = new SimpleDateFormat ("HH:mm:ss");

  public static String nowStr ()
  {
    return HMS.format (new Date ());
  }

  public static void pauseBetweenAPICalls (TwitterResponse response)
  {
    RateLimitStatus rateLimitStatus = response != null ? response.getRateLimitStatus () : null;
    pauseBetweenAPICalls (rateLimitStatus);
  }

  public static final int DEFAULT_DELAY = 500;

  public static void grabIds (Twitter twitter, String purpose, long userId, Set<Long> toPopulate,
                              BiFunction<Long, Long, Optional<IDs>> getIDs)
  {
    try
    {
      IDs idsObj = null;
      RateLimitStatus rateLimitStatus = twitter.getRateLimitStatus (purpose).get ('/' + purpose + "/ids");
      long cursor = -1;
      int it = 1;
      while (cursor != 0)
      {
        pauseBetweenAPICalls (rateLimitStatus);

        Optional<IDs> optional = getIDs.apply (userId, cursor);

        if (!optional.isPresent ()) break;

        idsObj = optional.get ();

        for (long id : idsObj.getIDs ())
          toPopulate.add (id);

        rateLimitStatus = idsObj.getRateLimitStatus ();
        if (rateLimitStatus == null) System.err.println ("RateLimitStatus is null!");
        int remaining = rateLimitStatus != null ? rateLimitStatus.getRemaining () : 0;
        System.out.printf ("#%03d cursor %d, remaining calls %d\n", (it++), cursor, remaining);

        System.out.println ("Number of " + purpose + " IDs grabbed " + idsObj.getIDs ().length);
        cursor = idsObj.getNextCursor ();

        // if (idsObj.getIDs ().length > 1000)
        // {
        // if (purpose.equals ("friends"))
        // System.out.println ("  num friends " + user.getFriendsCount ());
        // else
        // System.out.println ("  num followers " + user.getFollowersCount ());
        // }

      }
      if (idsObj != null && rateLimitStatus != null)
      {
        int secondsUntilReset = rateLimitStatus.getSecondsUntilReset ();
        int remainingCalls = rateLimitStatus.getRemaining ();
        System.out.println (secondsUntilReset + " until reset... " + remainingCalls + " calls remain");
      }

    } catch (TwitterException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace ();
    }
  }

  public static Set<Long> fetchFollowerIds (Twitter twitter, long userId)
  {
    return fetchFollowerIds (twitter, userId, Sets.newHashSet (), true);
  }

  public static Set<Long> fetchFollowerIds (Twitter twitter, long userId, Set<Long> followerIds, boolean force)
  {
    if (force || followerIds.isEmpty ())
    {
      grabIds (twitter, "followers", userId, followerIds, (uid, cursor) -> {
        try
        {
          return Optional.of (twitter.getFollowersIDs (uid, cursor));
        } catch (Exception e)
        {
          LOG.warn ("Error collecting follower IDs", e);
          return Optional.empty ();
        }
      });
    }
    return followerIds;
  }

  public static Set<Long> fetchFolloweeIds (Twitter twitter, long userId)
  {
    return fetchFolloweeIds (twitter, userId, Sets.newHashSet (), true);
  }

  public static Set<Long> fetchFolloweeIds (Twitter twitter, long userId, Set<Long> followeeIds, boolean force)
  {
    return fetchFriendIds (twitter, userId, followeeIds, force);
  }

  public static Set<Long> fetchFriendIds (Twitter twitter, long userId)
  {
    return fetchFriendIds (twitter, userId, Sets.newHashSet (), true);
  }

  public static Set<Long> fetchFriendIds (Twitter twitter, long userId, Set<Long> friendIds, boolean force)
  {
    if (force || friendIds.isEmpty ())
    {
      grabIds (twitter, "friends", userId, friendIds, (uid, cursor) -> {
        try
        {
          return Optional.of (twitter.getFriendsIDs (uid, cursor));
        } catch (Exception e)
        {
          LOG.warn ("Error collecting followee IDs", e);
          return Optional.empty ();
        }
      });
    }
    return friendIds;
  }

  public static String format (Date date)
  {
    return DATETIME_FORMAT.format (date);
  }

  public static Map<Object, Object> toMap (Object... objects)
  {
    Map<Object, Object> m = Maps.newHashMap ();

    for (int i = 0; i < Math.floor (objects.length / 2.0); i++)
      m.put (objects[2 * i], objects[2 * i + 1]);

    return m;
  }
}
