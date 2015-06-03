include 'sbtubuntu'
include 'docker'
include '::mongodb::server'

class { 'java':
  package => 'openjdk-7-jdk'
}



docker::run { 'graphite':
  image  => 'hopsoft/graphite-statsd',
  ports => ["80:80", "2003:2003", "8125:8125/udp"]
}
