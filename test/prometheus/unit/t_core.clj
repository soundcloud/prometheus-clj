(ns prometheus.unit.t_core
  (:require
    [midje.sweet :refer :all]
    [ring.mock.request :as ring]
    [prometheus.core :as prometheus])
  (:import (io.prometheus.client CollectorRegistry)
           (io.prometheus.client.exporter.common TextFormat)))

(def test-response (with-meta {:status 200 :body "ok"} {:path "/test"}))

(facts "metrics export"
  (let [registry (CollectorRegistry.)
        handler (prometheus/instrument-handler (constantly test-response) "test" registry)
        response (handler (ring/request :get "/test"))
        metrics (prometheus/dump-metrics registry)]
    (fact "handler returns delegate's response"
      response => {:status 200 :body "ok"})
    (fact "metrics should be a ring response"
      (:status metrics) => 200
      (get-in metrics [:headers "Content-Type"]) => TextFormat/CONTENT_TYPE_004)))
