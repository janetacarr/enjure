(ns enjure.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [enjure.cmd.notes :refer [notes]]
            [enjure.cmd.new :refer [create-project]]
            [enjure.cmd.serve :refer [serve-app]]
            [enjure.cmd.generate :refer [generate-thing]]
            [selmer.parser :as parser])
  (:gen-class))

(def ^:dynamic *project-name* "project-name")

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch java.io.IOException e
      ;;(printf "No enjure.edn file")
      :missing-edn)
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e))
      :missing-edn)))


(def help-msg
  "
Usage: enjure <command> <parameters>

Commands:
  serve - Starts the web server
  notes - Print all NOTES, FIXME, HACK, and TODO in project.
  generate - Create a new controller, page, entity, or migration.
  destory - Delete a new controller, page, entity, or migration.
  migrate - Run the database migrations.
  help - Print this message.
")

(defn -main [& args]
  (let [[cmd & params] args
        conf (load-edn "enjure.edn")]
    (parser/cache-off!)
    (selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))
    (if (and (= conf :missing-edn)
             (not (= cmd "new"))
             (not (= cmd "help")))
      (println (format "enjure %s must be used in enjure project directory" cmd))
      (do (alter-var-root #'*project-name*
                          (fn [_]
                            (:project-name conf)))
          (case cmd
            "notes" (notes)
            "new" (create-project (first params))
            "serve" (serve-app conf)
            "generate" (apply generate-thing conf params)
            "destroy" nil
            "migrate" nil
            "help" (println help-msg)
            (println help-msg))))))
