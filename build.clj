(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.janetacarr/enjure)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" lib version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn- pom-template [version]
  [[:description "A web framework for Clojure"]
   [:url "https://github.com/janetacarr/enjure"]
   [:licenses
    [:license
     [:name "APACHE LICENSE, VERSION 2.0"]
     [:url "https://www.apache.org/licenses/LICENSE-2.0"]]]
   [:developers
    [:developer
     [:name "Janet A. Carr"]]]
   [:scm
    [:url "https://github.com/janetacarr/enjure"]
    [:connection "scm:git:https://github.com/janetacarr/enjure.git"]
    [:developerConnection "scm:git:ssh://git@github.com/janetacarr/enjure.git"]
    [:tag version]]])

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]
                :pom-data (pom-template version)})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (b/install {:class-dir class-dir
              :lib lib
              :version version
              :basis basis
              :jar-file jar-file}))

(defn deploy
  [{:keys [repository] :as opts}]
  (let [dd-deploy (try (requiring-resolve 'deps-deploy.deps-deploy/deploy) (catch Throwable _))]
    (if dd-deploy
      (dd-deploy {:installer :remote
                  :artifact (b/resolve-path jar-file)
                  :repository (or (str repository) "clojars")
                  :pom-file (b/pom-path {:lib lib
                                         :class-dir class-dir})})
      (println "borked"))))
