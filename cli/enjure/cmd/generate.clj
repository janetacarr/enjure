(ns enjure.cmd.generate
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [selmer.parser :as parser]))

(defn generate-page
  [conf page-name]
  (let [{:keys [project-name]} conf
        us-project-name (string/replace project-name #"-" "_")
        path (str "src/"
                  us-project-name
                  "/pages")
        {:keys [out exit]} (shell/sh "mkdir" "-p" path)]
    (println (format "Generating page %s" page-name))
    (spit (str path "/" page-name ".clj")
          (parser/render-file "clj/enjure/page.cljt"
                              {:page-name page-name
                               :project-name project-name}))))

(defn generate-controller
  [conf controller-name]
  (let [{:keys [project-name]} conf
        us-project-name (string/replace project-name #"-" "_")
        path (str "src/"
                  us-project-name
                  "/controllers")
        {:keys [out exit]} (shell/sh "mkdir" "-p" path)]
    (println (format "Generating controllers %s" controller-name))
    (spit (str path "/" controller-name ".clj")
          (parser/render-file "clj/enjure/controllers.cljt"
                              {:controller-name controller-name
                               :project-name project-name}))))

;; Shit to generate:
;; Pages: enjure generate page thing
;; Controllers (actions, changes, removals)
;; Migrations ?????
;; Entities ????
(defn generate-thing
  [conf & params]
  (let [what (first params)
        the-name (second params)]
    (case what
      "page" (generate-page conf the-name)
      "controllers" (generate-controller conf the-name))))
