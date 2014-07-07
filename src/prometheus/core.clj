(ns prometheus.core
  (:import
    (com.matttproud.accepts Parser)
    (java.util.concurrent TimeUnit)
    (io.prometheus.client Prometheus)
    (io.prometheus.client.metrics Summary Counter)
    (io.prometheus.client.utility.hotspot Hotspot)
    (java.io StringWriter ByteArrayOutputStream ByteArrayInputStream)))

(def ^:private request-counter (atom nil))
(def ^:private request-summary (atom nil))

(defn- make-request-counter [app-name]
  (-> (Counter/newBuilder)
      (.namespace app-name)
      (.name "http_requests_total")
      (.labelNames (into-array String ["method" "status" "path"]))
      (.documentation "A counter of the total number of HTTP requests processed.")
      (.build)))

(defn- make-request-summary [app-name]
  (-> (Summary/newBuilder)
      (.namespace app-name)
      (.name "http_request_durations_milliseconds")
      (.labelNames (into-array String ["method" "status" "path"]))
      (.documentation "A histogram of the response latency for HTTP requests (in milliseconds).")
      (.purgeInterval 2 TimeUnit/MINUTES)
      (.build)))

(defn- record-request-metric [request-method response-status request-time response-path]
  (let [status-label (str (int (/ response-status 100)) "XX")
        method-label (.toUpperCase (name request-method))]
    (-> (.newPartial @request-counter)
        (.labelPair "method" method-label)
        (.labelPair "status" status-label)
        (.labelPair "path" response-path)
        (.apply)
        (.increment))
    (-> (.newPartial @request-summary)
        (.labelPair "method" method-label)
        (.labelPair "status" status-label)
        (.labelPair "path" response-path)
        (.apply)
        (.observe (double request-time)))))

(defn instrument-handler [handler]
  "Ring middleware to record request metrics"
  (fn [request]
    (let [request-method (:request-method request)
          start-time (System/currentTimeMillis)
          response (handler request)
          finish-time (System/currentTimeMillis)
          request-time (- finish-time start-time)
          response-status (get response :status 404)
          response-path (get (meta response) :path "unspecified")]
      (record-request-metric request-method response-status request-time response-path)
      response)))

; adapted from https://github.com/prometheus/client_java/blob/master/utility/servlet/src/main/java/io/prometheus/client/utility/servlet/MetricsServlet.java
(defn metrics-json []
  "Compojure handler to expose prometheus metrics in json format"
  (let [writer (StringWriter.)]
    (Prometheus/defaultDumpJson writer)
    {:status  200
     :headers {"Content-Type" "application/json; schema=\"prometheus/telemetry\"; version=0.0.2"}
     :body    (.toString writer)}))

; adapted from https://github.com/prometheus/client_java/blob/master/utility/servlet/src/main/java/io/prometheus/client/utility/servlet/MetricsServlet.java
(defn metrics-proto []
  "Compojure handler to expose prometheus metrics in protocol buffer binary format"
  (let [output (ByteArrayOutputStream.)]
    (Prometheus/defaultDumpProto output)
    {:status  200
     :headers {"Content-Type" "application/vnd.google.protobuf; proto=\"io.prometheus.client.MetricFamily\"; encoding=\"delimited\""}
     :body    (ByteArrayInputStream. (.toByteArray output))}))

; adapted from https://github.com/prometheus/client_java/blob/master/utility/servlet/src/main/java/io/prometheus/client/utility/servlet/MetricsServlet.java
(defn accepts-proto-response [request]
  (let [accept-specs (Parser/parse (get-in request [:headers "accept"] ""))
        proto-params (some #(when (and (= (.getType %) "application") (= (.getSubtype %) "vnd.google.protobuf")) (.getParams %)) accept-specs)]
    (when proto-params (and (= (get proto-params "proto") "io.prometheus.client.MetricFamily") (= (get proto-params "encoding") "delimited")))))

(defn metrics [request]
  "Compojure handler to expose prometheus metrics in either protocol buffer binary or json format depending on the http request accept header"
  (if (accepts-proto-response request) (metrics-proto) (metrics-json)))

(defn init! [app-name]
  (Prometheus/defaultInitialize)
  (Prometheus/defaultAddPreexpositionHook (Hotspot.))
  (reset! request-counter (make-request-counter app-name))
  (reset! request-summary (make-request-summary app-name)))
