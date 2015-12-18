(ns prometheus.core
  (:require
    [clojure.string :as string])
  (:import
    (clojure.lang IObj)
    (java.io StringWriter)
    (io.prometheus.client.hotspot DefaultExports)
    (io.prometheus.client.exporter.common TextFormat)
    (io.prometheus.client.exporter PushGateway)
    (io.prometheus.client Counter Histogram Counter$Child Histogram$Child CollectorRegistry Gauge Gauge$Child)))

; more useful set of buckets for microservice APIs than the defaults provided by the Histogram class
(def request-latency-histogram-buckets (atom [0.001, 0.005, 0.010, 0.020, 0.050, 0.100, 0.200, 0.300, 0.500, 0.750, 1, 5]))

(defn- ^Counter$Child counter-with-labels [^Counter counter label-array] (.labels counter (into-array String label-array)))

(defn- ^Gauge$Child gauge-with-labels [^Gauge gauge labels] (.labels gauge (into-array String labels)))

(defn- ^Histogram$Child histogram-with-labels [^Histogram histogram label-array] (.labels histogram (into-array String label-array)))

(defn- ^Counter make-counter [^CollectorRegistry registry namespace metric help label-names]
  (-> (Counter/build)
      (.namespace namespace)
      (.name metric)
      (.labelNames (into-array String label-names))
      (.help help)
      (.register registry)))

(defn- ^Gauge make-gauge [^CollectorRegistry registry namespace metric help label-names]
  (-> (Gauge/build)
      (.namespace namespace)
      (.name metric)
      (.labelNames (into-array String label-names))
      (.help help)
      (.register registry)))

(defn- ^Histogram make-histogram [^CollectorRegistry registry namespace metric help label-names buckets]
  (-> (Histogram/build)
      (.buckets (double-array buckets))
      (.namespace namespace)
      (.name metric)
      (.labelNames (into-array String label-names))
      (.help help)
      (.register registry)))

(defn register-counter
  "Registers a counter to the store and returns the new store."
  [store namespace metric help label-names]
  (assoc-in store [:metrics namespace metric] (make-counter (:registry store) namespace metric help label-names)))

(defn register-gauge
  "Registers a gauge to the store and returns the new store."
  [store namespace metric help label-names]
  (assoc-in store [:metrics namespace metric] (make-gauge (:registry store) namespace metric help label-names)))

(defn register-histogram
  "Registers a histogram to the store and returns the new store."
  [store namespace metric help label-names buckets]
  (assoc-in store
            [:metrics namespace metric]
            (make-histogram (:registry store) namespace metric help label-names buckets)))

(defn increase-counter
  "Increase the value of a registered counter."
  ([store namespace metric] (increase-counter store namespace metric [] 1.0))
  ([store namespace metric labels] (increase-counter store namespace metric labels 1.0))
  ([store namespace metric labels amount]
   (-> (counter-with-labels (get-in store [:metrics namespace metric]) labels)
       (.inc amount))))

(defn set-gauge
  "Set the value of a registered gauge."
  ([store namespace metric value] (set-gauge store namespace metric value []))
  ([store namespace metric value labels]
   (-> (gauge-with-labels (get-in store [:metrics namespace metric]) labels)
       (.set value))))

(defn track-observation
  "Track the value of an observation for a registered histogram."
  ([store namespace metric value] (track-observation store namespace metric value []))
  ([store namespace metric value labels]
   (-> (histogram-with-labels (get-in store [:metrics namespace metric]) labels)
       (.observe value))))

(defn init-defaults
  "Initialize the metrics system with defaults."
  []
  (DefaultExports/initialize)
  {:registry (CollectorRegistry/defaultRegistry)})

(defn with-path
  "Adds the matched compojure route as the :path response metadata attribute"
  [request response]
  (if-let [route (last (:compojure/route request))]
    (if (instance? IObj response)
      (let [response-meta (or (meta response) {})]
        (with-meta response (assoc response-meta :path route)))
      response)
    response))

(defn- record-request-metric [metrics-store app-name request-method response-status request-time response-path]
  (let [status-class (str (int (/ response-status 100)) "XX")
        method-label (string/upper-case (name request-method))
        labels [method-label (str response-status) status-class response-path]]
    (track-observation metrics-store app-name "http_request_latency_seconds" request-time labels)
    (increase-counter metrics-store app-name "http_requests_total" labels)))

(defn instrument-handler
  "Ring middleware to record request metrics"
  [handler ^String app-name ^CollectorRegistry registry]
  (let [metrics-store {:registry registry}
        metrics-store (register-counter metrics-store
                                        app-name
                                        "http_requests_total"
                                        "A counter of the total number of HTTP requests processed."
                                        ["method" "status" "statusClass" "path"])
        metrics-store (register-histogram metrics-store
                                          app-name
                                          "http_request_latency_seconds"
                                          "A histogram of the response latency for HTTP requests in seconds."
                                          ["method" "status" "statusClass" "path"]
                                          @request-latency-histogram-buckets)]
    (fn [request]
      (let [request-method (:request-method request)
            start-time (System/currentTimeMillis)
            response (handler request)
            finish-time (System/currentTimeMillis)
            response-status (get response :status 404)
            response-path (get (meta response) :path "unspecified")
            request-time (/ (double (- finish-time start-time)) 1000.0)]
        (record-request-metric metrics-store app-name request-method response-status request-time response-path)
        response))))

(defn dump-metrics
  "Ring handler to expose prometheus metrics using simple client's text format"
  [^CollectorRegistry registry]
  (let [writer (StringWriter.)]
    (TextFormat/write004 writer (.metricFamilySamples registry))
    {:status  200
     :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
     :body    (.toString writer)}))

(defn push-metrics!
  "Push metrics to the Prometheus push gateway."
  [^CollectorRegistry registry ^String hostname ^String job-name]
  (doto (PushGateway. hostname) (.pushAdd registry job-name)))
