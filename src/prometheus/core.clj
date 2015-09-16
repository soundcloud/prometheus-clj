(ns prometheus.core
  (:import
    (java.io StringWriter)
    (io.prometheus.client.exporter.common TextFormat)
    (io.prometheus.client Counter Histogram CollectorRegistry)))

; more useful set of buckets for microservice APIs than the defaults provided by the Histogram class
(def histogram-buckets (atom [0.001, 0.005, 0.010, 0.020, 0.050, 0.100, 0.200, 0.300, 0.500, 0.750, 1, 5]))

(defn- make-request-counter [app-name registry]
  (-> (Counter/build)
      (.namespace app-name)
      (.name "http_requests_total")
      (.labelNames (into-array String ["method" "status" "path"]))
      (.help "A counter of the total number of HTTP requests processed.")
      (.register registry)))

(defn- make-request-histogram [app-name registry]
  (-> (Histogram/build)
      (.namespace app-name)
      (.name "http_request_latency_seconds")
      (.labelNames (into-array String ["method" "status" "path"]))
      (.help "A histogram of the response latency for HTTP requests in seconds.")
      (.buckets (double-array @histogram-buckets))
      (.register registry)))

(defn- record-request-metric [counter histogram request-method response-status request-time response-path]
  (let [method-label (.toUpperCase (name request-method))
        status-label (str (int (/ response-status 100)) "XX")
        labels (into-array String [method-label status-label response-path])]
    (-> (.labels counter labels)
        (.inc))
    (-> (.labels histogram labels)
        (.observe request-time))))

(defn instrument-handler [handler ^String app-name ^CollectorRegistry registry]
  "Ring middleware to record request metrics"
  (let [counter (make-request-counter app-name registry)
        histogram (make-request-histogram app-name registry)]
    (fn [request]
      (let [request-method (:request-method request)
            start-time (System/currentTimeMillis)
            response (handler request)
            finish-time (System/currentTimeMillis)
            response-status (get response :status 404)
            response-path (get (meta response) :path "unspecified")
            request-time (/ (double (- finish-time start-time)) 1000.0)]
        (record-request-metric counter histogram request-method response-status request-time response-path)
        response))))

(defn dump-metrics [^CollectorRegistry registry]
  "Compojure handler to expose prometheus metrics using simple client's text format"
  (let [writer (StringWriter.)]
    (TextFormat/write004 writer (.metricFamilySamples registry))
    {:status  200
     :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
     :body    (.toString writer)}))
