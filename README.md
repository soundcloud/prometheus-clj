# prometheus-clj

A Clojure library designed to provide ring/compojure wrappers for Prometheus [SimpleClient](https://github.com/prometheus/client_java) metrics.

## Installation

#### Leiningen

prometheus-clj is available from [Clojars](https://clojars.org/com.soundcloud/prometheus-clj).

![Clojars Project](http://clojars.org/com.soundcloud/prometheus-clj/latest-version.svg)

## Usage

Require prometheus core.

```clojure
(:require [prometheus.core :as prometheus])
```

Wrap your ring handler so the prometheus client can start collecting metrics about your requests.

```clojure
(prometheus/instrument-handler handler your-app-name your-prometheus-collector-registry)
```

Create a compojure route so that the prometheus server can poll your application for metrics.

```clojure
(GET "/metrics" [] (prometheus/dump-metrics your-prometheus-collector-registry))
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
