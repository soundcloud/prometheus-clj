# prometheus-clj

A Clojure library designed to provide a wrapper to the [Prometheus](https://github.com/prometheus/client_java) java client.

## Installation

#### Leiningen

prometheus-clj is available from [Clojars](https://clojars.org/com.soundcloud/prometheus-clj).

```clojure
[com.soundcloud/prometheus-clj "1.0"]
```

## Usage

Require prometheus core.

```clojure
(:require [prometheus.core :as prometheus])
```

Initialise prometheus client for your application's namespace.

```clojure
(prometheus/init! "application_name")
```

Wrap your ring handler so the prometheus client can start collecting metrics about your requests.

```clojure
(prometheus/instrument-handler handler)
```

Create a compojure route so that the prometheus server can poll your application for metrics.

```clojure
(GET "/metrics" request (prometheus/metrics request))
```

## License

Copyright 2014 SoundCloud, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
