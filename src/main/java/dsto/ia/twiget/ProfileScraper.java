package dsto.ia.twiget;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProfileScraper
{
  @SuppressWarnings ("unused")
  private static Logger LOG = LogManager.getLogger ("dsto.ia.twiget");

  public static final int NO_TWEET_LIMIT = -1;

  public static boolean MOCK = false;

  private String targetHandle;

  private Twitter twitter;

  private User user;

  private Set<Long> followerIds = Sets.newHashSet ();
  private Set<Long> friendIds = Sets.newHashSet ();
  private List<Status> tweets = Lists.newArrayList ();

  private long targetId;

  public ProfileScraper (String targetHandle)
  {
    this.targetHandle = targetHandle;
  }

  public ProfileScraper (long id)
  {
    this.targetId = id;
  }

  public int getNumFollowers ()
  {
    fetchProfileInfo ();
    return user.getFollowersCount ();
  }

  public int getNumFriends ()
  {
    fetchProfileInfo ();
    return user.getFriendsCount ();
  }

  public Set<Long> getFollowerIds ()
  {
    fetchFollowerFriendInfo ();
    return followerIds;
  }

  public Set<Long> getFriendIds ()
  {
    fetchFollowerFriendInfo ();
    return friendIds;
  }

  public long getTargetId ()
  {
    fetchProfileInfo ();
    return user.getId ();
  }

  public User getUser ()
  {
    return user;
  }

  private void fetchProfileInfo ()
  {
    if (twitter == null)
    {
      // gather info
      // The factory instance is re-useable and thread safe.
      twitter = TwitterFactory.getSingleton ();
      try
      {
        Utils.pauseBetweenAPICalls (twitter.getRateLimitStatus ("users").get ("/users/show/:id"));
        if (targetHandle != null)
        {
          // for (String key : twitter.getRateLimitStatus ("users").keySet ())
          // System.out.println ("** " + key);

          user = twitter.showUser (targetHandle);
          targetId = user.getId ();
        } else
        {
          // pauseBetweenAPICalls (twitter.getRateLimitStatus ().get ("/users/show/:id"));
          user = twitter.showUser (targetId);
          // pauseBetweenAPICalls (user);
          targetHandle = user.getScreenName ();
        }
        Utils.pauseBetweenAPICalls (user);
        System.out.println ("Twitter connection established, user '@" + targetHandle + "' found.");

      } catch (TwitterException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }
    }
  }

  private void fetchFollowerFriendInfo ()
  {
    fetchProfileInfo ();

    if (followerIds.isEmpty () && friendIds.isEmpty ())
    {
      // grab follower IDs
      Utils.fetchFollowerIds (twitter, user.getId (), followerIds, false);
      System.out.println ("#followers: " + followerIds.size ());
      // grab friend IDs
      Utils.fetchFriendIds (twitter, user.getId (), friendIds, false);
    }
    System.out.println ("#friends:   " + friendIds.size ());
    // grab tweets?
    // if (fetchTweets)
    // grabTweets (user.getId ());

  }

  public List<Status> fetchTweets (int roughLimit)
  {
    if (tweets.isEmpty ())
    {
      Query query = new Query ("from:" + getTargetHandle ());
      // query.since ("2006-01-01");
      query.setCount (100);
      try
      {
        QueryResult result = null;
        while ((result == null || result.hasNext ()) && (roughLimit == -1 || tweets.size () < roughLimit))
        {
          result = twitter.search ().search (query);

          System.out.println ("# tweets from search: " + result.getCount ());

          System.out.println (result);
          List<Status> tws = result.getTweets ();
          for (Status tw : tws)
          {
            System.out.println ("tw: " + tw);
          }
          tweets.addAll (tws);

          query = result.nextQuery ();

          Utils.pauseBetweenAPICalls (result);
        }
      } catch (TwitterException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }
    }
    System.out.println ("returning " + tweets.size () + " tweets");
    return tweets;
  }

  protected Map<String, Object> getSnapshot ()
  {
    Map<String, Object> map = Maps.newHashMap ();
    map.put ("user", user);
    if (!friendIds.isEmpty ()) map.put ("friendIds", friendIds);
    if (!followerIds.isEmpty ()) map.put ("followerIds", followerIds);
    if (!tweets.isEmpty ()) map.put ("tweets", tweets);
    return map;
  }

  public String getTargetHandle ()
  {
    fetchProfileInfo ();
    return targetHandle;
  }

}
