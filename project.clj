(def build-version (or (System/getenv "BUILD_NUMBER") "1.1-SNAPSHOT"))

(defproject com.soundcloud/prometheus-clj build-version
  :description "Clojure wrappers for the Prometheus java client"
  :url "https://github.com/soundcloud/prometheus-clj"

  :license {:name "The Apache Software License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [io.prometheus/simpleclient "0.0.10"]
                 [io.prometheus/simpleclient_common "0.0.10"]]

  :min-lein-version "2.4.3"

  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins      [[lein-midje "3.1.3"]]}})
