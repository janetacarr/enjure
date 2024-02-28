(ns enjure.core
  (:require ))

(defprotocol Mapper
  "for defining data mappers"
  (find [_ id] "Finds the entity")
  (save [this] "saves `this` entity in the database")
  (delete [this] "deletes `this` entity in the database"))


(extend-protocol DataMapper
  clojure.lang.PersistentArrayMap
  (find [_ id])
  (save [this])
  (delete [this])

  clojure.lang.PersistentHashMap
  (find [_ id]
    )
  (save [this])
  (delete [this]))

(defrecord UserMapper []
  DataMapper
  (find [_ id] (next.jdbc/q (or id user-id)))
  (save [this])
  (delete [this]))

(defprotocol ApplicationRecord
  "For defining an active record"
  (find [_ id] "Finds the entity")
  (save [this] "saves `this` entity in the database")
  (delete [this] "deletes `this` entity in the database"))

(defprotocol handler-template
  (->get-handler-template [this entity])
  (->post-handler-template [this entity])
  (->put-handler-template [this entity])
  (->delete-handler-template [this entity]))


;; what about pagination ?
;; Simple? Next token?
;;
(defn ->get-all-handler-template
  "Takes a 0-arty function `all-entity-f` that returns all the entities and
  returns a ring handler for that entity."
  [all-entities-f]
  (fn [req]
    (try
      (let [{:keys [page limit]} (get-in req [param-type])]
        (if (valid-request? params)
          (if-let [results (all-entities-f) ;; protocol
                   ;;(record/find entity id)
                   ] ;; function?????
            (ok results)
            (not-found))
          (bad-request)))
      (catch Exception e
        (log/errorf "Error in request %s" (.getMessage e))
        (internal-server-error)))))

(defn ->get-handler-template
  "Takes entity function `ef` "
  [findf]
  (fn [req]
    (try
      (let [id (get-in req [param-type :id])]
        (if (valid-request? params)
          (if-let [user (findf id) ;; protocol
                   ;;(record/find entity id)
                   ] ;; function?????
            (ok user)
            (not-found))
          (bad-request)))
      (catch Exception e
        (log/errorf "Error in request %s" (.getMessage e))
        (internal-server-error)))))

(defn ->post-handler-template
  [ef]
  (fn [req]
    (try
      (let [params (get-in req [param-type])]
        (if (valid-request? params) ;; request before DB or after ?
          (when-let [user (ef id) ;; protocol
                     ;;(record/find entity id)
                     ] ;; function?????
            (created user))
          (bad-request)))
      (catch Exception e
        (log/debugf "bad request %s" (.getMessage e))
        (bad-request))
      (catch Exception e
        (log/errorf "Error in request %s" (.getMessage e))
        (internal-server-error)))))

(defprotocol Resourcer
  "Factory for producing resources for a model?"
  (entity->resource [this] "Creates CRUD resources for the `entity` returns a vector")
  (entity->xml-resource [this])
  (entity->json-resource [this]))

#_(extend-type UserMapper
    Resourcer
    (entity->resource [this]
      (let [route (str "/" entity "s")]
        [route {:get (fn [req]
                       (let [{:keys [query-params]} req
                             {:keys [id]} query-params]
                         (try
                           (let [user (record/find (->User))]
                             (ok user)))))
                :post (fn [req])
                :put (fn [req])
                :delete (fn [req])}])))

(deftype UserEntity [handler-templater]
  ApplicationRecord
  (find [_ id] "Finds the entity")
  (save [this] "saves `this` entity in the database")
  (delete [this] "deletes `this` entity in the database")

  Resourcer
  (entity->resource [this]
    (let [route (str "/" entity "s")]
      (vector route
              (hash-map :get (->get-handler-template handler-templater this)
                        :post (->post-handler-template handler-templater this)
                        :put (->put-handler-template handler-templater this)
                        :delete (->delete-handler-template handler-templater this)))))

  )

(deftype UserResourcer [router]
  Resourcer
  (entity->resource [this]
    ()))

;; symbols seem best
(defentity users
  {:email (db/index :types/text)
   :password :types/text
   :admin enjure.db.types/boolean})

(defentity users
  (db/otter
   {:email (db/index :types/text)
    :password :types/text
    :admin enjure.db.types/boolean}))

(entity->resource user)

(defn entity->resource
  [entity]
  (let [route (str "/" entity "s")]
    (defmethod router/routes [:get route] )
    (defmethod router/routes [:post route] )))

(defn entity->resource
  [entity]
  )

(defn typical-f
  [input]
  (let [user (-> :users
                 (find 1)
                 (assoc :blah "blah")
                 (save))
        user (-> (->User)
                 (find 1)
                 (assoc :blah "blah")
                 (save))]))

(defn entites-stuff
  [entity]
  ())

(comment
  )
