(ns enjure.cmd.serve
  (:require [clojure.java.shell :as shell]))

(defn serve-app
  [conf]
  (let [{:keys [project-name]} conf
        {:keys [exit out]} (shell/sh "clj" "-X:serve")]
    (println out)))
