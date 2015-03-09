package dsto.ia.twiget;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class AbstractTwitterApp
{
  protected static Logger LOG = LogManager.getLogger ("dsto.ia.twiget");

  protected Twitter twitter;
  protected RateLimitStatus rateLimitStatus;

  public void initialise () throws TwitterException
  {
    if (twitter == null)
    {
      // gather info
      // The factory instance is re-useable and thread safe.
      twitter = TwitterFactory.getSingleton ();
      rateLimitStatus = twitter.getRateLimitStatus ("users").get ("/users/show/:id");
      Utils.pauseBetweenAPICalls (rateLimitStatus);
      LOG.info ("Twitter connection established.");
    }
  }

  protected User lookupProfile (Object target)
  {
    String targetHandle = null;
    long targetId = -1L;

    if (target instanceof String)
      targetHandle = target.toString ();
    else
      targetId = (Long) target;

    try
    {
      initialise ();

      User user = null;
      Utils.pauseBetweenAPICalls (rateLimitStatus);

      if (targetHandle != null)
      {
        // for (String key : twitter.getRateLimitStatus ("users").keySet ())
        // System.out.println ("** " + key);

        user = twitter.showUser (targetHandle);
        // targetId = user.getId ();
      } else
      {
        // pauseBetweenAPICalls (twitter.getRateLimitStatus ().get ("/users/show/:id"));
        user = twitter.showUser (targetId);
        // pauseBetweenAPICalls (user);
        targetHandle = user.getScreenName ();
      }
      Utils.pauseBetweenAPICalls (user);
      LOG.info ("Twitter user '@" + targetHandle + "' found.");

      return user;

    } catch (TwitterException e)
    {
      e.printStackTrace ();
    }
    return null;
  }

}