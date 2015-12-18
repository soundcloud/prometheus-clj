(def build-version (or (System/getenv "BUILD_NUMBER") "2.1-SNAPSHOT"))

(defproject com.soundcloud/prometheus-clj build-version
  :description "Clojure wrappers for the Prometheus java client"
  :url "https://github.com/soundcloud/prometheus-clj"

  :license {:name "The Apache Software License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [io.prometheus/simpleclient "0.0.11"]
                 [io.prometheus/simpleclient_hotspot "0.0.11"]
                 [io.prometheus/simpleclient_common "0.0.11"]
                 [io.prometheus/simpleclient_pushgateway "0.0.11"]]

  :min-lein-version "2.4.3"

  ;:global-vars {*warn-on-reflection* true}

  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [ring-server "0.4.0"]]}})
