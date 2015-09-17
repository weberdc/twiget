package dsto.ia.twiget;

import java.util.Map;

import twitter4j.Query;
import twitter4j.Query.ResultType;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

public class RecentTweetCollector
{

  // Set this to your actual CONSUMER KEY and SECRET for your application as given to you by dev.twitter.com
  private static final String CONSUMER_KEY = "Q5BYFVHcCete7exbWKa5YceZp";// "--your key goes here--";
  private static final String CONSUMER_SECRET = "H3pcPx4SsZnmLwrBq3wjtLUcWQWH6LcFYdMTqdx8D6Fetqs2qm";// "--your secret goes here--";

  // How many tweets to retrieve in every call to Twitter. 100 is the maximum allowed in the API
  private static final int TWEETS_PER_QUERY = 100;

  // This controls how many queries, maximum, we will make of Twitter before cutting off the results.
  // You will retrieve up to MAX_QUERIES*TWEETS_PER_QUERY tweets.
  //
  // If you set MAX_QUERIES high enough (e.g., over 450), you will undoubtedly hit your rate limits
  // and you an see the program sleep until the rate limits reset
  private static final int MAX_QUERIES = 5;

  // We're wanting the most recent tweets of this person
  private static String PERSON_OF_INTEREST = "weberdc";// "wasabi_team3";

  // What we want to search for in this program. Justin Bieber always returns as many results as you could
  // ever want, so it's safe to assume we'll get multiple pages back...
  private static final String SEARCH_TERM = "from:" + PERSON_OF_INTEREST;// "Justin Bieber";

  /**
   * Replace newlines and tabs in text with escaped versions to making printing cleaner
   *
   * @param text The text of a tweet, sometimes with embedded newlines and tabs
   * @return The text passed in, but with the newlines and tabs replaced
   */
  public static String cleanText (String text)
  {
    text = text.replace ("\n", "\\n");
    text = text.replace ("\t", "\\t");

    return text;
  }

  /**
   * Retrieve the "bearer" token from Twitter in order to make application-authenticated calls.
   *
   * This is the first step in doing application authentication, as described in Twitter's documentation at
   * https://dev.twitter.com/docs/auth/application-only-auth
   *
   * Note that if there's an error in this process, we just print a message and quit. That's a pretty dramatic side
   * effect, and a better implementation would pass an error back up the line...
   *
   * @return The oAuth2 bearer token
   */
  public static OAuth2Token getOAuth2Token ()
  {
    OAuth2Token token = null;
    ConfigurationBuilder cb;

    cb = new ConfigurationBuilder ();
    cb.setApplicationOnlyAuthEnabled (true);

    cb.setOAuthConsumerKey (CONSUMER_KEY).setOAuthConsumerSecret (CONSUMER_SECRET);

    try
    {
      token = new TwitterFactory (cb.build ()).getInstance ().getOAuth2Token ();
    } catch (Exception e)
    {
      System.out.println ("Could not get OAuth2 token");
      e.printStackTrace ();
      System.exit (0);
    }

    return token;
  }

  /**
   * Get a fully application-authenticated Twitter object useful for making subsequent calls.
   *
   * @return Twitter4J Twitter object that's ready for API calls
   */
  public static Twitter getTwitter ()
  {
    OAuth2Token token;

    // First step, get a "bearer" token that can be used for our requests
    token = getOAuth2Token ();

    // Now, configure our new Twitter object to use application authentication and provide it with
    // our CONSUMER key and secret and the bearer token we got back from Twitter
    ConfigurationBuilder cb = new ConfigurationBuilder ();

    cb.setApplicationOnlyAuthEnabled (true);

    cb.setOAuthConsumerKey (CONSUMER_KEY);
    cb.setOAuthConsumerSecret (CONSUMER_SECRET);

    cb.setOAuth2TokenType (token.getTokenType ());
    cb.setOAuth2AccessToken (token.getAccessToken ());

    // And create the Twitter object!
    return new TwitterFactory (cb.build ()).getInstance ();

  }

