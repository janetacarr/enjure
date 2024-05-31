(ns enjure.internal.frm.core
  "Function-relational mapping setup namespace"
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [environ.core :refer [env]]
            [selmer.parser :as parser]
            [camel-snake-kebab.core :as csk]))

(def database-url (env :database-url))
(def ds (jdbc/get-datasource database-url))
(def ds-opts (jdbc/with-options ds jdbc/unqualified-snake-kebab-opts))

(def ^:dynamic *app-name* "enjure")

(def table-info (jdbc/execute! ds-opts ["
SELECT
    table_schema,
    table_name,
    column_name,
    data_type
FROM
    information_schema.columns
WHERE
    table_schema NOT IN ('pg_catalog', 'information_schema');
"]))

(def table-and-columns
  (reduce-kv (fn [acc table-name columns]
               (conj acc {:table-name table-name
                          :table-schema (-> columns first :table-schema)
                          :columns (mapv (fn [column]
                                           (let [{:keys [column-name data-type]} column]
                                             {(csk/->kebab-case-symbol column-name) data-type}))
                                         columns)}))
             [] (group-by :table-name table-info)))

(defn replace-template
  "replace the symbol template str thingy"
  [template params]
  (clojure.string/replace template #":(\w+)" #(get params (keyword (second %)))))

(defn replace-query
  "replace the symbol template str thingy"
  [template params]
  (clojure.string/replace template #":(\w+)" #(do % "?")))

(defn find-param
  [template]
  (mapv #(keyword (subs % 1)) (re-seq #":\w+" template)))

;; NOTE: Does this need to be a macro?
(defmacro fn-template-body
  [query-template table]
  (let [{:keys [columns]} table
        params (mapv #(-> % keys first) columns)
        args (mapv #(-> % name (str "__") (gensym)) params)
        table-render (assoc table :column-names (mapv str params))
        query (parser/render-file query-template table-render)]
    `(fn [ds# ~@args]
       (jdbc/execute! ds# [~query ~@params]))))

;; Freelancing / Consulting
;; Twitch
;; Open Source (Enjure funding)
;; Course
;; Projects (Scinamalink)

#_(comment
    ;; Filterless
    create-users
    get-all-users
    update-all-users
    delete-all-users

    ;; Single value filters
    get-users-by-id
    update-users-by-id
    delete-users-by-id
    upsert-users-by-id

    get-users-neq-id
    updates-users-neq-id
    delete-users-neq-id
    upsert-users-neq-id

    ;; Multi-value filter
    get-users-in
    update-users-in
    delete-users-in
    upsert-users-in

    ;; Exclusionary multi-value filter
    get-users-not-in
    update-users-not-in
    delete-users-not-in
    upsert-users-not-in

    ;; Joins?

    )

;; The gist:
;; Iterate over template types
;; Create the namespace with create-ns
;; Create the function templates
;; intern the vars with the templated functions
;; add docstrings by modifying metadata with alter-meta!
;; add docstring to the ns created for each interned symbol

(def fn-templates {"create-:table_name" "insert into :table_name (...) values (...);"
                   "get-:table_name-by-id" "select * from :table_name where id = ?;"
                   "get-all-:table_name" "select * from :table_name;"
                   "update-:table_name-by-id" "update :table_name set :column_name = :value where id = ?;"
                   "delete-from-:table_name-by-id" ""})

;; (intern 'enjure.router.internal.frm
;;         'get-api-token-by-id
;;         (fn-template-body "templates/sql/select_by_id.sql"
;;                           {:table-schema "public"
;;                            :table-name "api_tokens"
;;                            :column-name "client_id"
;;                            :data-type "text"} {:id 1}))



(defn db->fs
  [record]
  (let [{:keys [table-schema table-name column-name data-type]} record]
    (doseq [[template-name fn-template] fn-templates]
      (intern (symbol table-schema)
              (symbol (replace-template fn-template {:table_name table-name}))
              ))))

#_(for [[table columns] (group-by #(select-keys % [:table-name :table-schema]) table-info)]
    (hash-map :table-name (:table-name table)
              :table-schema (:table-schema table)
              :columns (mapv (fn [column]
                               (let [{:keys [column-name data-type]} column]
                                 {(csk/->kebab-case-symbol column-name) data-type}))
                             columns)))
