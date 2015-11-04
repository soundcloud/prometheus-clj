(ns prometheus.core-test
  (:require
    [clojure.test :refer :all]
    [ring.mock.request :as ring]
    [prometheus.core :as prometheus])
  (:import
    (io.prometheus.client CollectorRegistry)
    (io.prometheus.client.exporter.common TextFormat)))

(defn test-handler [_]
  (with-meta
    {:status 200 :body "ok"}
    {:path "`/test"}))

(deftest dump-metrics
  (let [registry (CollectorRegistry.)
        request (ring/request :get "/test")
        handler (prometheus/instrument-handler test-handler "test" registry)
        response (prometheus/with-path request (handler request))
        metrics (prometheus/dump-metrics registry)]
    (testing "handler returns delegate's response"
      (is (= {:status 200 :body "ok"} response)))
    (testing "metrics should be a ring response"
      (is (= 200 (:status metrics)))
      (is (= TextFormat/CONTENT_TYPE_004
             (get-in metrics [:headers "Content-Type"]))))))
