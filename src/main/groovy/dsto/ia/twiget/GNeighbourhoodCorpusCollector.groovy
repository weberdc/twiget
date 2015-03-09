package dsto.ia.twiget

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import com.google.common.base.Stopwatch
import com.google.common.collect.Sets

def timer = Stopwatch.createStarted ()

def rootDir = 'data/HuT/'
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

new File('incoming/HuT_neighbourhood_corpus.json').withWriter ('UTF-8') { out ->
  ids.each { id ->
    println ("Collecting tweets for @$id")
    collector.setId (id)
    def tweets = collector.collect (UserCorpusCollector.NO_LIMIT)
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
println ("Collected " + count + " tweets")
