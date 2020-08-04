# prometheus-clj

> :warning: **Archived**: This prometheus-clj project isn't longer maintained by SoundCloud. Check out [iapetos](https://github.com/clj-commons/iapetos) for an actively maintained Clojure wrapper around the official [Prometheus Java Client](https://github.com/prometheus/client_java).

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

## Example with custom metrics

```clojure
(ns prometheus.example
  (:require [prometheus.core :as prometheus]
            [ring.server.standalone :refer [serve]]))

(defonce store (atom nil))

(defn register-metrics [store]
  (->
    store
    (prometheus/register-counter "test" "some_counter" "some test" ["foo"])
    (prometheus/register-gauge "test" "some_gauge" "some test" ["foo"])
    (prometheus/register-histogram "test" "some_histogram" "some test" ["foo"] [0.7 0.8 0.9])))

(defn init! []
  (->> (prometheus/init-defaults)
       (register-metrics)
       (reset! store)))

(defn handler [_]
  (prometheus/increase-counter @store "test" "some_counter" ["bar"] 3)
  (prometheus/set-gauge @store "test" "some_gauge" 101 ["bar"])
  (prometheus/track-observation @store "test" "some_histogram" 0.71 ["bar"])
  (prometheus/dump-metrics (:registry @store)))


(defn -main [&]
  (init!)
  (serve
    (prometheus/instrument-handler handler
                                   "test_app"
                                   (:registry @store))))

```

Run the example server:

    lein run -m prometheus.example

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
