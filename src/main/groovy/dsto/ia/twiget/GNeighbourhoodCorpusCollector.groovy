package dsto.ia.twiget

import java.text.SimpleDateFormat;
import java.util.Date;

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import twitter4j.TwitterException

import com.google.common.base.Stopwatch
import com.google.common.collect.Sets

def timer = Stopwatch.createStarted ()

def rootDir = 'incoming/HuT/'
def suffix = '_neighbourhood_profiles.json'
def seedFiles = ["${rootDir}hamzah_q${suffix}", "${rootDir}wassimdoureihi${suffix}", "${rootDir}UthmanB${suffix}"]

def ids = Sets.newHashSet ()
if (args.size () < 1) {

  def json = new JsonSlurper ()
  seedFiles.each {
    def firstLine = new File (it).readLines ()[0]
    println (it + ": " + firstLine)
    def neighbourhood = json.parseText (firstLine).seed
    ids += neighbourhood.user.id
    ids.addAll (neighbourhood.followeeIDs)
    ids.addAll (neighbourhood.followerIDs)
    //  println ("Collecting ${neighbourhood.followeeIDs.size ()} followees and ${neighbourhood.followerIDs.size ()} followers")
  }
} else {
  println ("Reading IDs in from " + args[0])
  println (new File(args[0]).exists ())
  // read them in from the file
  new File (args[0]).eachLine { println (it); if (it.trim ().length ()) ids << Long.parseLong (it.trim ()) }
}
println ("A total of " + ids.size () + " accounts to review")

//def corpus = []
def collector = new UserCorpusCollector ()
def count = 0

def nowTS = new SimpleDateFormat ("yyyyMMdd-HHmmss").format (new Date ());
def failedIDsFile = new File ("incoming/HuT_neighbourhood_corpus-failed_ids-${nowTS}.txt")
if (failedIDsFile.exists ())
  failedIDsFile.delete ()
def failedIDsOut = new BufferedWriter (new FileWriter (failedIDsFile))

new File('incoming/HuT_neighbourhood_corpus-' + nowTS + '.json').withWriter ('UTF-8') { out ->
  ids.each { id ->

    println ("Collecting tweets for @$id")

    collector.setId (id)
    def tweets = collector.collect (UserCorpusCollector.NO_LIMIT)

    if (collector.getError () != null ||
    (tweets.size () == 0 && !collector.isProtectedAccount ())) { // retrieve failed

      println ("  Failed to grab @" + id + ", making a note of it...")
      failedIDsOut.write (id + '\n')
      failedIDsOut.flush ()

    } else {
      count += tweets.size ()
      println ("  Grabbed another " + tweets.size () + " tweets -> " + count + " so far...")
      //  corpus << tweets
      tweets.each { tweet ->
        out.write (JsonOutput.toJson (tweet))
        out.write ('\n')
      }
      out.flush ()
    }
  }
}
println ("Collected " + count + " tweets")

failedIDsOut.close ()
