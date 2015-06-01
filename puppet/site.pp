include 'sbtubuntu'

class { 'java':
  package => 'openjdk-7-jdk'
}

class { 'nodejs':
  version => 'latest'
}

include '::mongodb::server'

include '::apache'

apache::vhost { 'graphite.inventory.com':
  port    => '80',
  docroot => '/opt/graphite/webapp',
  wsgi_application_group      => '%{GLOBAL}',
  wsgi_daemon_process         => 'graphite',
  wsgi_daemon_process_options => {
    processes          => '5',
    threads            => '5',
    display-name       => '%{GROUP}',
    inactivity-timeout => '120',
  },
  wsgi_import_script          => '/opt/graphite/conf/graphite.wsgi',
  wsgi_import_script_options  => {
    process-group     => 'graphite',
    application-group => '%{GLOBAL}'
  },
  wsgi_process_group          => 'graphite',
  wsgi_script_aliases         => {
    '/' => '/opt/graphite/conf/graphite.wsgi'
  },
  headers => [
    'set Access-Control-Allow-Origin "*"',
    'set Access-Control-Allow-Methods "GET, OPTIONS, POST"',
    'set Access-Control-Allow-Headers "origin, authorization, accept"',
  ],
  directories => [{
    path => '/media/'}
  ]
}->
class {'graphite':
  gr_web_server => 'none'
}

class {'statsd':
  backends     => ['./backends/graphite'],
  graphiteHost => 'localhost',
  require      => Class['nodejs']
}
