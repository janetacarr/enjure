(ns enjure.internal.resource)

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
                         (let [k# (enjure.router.core/->key ~uri ~method)]
                           (assoc enjure.router.core/*routing-table*
                                  k#
                                  ~name))))
       #'~name))
