(ns prometheus.example
  (:require [prometheus.core :as prometheus]
            [ring.server.standalone :refer [serve]]))

(defonce store (atom nil))

(defn register-metrics [store]
  (->
    store
    (prometheus/register-counter "test" "some_counter" "some test" ["foo"])
    (prometheus/register-gauge "test" "some_gauge" "some test" ["foo"])
    (prometheus/register-histogram "test" "some_histogram" "some test" ["foo"] [0.7 0.8 0.9])))

(defn init! []
  (->> (prometheus/init-defaults)
       (register-metrics)
       (reset! store)))

(defn handler [_]
  (prometheus/increase-counter @store "test" "some_counter" ["bar"] 3)
  (prometheus/set-gauge @store "test" "some_gauge" 101 ["bar"])
  (prometheus/track-observation @store "test" "some_histogram" 0.71 ["bar"])
  (prometheus/dump-metrics (:registry @store)))


(defn -main [&]
  (init!)
  (serve
    (prometheus/instrument-handler handler
                                   "test_app"
                                   (:registry @store))))
