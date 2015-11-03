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
        handler (prometheus/instrument-handler test-handler "test" registry)
        response (handler (ring/request :get "/test"))
        metrics (prometheus/dump-metrics registry)]
    (testing "handler returns delegate's response"
      (is (= response {:status 200 :body "ok"})))
    (testing "metrics should be a ring response"
      (is (= 200 (:status metrics)))
      (is (= TextFormat/CONTENT_TYPE_004
             (get-in metrics [:headers "Content-Type"]))))))
