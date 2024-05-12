(ns enjure.router.core
  (:require [clojure.tools.logging :as log]))

;; TODO parse & coerce params
(defonce ^:dynamic *routing-table* {})
(defonce ^:dynamic *reverse-routing-table* {})

(defn ->key
  [uri request-method]
  (str uri "_" (name request-method)))

(defn ->router
  [req]
  (let [{:keys [request-method uri]} req
        k (->key uri request-method)
        handling-expressions (get @#'*routing-table* k
                                  "look up radix tree here")]
    (log/infof "%s %s" (clojure.string/upper-case (name request-method)) uri)
    (handling-expressions req)))
