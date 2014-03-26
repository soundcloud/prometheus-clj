(def feature-version "1.0")
(def build-version (or (System/getenv "BUILD_NUMBER") "local"))
(def release-version (str feature-version "." build-version))

(defproject prometheus feature-version
  :description "Clojure wrappers for the Prometheus java client"
  :url "https://github.com/soundcloud/prometheus-clj"

  :license {:name "The Apache Software License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [io.prometheus.client.utility/hotspot "0.0.2"]
                 [com.matttproud.accepts/accepts "0.0.2"]]

  :min-lein-version "2.0.0"

  :manifest {"Implementation-Version" ~release-version}

  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins      [[lein-midje "3.0.1"]]}})
