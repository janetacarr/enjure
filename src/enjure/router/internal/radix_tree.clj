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
               (let [edges (:children prefixtree)
                     pf (get (filterv #(string/includes? % ":") (keys edges)) 0)
                     paths (set (string/split pf #"/"))
                     next-node (get edges pf)

                     param-val (some->> (string/split (subs s elements-found) #"/")
                                        (filterv (fn [search-path]
                                                   (not (some paths (list search-path))))))

                     param-name (some->> (string/split pf #"/")
                                         (filterv #(string/includes? % ":")))

                     params-for-prefix (->> param-val
                                            (mapv #(hash-map (keyword (subs %1 1)) %2) param-name)
                                            (apply merge path-params)) ;; {:user-id "5317"}
                     pv (set (vals params-for-prefix))

                     last-param (some->> (string/split (subs s elements-found) #"/")
                                         (filterv #(some pv (list %)))
                                         (peek))

                     found (+ (count last-param)
                              (string/last-index-of (subs s elements-found)
                                                    last-param))]
                 (if next-node
                   (search next-node
                           s
                           (+ elements-found found)
                           params-for-prefix)
                   (when-not (emptyv? next-nodes)
                     (recur (pop next-nodes)))))
               (search next-node s (+ elements-found (count next-node-k)) path-params)))))))))

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
                                   (let [new-prefix (remove-prefix prefix lcp)]
                                     (when-not (empty? new-prefix)
                                       (->prefix
                                        new-prefix
                                        (->prefixtree
                                         (-> prefixtree :children (get prefix) :data)
                                         (-> prefixtree :children (get prefix) :children)))))
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
