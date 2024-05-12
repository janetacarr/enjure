(ns enjure.controllers.core
  (:require [enjure.internal.resource :refer [defresource]]
            [clojure.tools.logging :as log]))

;; TODO parse body/form/etc params
(defmacro defaction
  "Creates a POST resource on `uri`
  named `name`"
  [name uri argsv & body]
  `(defresource ~name :post ~uri
     ~argsv
     ~@body))


(defaction signin "/signin" [req] {:status 200})
