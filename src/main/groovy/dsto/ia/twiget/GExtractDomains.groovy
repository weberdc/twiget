package dsto.ia.twiget

def usage () {
  println ("groovy GExtractDomains.groovy [--pretty|-p] [--debug|-v] [--help|-h|-?]")
  println ("  prints out as a JSON map all URLs (cleaned) in stdin, along with their counts.")
  println ("  --pretty prints out nicely for human readability (ie. multiline)")
  println ("  --debug  prints out debug printlns")
  println ("  --help   prints this help message")
}

def clean (domain) {
  // strip port numbers out
  if (domain =~ /:/) domain = domain.substring (0, domain.indexOf (':'))
  
  // remove www subdomain - obvious
  if (domain =~ /^www/) domain -= 'www.'
  
  // reasons to reject
  if (! (domain =~ /\./) || // no dots
      domain =~ /^\.+$/  || // only dots
      domain =~ /\.$/    || // ends with a dot
      domain =~ /^\./)      // starts with a dot
    domain = null

  // uniform case  
  domain?.toLowerCase ()
}

def updateCount (domain, counts) {
  if (domain)
    counts[domain] = (counts[domain] ? counts[domain] + 1 : 1)
}

if (args.contains ('--help') || args.contains ('-h') || args.contains ('-?')) {
  usage ()
  System.exit (0)
}

def pretty = args.contains ('--pretty') || args.contains ('-p')
def debug = args.contains ('--debug') || args.contains ('-v')

def counts = [:] // <String, Integer>

def i = 0
System.in.withReader { reader ->
  def line = "x"
  while (line) {
    line = reader.readLine ()
    if (! line) break
    
    if (debug) printf ("%10d: %s...\n", ++i, line[0..70])
  
    (line =~ /https?:\/\/([^\/\\"]*)/).each {
      def domain = clean (it[1])
      updateCount (domain, counts)
    }
  }
}

if (pretty) {
  println ("{")
  def size = counts.size ()
  counts.keySet ().sort ().each { key ->
    print ("  $key : ${counts[key]}")
    if (--size) println (',')
    else println ()
  }
  println ("}")
} else {
  println ("{" + counts.collect { "${it.getKey ()}:${it.getValue ()}" }.join (',') + "}")
}



//[a-zA-Z0-9\-]