(ns prometheus.core
  (:require
    [clojure.string :as string])
  (:import
    (clojure.lang IObj)
    (java.io StringWriter)
    (io.prometheus.client.exporter.common TextFormat)
    (io.prometheus.client Counter Histogram Counter$Child Histogram$Child CollectorRegistry Gauge Gauge$Child)))

; more useful set of buckets for microservice APIs than the defaults provided by the Histogram class
(def histogram-buckets (atom [0.001, 0.005, 0.010, 0.020, 0.050, 0.100, 0.200, 0.300, 0.500, 0.750, 1, 5]))

(defn- ^Counter make-request-counter [app-name registry]
  (-> (Counter/build)
      (.namespace app-name)
      (.name "http_requests_total")
      (.labelNames (into-array String ["method" "status" "statusClass" "path"]))
      (.help "A counter of the total number of HTTP requests processed.")
      (.register registry)))

(defn- ^Histogram make-request-histogram [app-name registry]
  (-> (Histogram/build)
      (.buckets (double-array @histogram-buckets))
      (.namespace app-name)
      (.name "http_request_latency_seconds")
      (.labelNames (into-array String ["method" "status" "statusClass" "path"]))
      (.help "A histogram of the response latency for HTTP requests in seconds.")
      (.register registry)))

(defn- ^Counter$Child counter-with-labels [^Counter counter label-array] (.labels counter (into-array String label-array)))

(defn- ^Gauge$Child gauge-with-labels [^Gauge gauge labels] (.labels gauge (into-array String labels)))

(defn- ^Histogram$Child histogram-with-labels [^Histogram histogram label-array] (.labels histogram (into-array String label-array)))

(defn- record-request-metric [counter histogram request-method response-status request-time response-path]
  (let [status-class (str (int (/ response-status 100)) "XX")
        method-label (string/upper-case (name request-method))
        labels [method-label (str response-status) status-class response-path]]
    (-> (histogram-with-labels histogram labels) (.observe request-time))
    (-> (counter-with-labels counter labels) (.inc))))

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

(defn set-gauge
  "Set the value of a registered gauge."
  [store namespace metric value & {:keys [labels] :or {labels []}}]
  (-> (gauge-with-labels (get-in store [:metrics namespace metric]) labels)
      (.set value)))

(defn register-gauge
  "Registers a gauge to the store and returns the new store."
  [store namespace metric help label-names]
  (assoc-in store [:metrics namespace metric] (make-gauge (:registry store) namespace metric help label-names)))

(defn register-counter
  "Registers a counter to the store and returns the new store."
  [store namespace metric help label-names]
  (assoc-in store [:metrics namespace metric] (make-counter (:registry store) namespace metric help label-names)))

(defn increase-counter
  "Increase the value of a registered counter."
  [store namespace metric & {:keys [amount labels] :or {amount 1.0 labels []}}]
  (-> (counter-with-labels (get-in store [:metrics namespace metric]) labels)
      (.inc amount)))

(defn register-histogram
  "Registers a histogram to the store and returns the new store."
  [store namespace metric help label-names buckets]
  (assoc-in store
            [:metrics namespace metric]
            (make-histogram (:registry store) namespace metric help label-names buckets)))

(defn track-observation
  "Track the value of an observation for a registered histogram."
  [store namespace metric value & {:keys [labels] :or {labels []}}]
  (-> (histogram-with-labels (get-in store [:metrics namespace metric]) labels)
      (.observe value)))

(defn with-path
  "Adds the matched compojure route as the :path response metadata attribute"
  [request response]
  (if-let [route (last (:compojure/route request))]
    (if (instance? IObj response)
      (let [response-meta (or (meta response) {})]
        (with-meta response (assoc response-meta :path route)))
      response)
    response))

(defn instrument-handler
  "Ring middleware to record request metrics"
  [handler ^String app-name ^CollectorRegistry registry]
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

(defn dump-metrics
  "Ring handler to expose prometheus metrics using simple client's text format"
  [^CollectorRegistry registry]
  (let [writer (StringWriter.)]
    (TextFormat/write004 writer (.metricFamilySamples registry))
    {:status  200
     :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
     :body    (.toString writer)}))
