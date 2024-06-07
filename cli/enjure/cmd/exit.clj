(ns enjure.cmd.exit
  (:import [java.lang Runtime]))

(defn exit
  ([]
   (exit 0))
  ([status]
   (.exit (Runtime/getRuntime) status)))

(defmacro with-exit
  "Runs a body of forms, presumably calling Runtime.exec
  (clojure.java.shell/sh) and calls Runtime.exit when finished
  or throws."
  [& body]
  `(try
     (do ~@body
         (exit))
     (catch Exception e#
       (println "Error: " (.getMessage e#))
       (exit -1))))

(defmacro defproc
  "Defines a function var, wrapping the `body` of
  expressions in `with-exit`."
  [name argsv & body]
  `(defn ~name
     ~argsv
     (with-exit ~@body)))
