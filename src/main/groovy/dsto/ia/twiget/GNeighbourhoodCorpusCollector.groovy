package dsto.ia.twiget

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import com.google.common.base.Stopwatch
import com.google.common.collect.Sets

if (! args.contains ('--no-proxy')) {
  GUtils.configureProxy ("/home/${System.properties.'user.name'}/.gradle/gradle.properties")
  println ("Proxy configured...")
}

def timer = Stopwatch.createStarted ()

def rootDir = 'incoming/HuT/'
def suffix = '_neighbourhood_profiles.json'
def seedFiles = [
  "${rootDir}hamzah_q${suffix}",
  "${rootDir}wassimdoureihi${suffix}",
  "${rootDir}UthmanB${suffix}"
]

def ids = Sets.newHashSet ()
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
println ("A total of " + ids.size () + " accounts to review")

//def corpus = []
def collector = new UserCorpusCollector ()
def count = 0

def dateStr = Utils.format (new Date ())
def failedIDs = new FileWriter("incoming/failed_ids-${dateStr}.txt")

new File("incoming/HuT_neighbourhood_corpus-${dateStr}.json").withWriter ('UTF-8') { out ->
  ids.each { id ->
    println ("Collecting tweets for @$id")
    collector.setId (id)
    def tweets = collector.collect (UserCorpusCollector.NO_LIMIT)
    
    if (collector.error) {
      failedIDs << "$id\n"
      failedIDs.flush ()
    }      
    
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
failedIDs.close ()
println ("Collected " + count + " tweets")
