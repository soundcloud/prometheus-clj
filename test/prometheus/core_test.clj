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

(deftest custom-metrics
  (let [registry (CollectorRegistry.)
        store {:registry registry}]
    (testing "adds a custom counter"
      (let [store (prometheus/register-counter store "test" "my_custom_counter" "some counter" ["foo"])]
        (prometheus/increase-counter store "test" "my_custom_counter" :labels ["bar"])
        (is (.contains (:body (prometheus/dump-metrics registry)) "test_my_custom_counter"))))
    (testing "adds a custom gauge"
      (let [store (prometheus/register-gauge store "test" "my_custom_gauge" "some gauge" ["foo"])]
        (prometheus/set-gauge store "test" "my_custom_gauge" 101 :labels ["bar"])
        (is (.contains (:body (prometheus/dump-metrics (:registry store)))
                       "test_my_custom_gauge{foo=\"bar\",} 101.0"))))
    (testing "adds a custom histogram"
      (let [store (prometheus/register-histogram
                    store
                    "test"
                    "my_custom_histogram"
                    "some histogram"
                    ["foo"]
                    [10 90 100])]
        (prometheus/track-observation store "test" "my_custom_histogram" 87 :labels ["bar"])
        (is (.contains (:body (prometheus/dump-metrics registry))
                       "test_my_custom_histogram_bucket{foo=\"bar\",le=\"90.0\",} 1.0"))
        (is (.contains (:body (prometheus/dump-metrics registry))
                       "test_my_custom_histogram_bucket{foo=\"bar\",le=\"100.0\",} 1.0"))
        (is (.contains (:body (prometheus/dump-metrics registry))
                       "test_my_custom_histogram_sum{foo=\"bar\",} 87.0"))
        (is (.contains (:body (prometheus/dump-metrics registry))
                       "test_my_custom_histogram_count{foo=\"bar\",} 1.0"))))))