  public static void main (String[] args)
  {
    // We're curious how many tweets, in total, we've retrieved. Note that TWEETS_PER_QUERY is an upper limit,
    // but Twitter can and often will retrieve far fewer tweets
    int totalTweets = 0;

    // This variable is the key to our retrieving multiple blocks of tweets. In each batch of tweets we retrieve,
    // we use this variable to remember the LOWEST tweet ID. Tweet IDs are (java) longs, and they are roughly
    // sequential over time. Without setting the MaxId in the query, Twitter will always retrieve the most
    // recent tweets. Thus, to retrieve a second (or third or ...) batch of Tweets, we need to set the Max Id
    // in the query to be one less than the lowest Tweet ID we've seen already. This allows us to page backwards
    // through time to retrieve additional blocks of tweets
    long maxID = -1;

    Twitter twitter = getTwitter ();

    // Now do a simple search to show that the tokens work
    try
    {
      // There are limits on how fast you can make API calls to Twitter, and if you have hit your limit
      // and continue to make calls Twitter will get annoyed with you. I've found that going past your
      // limits now and then doesn't seem to be problematic, but if you have a program that keeps banging
      // the API when you're not allowed you will eventually get shut down.
      //
      // Thus, the proper thing to do is always check your limits BEFORE making a call, and if you have
      // hit your limits sleeping until you are allowed to make calls again.
      //
      // Every time you call the Twitter API, it tells you how many calls you have left, so you don't have
      // to ask about the next call. But before the first call, we need to find out whether we're already
      // at our limit.

      // This returns all the various rate limits in effect for us with the Twitter API
      Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus ("search");

      // This finds the rate limit specifically for doing the search API call we use in this program
      RateLimitStatus searchTweetsRateLimit = rateLimitStatus.get ("/search/tweets");

      // Always nice to see these things when debugging code...
      System.out.printf ("You have %d calls remaining out of %d, Limit resets in %d seconds\n",
                         searchTweetsRateLimit.getRemaining (), searchTweetsRateLimit.getLimit (),
                         searchTweetsRateLimit.getSecondsUntilReset ());

      // Grab the user's profile and find the ID of their most recent tweet
      User user = twitter.showUser (PERSON_OF_INTEREST);
      System.err.println ('@' + PERSON_OF_INTEREST + " : " + user.getStatus ());
      maxID = user.getStatus ().getId () + 1;

      // This is the loop that retrieve multiple blocks of tweets from Twitter
      for (int queryNumber = 0; queryNumber < MAX_QUERIES; queryNumber++)
      {
        System.out.printf ("\n\n!!! Starting loop %d\n\n", queryNumber);

        // Do we need to delay because we've already hit our rate limits?
        if (searchTweetsRateLimit.getRemaining () == 0)
        {
          // Yes we do, unfortunately ...
          System.out.printf ("!!! Sleeping for %d seconds due to rate limits\n",
                             searchTweetsRateLimit.getSecondsUntilReset ());

          // If you sleep exactly the number of seconds, you can make your query a bit too early
          // and still get an error for exceeding rate limitations
          //
          // Adding two seconds seems to do the trick. Sadly, even just adding one second still triggers a
          // rate limit exception more often than not. I have no idea why, and I know from a Comp Sci
          // standpoint this is really bad, but just add in 2 seconds and go about your business. Or else.
          Thread.sleep ((searchTweetsRateLimit.getSecondsUntilReset () + 2) * 1000l);
        }

        Query q = new Query (SEARCH_TERM); // Search for tweets that contains this term
        q.setCount (TWEETS_PER_QUERY); // How many tweets, max, to retrieve
        q.resultType (ResultType.recent); // Get all tweets
        q.setLang ("en"); // English language tweets, please
        // if (queryNumber == 0) q.setMaxId (maxID); // start from most recent tweet

        // If maxID is -1, then this is our first call and we do not want to tell Twitter what the maximum
        // tweet id is we want to retrieve. But if it is not -1, then it represents the lowest tweet ID
        // we've seen, so we want to start at it-1 (if we start at maxID, we would see the lowest tweet
        // a second time...
        if (maxID != -1)
        {
          q.setMaxId (maxID - 1);
        }

        // This actually does the search on Twitter and makes the call across the network
        QueryResult r = twitter.search (q);

        // If there are NO tweets in the result set, it is Twitter's way of telling us that there are no
        // more tweets to be retrieved. Remember that Twitter's search index only contains about a week's
        // worth of tweets, and uncommon search terms can run out of week before they run out of tweets
        if (r.getTweets ().size () == 0)
        {
          break; // Nothing? We must be done
        }

        // loop through all the tweets and process them. In this sample program, we just print them
        // out, but in a real application you might save them to a database, a CSV file, do some
        // analysis on them, whatever...
        for (Status s : r.getTweets ()) // Loop through all the tweets...
        {
          // Increment our count of tweets retrieved
          totalTweets++;

          // Keep track of the lowest tweet ID. If you do not do this, you cannot retrieve multiple
          // blocks of tweets...
          if (maxID == -1 || s.getId () < maxID)
          {
            maxID = s.getId ();
          }

          // Do something with the tweet....
          System.out.printf ("At %s, @%-20s said:  %s\n", s.getCreatedAt ().toString (), s.getUser ().getScreenName (),
                             cleanText (s.getText ()));

        }

        // As part of what gets returned from Twitter when we make the search API call, we get an updated
        // status on rate limits. We save this now so at the top of the loop we can decide whether we need
        // to sleep or not before making the next call.
        searchTweetsRateLimit = r.getRateLimitStatus ();
      }

    } catch (Exception e)
    {
      // Catch all -- you're going to read the stack trace and figure out what needs to be done to fix it
      System.out.println ("That didn't work well...wonder why?");

      e.printStackTrace ();

    }

    System.out.printf ("\n\nA total of %d tweets retrieved\n", totalTweets);
    // That's all, folks!

  }
}
