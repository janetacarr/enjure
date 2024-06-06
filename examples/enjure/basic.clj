(ns enjure.basic
  (:require [enjure.server :as en]
            [enjure.responses :as resp]
            [enjure.pages.core :refer [defpage]]))

;; GET
(defpage index "/"
  [req]
  "<h1>Welcome!</h1>")

(defpage home "/home"
  [req]
  "<h1>MSN Hotmail</h1>")

(defpage user "/users/:user-id"
  [req]
  (let [{:keys [path-params]} req
        {:keys [user-id]} path-params]
    (format "<h1>Hello, %s</h1>" user-id)))

(defn -main [& args]
  (en/start-server! {:port 8080 :legacy-return-value? false}))
