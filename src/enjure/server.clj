(ns enjure.server
  (:require [org.httpkit.server :as hk-server]
            [enjure.router.core :as router]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(defn find-files [base-dir]
  (let [src-dir (io/file base-dir "src")
        dirs (file-seq src-dir)
        file-paths (map #(.getPath %) (filter #(.isFile %) dirs))
        pattern #"src\/[^\/]+\/(pages|controllers)\/[^\/]+\.clj"]
    (filter #(re-find pattern %) file-paths)))

(defn load-files
  []
  (doseq [file-name (find-files ".")]
    (log/debugf "Loading %s" file-name)
    (load-file file-name)))

;; Todo make default opts
;; TODO add production options
;; TODO Should this be managed with mount/compnonet?
(defn start-server!
  [opts]
  (log/info "Loading files")
  (load-files)
  (log/info "Starting web server")
  (let [options (merge opts {:legacy-return-value? false})]
    (loop [server (hk-server/run-server #'router/->router opts)]
      (recur server))))
