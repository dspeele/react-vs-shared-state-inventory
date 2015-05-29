exec { 'set-repo':
  command => 'echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list'
  before  => Exec['update']
}

exec { 'update':
  command => 'sudo apt-get update'
  before  => Exec['set-noninteractive']
}

exec { 'set-noninteractive':
  command => 'export DEBIAN_FRONTEND=noninteractive'
  before  => Exec['install-sbt']
}

exec { 'install-sbt':
  command => 'sudo apt-get -y --force-yes install sbt'
  before  => Class['nodejs']
}

class { 'nodejs':
    version => 'latest',
}

class {'::mongodb::server':
  auth => true,
}

mongodb::db {'shared_state_inventory':
  user          => 'shared_state_inventory',
  password_hash => 'shared_state_inventory_123',
}

class {'graphite':}

class {'statsd':
  backends     => ['./backends/graphite'],
  graphiteHost => 'localhost',
  require      => Class['nodejs']
}
