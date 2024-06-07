(ns enjure.cmd.new
  (:require [clojure.string :as string]
            [clojure.java.shell :as shell]
            [selmer.parser :as parser]))

;; TODO: this should NOT be visible to
;; developers.
(def deps
  {:paths ["src" "resources"]
   :deps {'org.clojure/clojure {:mvn/version "1.11.1"}
          'http-kit/http-kit {:mvn/version "2.8.0"}
          'likid_geimfari/secrets {:mvn/version "2.1.1"}
          'selmer/selmer {:mvn/version "1.12.59"}
          'com.github.seancorfield/next.jdbc {:mvn/version "1.3.894"}
          'com.taoensso/timbre {:mvn/version "5.2.1"}
          'environ/environ {:mvn/version "1.2.0"}
          'org.postgresql/postgresql {:mvn/version "42.3.1"}
          'camel-snake-kebab/camel-snake-kebab {:mvn/version "0.4.2"}
          'org.clojure/tools.cli {:mvn/version "1.1.230"}
          'com.janetacarr/enjure {:mvn/version "0.1.0"}}
   :aliases
   {:build {:deps {'slipset/deps-deploy {:mvn/version "0.2.0"}
                   'io.github.clojure/tools.build {:mvn/version "0.9.6"}}
            :ns-default 'build}
    :dev {:extra-deps {'cider/cider-nrepl {:mvn/version "0.26.0"}
                       'com.clojure-goes-fast/clj-java-decompiler {:mvn/version "0.3.1"}}
          :main-opts ["-m" "nrepl.cmdline"
                      "--middleware" "[cider.nrepl/cider-middleware]"]}
    :dev+examples {:extra-paths ["examples" "test" "cli"]
                   :extra-deps {'cider/cider-nrepl {:mvn/version "0.29.0"}
                                'com.clojure-goes-fast/clj-java-decompiler {:mvn/version "0.3.1"}
                                'criterium/criterium {:mvn/version "0.4.0"}}
                   :main-opts ["-m" "nrepl.cmdline"
                               "--middleware" "[cider.nrepl/cider-middleware]"]}
    :cli {:extra-paths ["cli"]
          :main-opts ["-m" "enjure.core"]}
    :test {:extra-paths ["test"]
           :extra-deps {'criterium/criterium {:mvn/version "0.4.6"}
                        'io.github.cognitect-labs/test-runner
                        {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
           :main-opts ["-m" "cognitect.test-runner"]
           :exec-fn 'cognitect.test-runner.api/test}}})

(defn create-project
  [project-name]
  (println (format "Creating new enjure project %s..." project-name))
  (let [conf {:project-name project-name}
        us-project-name (string/replace project-name #"-" "_")
        deps (update deps :aliases #(merge %
                                           {:serve {:main-opts ["-m" (str project-name ".core")]
                                                    :exec-fn (symbol (str project-name ".core/-main"))}}))]
    (shell/sh "mkdir" "-p" (str project-name
                                "/src/"
                                us-project-name))
    (spit (str project-name "/enjure.edn") conf)
    (spit (str project-name "/deps.edn") deps)
    ;; what about handling deps.edn and stuff?

    (spit (str project-name
               "/src/"
               us-project-name
               "/core.clj")
          (parser/render-file "clj/core.cljt" conf))
    (println "Done.")))
