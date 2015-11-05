(ns prometheus.core-test
  (:require
    [clojure.test :refer :all]
    [ring.mock.request :as ring]
    [prometheus.core :as prometheus])
  (:import
    (io.prometheus.client CollectorRegistry)
    (io.prometheus.client.exporter.common TextFormat)))

(defn test-handler [request]
  (prometheus/with-path request {:status 200 :body "ok"}))

(deftest dump-metrics
  (let [registry (CollectorRegistry.)
        handler (prometheus/instrument-handler test-handler "test" registry)
        response (handler (assoc (ring/request :get "/test-path") :compojure/route ["GET" "/test-path"]))
        metrics (prometheus/dump-metrics registry)]
    (testing "handler returns delegate's response"
      (is (= {:status 200 :body "ok"} response)))
    (testing "metrics should be a ring response"
      (is (= 200 (:status metrics)))
      (is (= TextFormat/CONTENT_TYPE_004 (get-in metrics [:headers "Content-Type"])))
      (is (.contains (:body metrics) "http_request_latency_seconds"))
      (is (.contains (:body metrics) "http_requests_total"))
      (is (.contains (:body metrics) "/test-path")))))
