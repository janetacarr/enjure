(ns enjure.router.core
  (:require [clojure.tools.logging :as log]
            [clojure.core.reducers :as r]
            [enjure.router.internal.radix-tree :as tree]))

;; TODO parse & coerce params
(defonce ^:dynamic *routing-table* (tree/tree-root))
(defonce ^:dynamic *reverse-routing-table* {})

(defn ->key
  [uri request-method]
  (str uri "_" (name request-method)))

;; TODO: Should this be here?
(defn parse-query-string
  [query-string]
  (when query-string
    (some->> (clojure.string/split query-string #"&")
             (mapv #(clojure.string/split % #"="))
             (reduce (fn [acc [name val]]
                       (let [k (keyword name)]
                         (if-let [existing (get acc k)]
                           (merge acc {k (if (coll? existing)
                                           (conj existing val)
                                           (vector existing val))})
                           (merge acc {k val})))) {}))))

(defn ->router
  [req]
  (let [{:keys [request-method uri]} req
        {:keys [path-params]
         :as handling-expressions} (tree/search @#'*routing-table* uri)
        responding-expressions (get handling-expressions request-method)
        query-params (parse-query-string (:query-string req))]
    (log/infof "%s %s" (clojure.string/upper-case (name request-method)) uri)
    (if (nil? responding-expressions)
      {:status 404}
      (responding-expressions (merge req {:path-params path-params
                                          :query-params query-params})))))
