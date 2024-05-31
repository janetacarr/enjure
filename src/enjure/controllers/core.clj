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

(defmacro defchange
  "Create a PUT resource on `uri`
  named `name`"
  [name uri argsv & body]
  `(defresource ~name :put ~uri
     ~argsv
     ~@body))

(defmacro defremoval
  "Create a DELETE resource on `uri`
  name `name`"
  [name uri argsv & body]
  `(defresource ~name :delete ~uri
     ~argsv
     ~@body))
