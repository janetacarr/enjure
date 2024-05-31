(ns enjure.cmd.notes
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.core.reducers :as r]))

(defn find-keywords-in-line [line-number line]
  (when-let [keyword (re-find #"(NOTES|FIXME|HACK|TODO)" line)]
    {:line line-number
     :keyword (get keyword 0)
     :text (str/trim (str/replace line #"(NOTES|FIXME|HACK|TODO):?" ""))}))

(defn find-keywords-in-file [file]
  (with-open [rdr (io/reader file)]
    (doall (keep-indexed (fn [index line]
                           (when-let [note (find-keywords-in-line (inc index) line)]
                             (assoc note :file (.getPath file))))
                         (line-seq rdr)))))

(defn find-keywords-in-dir [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (mapcat find-keywords-in-file)
       (remove nil?)))

(defn file?
  [f]
  (.isFile f))

(defn directory?
  [f]
  (.isDirectory f))

(defn format-note [{:keys [file line keyword text]}]
  (str "  * [" line "] [" keyword "] " text))

(defn group-notes-by-file [notes]
  (reduce (fn [acc {:keys [file] :as note}]
            (update acc file conj note))
          {}
          notes))

(defn print-notes [notes]
  (let [grouped-notes (group-notes-by-file notes)]
    (doseq [[file notes] grouped-notes]
      (println file ":")
      (doseq [note notes]
        (println (format-note note)))
      (println))))

(defn notes
  []
  ;; FIXME: Should we be reading from a particular dir ?
  (print-notes (find-keywords-in-dir "./src")))
