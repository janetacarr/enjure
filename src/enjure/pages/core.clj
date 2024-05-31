(ns enjure.pages.core
  (:require [clojure.tools.logging :as log]
            [enjure.router.internal.radix-tree :as tree]))

(defn internal-server-error
  []
  {:status 500})

(defn content-type
  [resp mime-type]
  (assoc resp :headers {"content-type" mime-type}))

(defmacro defpage
  "Creates and HTML page from `body`. `body` should
  return an HTML string."
  [name uri argsv & body]
  `(do (defn ~name
         ~argsv
         (try
           (as-> (do ~@body) resp#
             (hash-map :status 200 :body resp#)
             (content-type resp# "text/html"))
           (catch Exception e#
             (log/errorf ~(str "Caught exception in " name ": %s\n")
                         (.getMessage e#))
             (internal-server-error))))
       (alter-var-root #'enjure.router.core/*routing-table*
                       (fn [~'_]
                         (let [existing# (tree/search enjure.router.core/*routing-table* ~uri)]
                           (tree/insert enjure.router.core/*routing-table*
                                        ~uri
                                        (merge existing# {:get ~name})))))
       (alter-var-root #'enjure.router.core/*reverse-routing-table*
                       (fn [~'_]
                         (assoc enjure.router.core/*reverse-routing-table*
                                #'~name
                                ~uri)))
       #'~name))
