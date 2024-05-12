(ns enjure.server
  (:require [org.httpkit.server :as hk-server]
            [enjure.router.core :as router]))

;; TODO make default opts
;; TODO add production options
(defn start-server!
  [opts]
  (hk-server/run-server #'router/->router opts))
