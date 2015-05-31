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
- Install Vagrant Puppet-Install plugin
  - vagrant plugin install vagrant-puppet-install 
- Install Vagrant Librarian-Puppet plugin (version 0.9.0)
  - vagrant plugin install vagrant-librarian-puppet --plugin-version 0.9.0 
- Run vagrant up
- Once this is completed (will take a whiiiiile):
  - vagrant ssh
  - cd /vagrant (this is where our repo is mounted
  - Now you can cd reactive-inventory or shared-state-inventory
  - Run sbt- sbt
  - You can run the various commands from within sbt- run, test, compile, clean, gen-idea (to generate idea project)
