#Reactive v Shared State Concurrency: Inventory Mgmt w/ Scala/Play/Akka & MongoDB - Example Applications

###Getting Started

- Vagrantfile is in root of project
- Puppet provisioning is in puppet directory
- To get started:
- Install Homebrew
  - https://github.com/Homebrew/homebrew/blob/master/share/doc/homebrew/Installation.md#installation
- Install Ruby (Macs already have Ruby installed) 
  - brew install ruby
- If you encounter permission issues, install RVM 
  - https://github.com/rvm/rvm#installation
- Verify Ruby 2.2.1 is set
- Install Vagrant
  - http://docs.vagrantup.com/v2/installation/
- Install Vagrant Puppet-Install plugin
  - vagrant plugin install vagrant-puppet-install 
- Install Vagrant Librarian-Puppet plugin (version 0.9.0)
  - vagrant plugin install vagrant-librarian-puppet --plugin-version 0.9.0 
- Run vagrant up
- Once this is completed (will take a whiiiiile):
  - vagrant ssh
  - cd /vagrant (this is where our repo is mounted)
  - Now you can cd reactive-inventory or shared-state-inventory
  - Run sbt- sbt
  - You can run the various commands from within sbt- run, test, compile, clean, gen-idea (to generate idea project)

###Vagrant box
- Addressable via private network adapter at 192.168.44.22
- Ubuntu box
  - Base box- https://atlas.hashicorp.com/ubuntu/boxes/trusty64

###MongoDB for Persistence
- We are using MongoDB for our database for both apps
- Installed on our Ubuntu virtual machine via Puppet
- Puppet module- https://forge.puppetlabs.com/puppetlabs/mongodb
- Running on port 27017
- You can log into the mongo db by running mongo from inside the Vagrant box
- We are also using Embedded MongoDB for some of our tests
  - https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo

###StatsD/Graphite for Metrics
- We are using StatsD as an aggregator and Graphite as a metric data store and display vehicle
- StatsD- https://github.com/etsy/statsd
- Graphite- http://graphite.readthedocs.org/en/latest/
- These are installed as a Docker container
  - https://github.com/hopsoft/docker-graphite-statsd
- StatsD port is 8125
- Graphite web gui port is 80

###Load Testing
- We're using JMeter
- Instructions and Test Plan can be found in load_testing directory

###Useful Links
- sbt
  - http://www.scala-sbt.org/
- Reactive Mongo
  - Non-blocking Mongo driver 
  - http://reactivemongo.org/ 
- Akka
  - Actor framework
  - http://akka.io/
- Play framework
  - Web framework built on Akka
  - https://www.playframework.com/
- TrieMap
  - http://www.scala-lang.org/api/2.11.4/index.html#scala.collection.concurrent.TrieMap 
  - http://lampwww.epfl.ch/~prokopec/ctries-snapshot.pdf 
- HBC Digital on Twitter
  - Dynamic, cutting edge technology company with empowered engineers
  - https://twitter.com/hbcdigital
- Dana Peele
  - Application developer
  - https://twitter.com/Dana_S_Peele
