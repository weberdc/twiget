# Twiget - Twitter data gathering scripts

Twiget is my collection of experiments and utilities to gather Twitter data.

# Scripts/Apps

## GUserCorpusCollector

Takes a commandline argument specifying a seeds file in csv form, where the last column is the Twitter ID. The file might simply be a list of IDs, or a list of rows with the screen name and then the ID,
separated by a comma.

An optional flag is '--no-proxy', which will tell the script to not look for proxy settings in `/home/'whoami'/.gradle/gradle.properties`.

Assuming an input file of `seeds.csv`, a file will be produced titled something like `seeds-corpus.json` which includes as many tweets as could be gathered from the list of seeds, with each tweet being a JSON blob on each line of the file, along with another file called `failed_ids-CURRENT_DATETIME.txt` (where `CURRENT_DATETIME` is replaced with a formatted string of the current date/time value), which is list of the IDs which failed to collect anything due to an error (i.e. an exception occurred while attempting to collect the tweets). This can then form the input to another run of this program to attempt to gather more tweets, as sometimes the attempt to gather tweets for a given ID simply fails once but works the next time.

## GNeighbourhoodCorpusCollector (pretty much not current, but should still work)

This program builds a set of IDs of users whose tweets to collect to build a corpus of a community. Starting with hardwired seeds in my custom neighbourhood profile files, it gathers the IDs of the seeds, their followers and whom they follow and then progressively gathers those users' tweets and writes them out (one tweet as a JSON blob per line) to a single file. IDs where a failure to gather the tweets (i.e. an exception occurred) are gathered to a file starting with `failed_ids-` so that repeat attempts can be made to gather their tweets.

An optional flag is '--no-proxy', which will tell the script to not look for proxy settings in `/home/'whoami'/.gradle/gradle.properties`.

This may be better refactored into a program that builds the list of IDs from the neighbourhood profile files and then combined with `GUserCorpusCollector`.

## GRestructureNeighbourhoodData (not current)

Starting with hardwired neighbourhood profile files, which are hetereogeneous in structure, produces a homogeneously structured file of JSON profiles, even if some of the information is lacking. This was done to support Peter Paraskevopoulos parsing the data to put it into Hive and Titan.

## GFieldSummator (not current, but should work)

Tells you an upper bound on how many tweets (roughly) would need to be gathered based on the profiles contained in a specific neighbourhood profile file.

## NeighbourhoodCollector (not current, but should work)

Given a screen name, this collects the specified user's profile, and the IDs of their followers and whom they follow, and write the data out as a single JSON blob to a file titled `<seedname>-neighbourhood-<date>.json`.

## NeighbourhoodProfileCollector (not current, but should work)

Given a screen name, this collects the specified user's profile, and the profiles of their followers and whom they follow, and the IDs of _their_ followers and whom they follow, and write the data out as a single JSON blob to a file titled `<seedname>_neighbourhood_profiles.json`.

## RecentTweetCollector

Adopted from online somewhere. Gathers the most recent accessible tweets by a given user

## UserCorpusCollector (not current, but should work)

Collects the tweets from one specific user, specified with a screen name or a Twitter ID.

## UserNetworkCollector (not current)

Starting from a specific user (screen name or Twitter ID), collect the follower/followee relationships down to a particular depth and just report who follows whom as a multimap.


