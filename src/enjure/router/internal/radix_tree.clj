(ns enjure.router.internal.radix-tree
  (:require [clojure.string :as string]))



(defn ->prefixtree
  "Creates a prefix (radix) tree root"
  [data prefix]
  {:children prefix
   :data data})

(defn- ->prefix
  "Returns a map representing an 'edge' in the graph."
  [prefix prefixtree]
  {prefix prefixtree})

(defn tree-root
  []
  (->prefixtree nil
                (->prefix ""
                          (->prefixtree nil nil))))

(defn- found?
  [elements-found s]
  (= elements-found (count s)))

(defn emptyv?
  [v]
  (nil? (peek v)))

(defn search
  "Searches `prefixtree`, radix tree, created with `->prefixtree`, for
  string `s`. Optionally takes `elements-found` for recursion.
  I only recommend you use the 2-arity version"
  ([prefixtree s]
   (search prefixtree s 0 {}))
  ([prefixtree s elements-found path-params]
   (if (= elements-found (count s))
     (assoc (:data prefixtree)
            :path-params path-params)
     ;; TODO: Maybe pull some of this out into functions?
     (if (and (some? (:children prefixtree))
              (< elements-found (count s)))
       (let [prefixes (-> (:children prefixtree)
                          (keys))
             suffix (subs s elements-found)]
         (loop [next-nodes (->> prefixes
                                (filterv #(string/starts-with? suffix %)))]
           (let [next-node-k (peek next-nodes)
                 next-node (get (:children prefixtree) next-node-k)]
             (if-not next-node
               ;; TODO: pluggable coercion ???
               ;; con
               (let [edges (:children prefixtree)
                     pf (peek (filterv #(string/starts-with? % ":") (keys edges)))
                     next-node (get edges pf)
                     param (-> (subs s elements-found)
                               (string/split #"/")
                               (get 0))]
                 (if next-node
                   (search next-node
                           s
                           (+ elements-found (count param))
                           (assoc path-params (keyword (subs pf 1)) param))
                   (when-not (emptyv? next-nodes)
                     (recur (pop next-nodes)))))
               (search next-node s (+ elements-found (count next-node-k)) path-params)))))))))

(def testtree
  (->prefixtree
   {:get (fn [req] {:status 200
                    :body "root"})}
   (->prefix "/user"
             (->prefixtree
              {:get (fn [req] {:status 200 :body "user"})}
              (->prefix "s"
                        (->prefixtree
                         {:get (fn [req] {:status 200
                                          :body "users"})}
                         (->prefix "/:user-id"
                                   (->prefixtree
                                    {:get (fn [req] {:status 200
                                                     :body "user-id"})}
                                    (->prefix "/settings"
                                              (->prefixtree
                                               {:get (fn [req] {:status 200
                                                                :body "settings"})}
                                               nil))))))))))

(defn longest-prefix
  [& strs]
  (->> strs
       (apply mapv (fn [& cs]
                     (when (apply = cs)
                       (first cs))))
       (reduce (fn [acc c]
                 (if (nil? c)
                   (reduced acc)
                   (conj acc c))) [])
       (string/join)))

(defn remove-prefix
  [s prefix]
  (string/join (subvec (vec s) (count prefix))))

(defn insert
  ([prefixtree s data]
   (insert prefixtree s data 0))
  ([prefixtree s data elements-found]
   (assert (string? s) "insert must be string")
   (if (< elements-found (count s))
     (let [prefixes (-> (:children prefixtree)
                        (keys))
           suffix (subs s elements-found)
           next-nodes (->> prefixes
                           (filterv #(string/starts-with? suffix %)))
           in-prefixes? (peek (filterv #(= suffix %) prefixes))]
       (if (or (empty? next-nodes) in-prefixes?)
         (let [existing-prefix (->> prefixes
                                    (mapv (fn [prefix]
                                            [prefix (longest-prefix suffix prefix)]))
                                    (filterv #(-> % (get 1) not-empty))
                                    (peek))
               [prefix lcp] existing-prefix]
           (if (empty? existing-prefix)
             (merge prefixtree {:children (merge (:children prefixtree)
                                                 (->prefix suffix
                                                           (->prefixtree data
                                                                         nil)))})
             ;; TODO: cleanup this monstrosity
             (merge prefixtree
                    {:children (merge
                                (-> (:children prefixtree)
                                    (dissoc prefix))
                                (->prefix
                                 lcp
                                 (->prefixtree
                                  (when (= suffix lcp)
                                    data)
                                  (merge
                                   (->prefix
                                    (remove-prefix prefix lcp)
                                    (->prefixtree
                                     (-> prefixtree :children (get prefix) :data)
                                     (-> prefixtree :children (get prefix) :children)))
                                   (when (not= suffix lcp)
                                     (->prefix
                                      (remove-prefix suffix lcp)
                                      (->prefixtree data nil)))))))})))
         (reduce (fn [prefixtree next-node-k]
                   (let [next-node (get (:children prefixtree) next-node-k)]
                     (merge prefixtree
                            {:children
                             (assoc (:children prefixtree)
                                    next-node-k
                                    (insert next-node
                                            s
                                            data
                                            (+ elements-found (count next-node-k))))})))
                 prefixtree
                 next-nodes)))
     (throw (ex-info "duplicate route entry" {:route s})))))

(defn found-route
  [data req]
  (let [{:keys [get put post delete]} data
        f (or get put post delete)]
    (f req)))

(def long-tree (-> testtree
                   (insert "/owners" {:get (fn [req] {:status 200 :body "owners"})})))
(def owlers-tree (insert long-tree "/owlers" {:get (fn [req] {:status 200 :body "owlers"})}))

(def owners-tree (insert owlers-tree "/users/:user-id/owner" {:get (fn [req] {:status 200 :body "id/owner"})}))

(def root (tree-root))

(def users (-> root
               (insert "/users" {:get (fn [req] {:status 200 :body "user"})})))

(def user (insert users "/user" {:get (fn [req] {:status 200 :body "user"})}))

(def user-id (insert user "/users/:user-id" {:get (fn [req] {:status 200 :body "user-id"})}))

(def owners2 (insert user-id "/users/:user-id/owner" {:get (fn [req] {:status 200 :body "id/owner"})}))

(def admin (insert user-id "/users/admin" {:get (fn [req] {:status 200 :body "static admin"})}))
;; (clojure.pprint/pprint owners-tree)

;; ((:get (search owners-tree "/owners")) {})
;; (search long-tree "/7531")
(def router (-> root
                (insert "/owners" {:get (fn [req] {:status 200 :body "owners"})})
                (insert "/owlers" {:get (fn [req] {:status 200 :body "owlers"})})
                (insert "/users/:user-id/owner" {:get (fn [req] {:status 200 :body "id/owner"})})
                ;;(insert "/users/settings" {:get (fn [req] (fn [req] {:body "users/settings"}))})
                (insert "/user" {:get (fn [req] {:status 200 :body "user"})})
                (insert "/users/:user-id" {:get (fn [req] {:status 200 :body "user-id"})})
                (insert "/users" {:get (fn [req] {:status 200 :body "user"})})))
