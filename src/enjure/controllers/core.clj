(ns enjure.controllers.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn get-clj-files
  "Returns a vector of all the clj files in `directory`"
  [directory]
  (try
    (->> (io/file directory)
         (.listFiles)
         (filterv (fn file? [block] (.isFile block)))
         (mapv (fn get-file-name [file] (.getName file)))
         (filterv (fn clojure-file? [file] (string/ends-with? file ".clj"))))
    (catch Exception e
      (println "[FATAL] While getting controller files: " (Throwable->map e)))))

(defn load-controllers
  "Loads all the clj files in the controllers directory.
  Should only be called once at startup. And maybe during
  repl reload."
  []
  (try
    (doseq [clj-file (get-clj-files "./src/%/controllers")]
      (load-file clj-file))
    (catch Exception e
      (println "[FATAL] While loading controller files:  " (Throwable->map e)))))
