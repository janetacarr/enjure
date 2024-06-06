(ns enjure.basic.controllers.echo
  (:require [enjure.controllers.core :refer [defaction
                                             defchange
                                             defremoval]]))

(defaction echo-create "/echo"
  [req]
  (let [{:keys [query-params]} req]
    (println req)
    {:status 200}))

(defchange echo-update "/echo/:id"
  [req]
  (println req)
  {:status 200})

(defremoval echo-delete "/echo/:id"
  [req]
  (println req)
  {:status 200})
