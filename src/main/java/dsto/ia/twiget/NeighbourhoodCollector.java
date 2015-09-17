package dsto.ia.twiget;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import twitter4j.TwitterException;

public class NeighbourhoodCollector extends AbstractTwitterApp
{

  public static void main (String[] args) throws TwitterException, IOException
  {
    if (args.length < 1)
    {
      System.out.println ("Usage: NeighbourhoodCollector screen_name");
      return;
    }

    String seedHandle = args[0];
    SnowballCollector collector = new SnowballCollector ();
    collector.initialise ();

    Neighbourhood n = collector.collectNeighbourhoodOf (seedHandle, true); // profile, follower IDs, followee IDs

    System.out.printf ("Profile %s (%d)\n", n.profile.getName (), n.profile.getId ());
    System.out.println ("Followed by: " + n.followerIDs.size ());
    System.out.println ("Follows    : " + n.followeeIDs.size ());

    String jsonFile = String.format ("%s-neighbourhood-%s.json", seedHandle, Utils.format (new Date ()));
    BufferedWriter out = Files.newBufferedWriter (Paths.get (jsonFile));

    LOG.info ("Writing corpus to " + jsonFile);

    Utils.persist (n.makeSnapshot (), out);

    out.close ();

    LOG.info ("Wrote corpus to " + jsonFile);
  }

}
