(def build-version (or (System/getenv "BUILD_NUMBER") "1.1-SNAPSHOT"))

(defproject com.soundcloud/prometheus-clj build-version
  :description "Clojure wrappers for the Prometheus java client"
  :url "https://github.com/soundcloud/prometheus-clj"

  :license {:name "The Apache Software License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [io.prometheus.client.utility/hotspot "0.0.4"]
                 [com.matttproud.accepts/accepts "0.0.2"]]

  :min-lein-version "2.0.0"

  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins      [[lein-midje "3.0.1"]]}})
