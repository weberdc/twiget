package dsto.ia.twiget;

import java.io.IOException;
import java.util.Set;

import twitter4j.TwitterException;
import twitter4j.User;

public class NeighbourhoodProfilesCollector extends AbstractTwitterApp
{

  public static void main (String[] args) throws TwitterException, IOException
  {
    if (args.length < 2)
    {
      System.out.println ("Usage: NeighbourhoodProfilesCollector screen_name friendThreshold (-1 for all)");
      return;
    }

    String seedHandle = args[0];
    int threshold = Integer.parseInt (args[1]);

    LOG.info ("starting neighbourhood profile collection from @" + seedHandle + " (up to " + threshold + ")");

    SnowballCollector collector = new SnowballCollector ();
    collector.initialise ();

    Set<User> profiles = collector.collectAndExportNeighbouringProfiles (seedHandle, threshold);

    System.out.printf ("Starting from %s, we found (%d) profiles.\n", seedHandle, profiles.size ());
  }
}
