(ns enjure.core
  (:require [enjure.cmd.notes :refer [notes]])
  (:gen-class))

(def help-msg
  "
Usage: enjure <command> <parameters>

Commands:
  notes - Print all NOTES, FIXME, HACK, and TODO in project.
  generate - Create a new controller, page, entity, or migration.
  destory - Delete a new controller, page, entity, or migration.
  migrate - Run the database migrations.
  help - Print this message.
")

;; NOTE: this is a test note
(defn -main [& args]
  (let [[cmd & params] args]
    (case cmd
      "notes" (notes)
      "new" nil ;; Create new enjure app
      "generate" nil ;; create new views/controllers/entities ?
      "destroy" nil
      "migrate" nil
      "help" (println help-msg)
      (println help-msg))))
