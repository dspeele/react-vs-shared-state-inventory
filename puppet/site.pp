include 'scala'

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
