(ns enjure.cmd.serve
  (:require [clojure.java.shell :as shell]
            [clojure.java.browse :refer [browse-url]]
            [clojure.string :as string]))

(defn serve-app
  [conf]

  (let [{:keys [project-name]} conf
        us-project-name (string/replace project-name #"-" "_")]
    (load-file (str "src/" us-project-name "/core.clj"))
    (let [main (requiring-resolve (symbol (str project-name ".core/-main")))]
      (main))))
