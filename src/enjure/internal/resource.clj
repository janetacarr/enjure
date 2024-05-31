(ns enjure.internal.resource
  (:require [enjure.router.internal.radix-tree :as tree]
            [enjure.router.core :as router]
            [clojure.tools.logging :as log]))

(defmacro defresource
  [name method uri argsv & body]
  `(do (defn ~name
         ~argsv
         (try
           (do ~@body)
           (catch Exception e#
             (log/errorf ~(str "Caught exception in " name ": %s\n")
                         (.getMessage e#))
             {:status 500})))
       (alter-var-root #'enjure.router.core/*routing-table*
                       (fn [~'_]
                         (let [existing# (tree/search enjure.router.core/*routing-table* ~uri)]
                           (tree/insert enjure.router.core/*routing-table*
                                        ~uri
                                        (merge existing# {~method ~name})))))
       #'~name))
