package dsto.ia.twiget

class GUtils
{
  static configureProxy (gradlePropertiesFile) {
    def proxyProps = new Properties ()
    proxyProps.load (new FileReader (gradlePropertiesFile))
    // gradle proxy properties are written as lines "systemProp.x.y = v"
    proxyProps.collect { k, v -> System.properties."${k - 'systemProp.'}" = v }
  }
}
