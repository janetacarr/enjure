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
  "<h1>MSN Hotmail</h1>")

(defpage user "/users/:user-id"
  [req]
  (with-paths [:user-id int?]
    "<h1>MSN Hotmail</h1>"))

(defpage user "/users/:user-id" {:user-id int?}
  [req]
  "<h1>MSN Hotmail</h1>")


;; GET
;; app.views.pages/signin
;; (defpage signin "/signin"
;;   [req])

;; (defn redirect
;;   ([page-or-route]
;;    (redirect page nil))
;;   ([page-or-route kind]
;;    (cond
;;      (fn? page) (page req)
;;      )))

;; (def signin-body
;;   {:email string?
;;    :password string?})
;; POST
;; app.controllers.actions/signin
;; (defaction signin "/signin"
;;   [req]
;;   (let [{:keys [email password]} (:params req)]
;;     (if (check-db email password)
;;       (redirect pages/home :see-other)
;;       (created )
;;       (bad-sigin))))

;; ;; PUT
;; ;; app.controllers.changes/signin
;; (defchange signin "/signin")

;; ;; DELET
;; ;; app.controllers.removals/signin
;; (defremove )


(defn -main [& args]
  (en/start-server! {:port 8080}))
