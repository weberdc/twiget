package dsto.ia.twiget

import groovy.json.JsonOutput

import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch
import com.google.common.collect.Sets

if (! args.contains ('--no-proxy')) {
  GUtils.configureProxy ("/home/${System.properties.'user.name'}/.gradle/gradle.properties")
  println ("Proxy configured...")
}

def timer = Stopwatch.createStarted ()

def stripOptions (argList) { argList.findAll { ! it.startsWith ('-') } }

if (! stripOptions (args).size ()) {
  println ("Must supply a path to a seed file as an argument")
  return
} else {
  println ("Running with args: ${args.join (' ')}")
}

def seedsFile = new File (stripOptions (args)[0])

if (! seedsFile.exists ()) {
  println ("Could not find file $seedsFile (${seedsFile.absolutePath})")
  return
}

def ids = Sets.newHashSet ()
seedsFile.eachLine { line ->
  def id = Long.parseLong (line.split(",")[-1].trim ())
  if (id) ids << id
}

def rootDir = seedsFile.parentFile.absolutePath
def seedFN = seedsFile.name - ".csv"

println ("A total of " + ids.size () + " accounts to collect tweets from.")

def collector = new UserCorpusCollector ()
def count = 0
def idCount = 1

def dateStr = Utils.format (new Date ())
def failedIDs = new FileWriter("${rootDir}/failed_ids-${dateStr}.txt")

def outfile = new File("${rootDir}/${seedFN}-corpus.json")
println ("Writing to ${outfile.absolutePath}")
outfile.withWriter ('UTF-8') { out ->
  ids.each { id ->
    println ("Collecting tweets for id ${idCount++}: @$id")
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
println ("Collected " + count + " tweets in " + timer.elapsed (TimeUnit.MINUTES) + " minutes.")
