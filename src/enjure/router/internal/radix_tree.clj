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

(defn- emptyv?
  [v]
  (nil? (peek v)))

(defn- longest-prefix
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

;; (defn replace-path-params
;;   "Replaces URI path parameters with values from the map."
;;   [uri params]
;;   (clojure.string/replace uri #":([\w-]+)" #(get params (keyword (second %)))))

(defn- find-prefix-path-params
  [prefixtree s elements-found path-params]
  (try
    (let [edges (:children prefixtree)
          lcp-pf (->> (keys edges)
                      (filterv #(string/includes? % ":"))
                      (filterv #(not (empty? (longest-prefix (subs s elements-found)
                                                             %)))))
          pf (get (if (emptyv? lcp-pf)
                    (filterv #(string/includes? % ":") (keys edges))
                    lcp-pf)
                  0 "")
          paths (set (string/split pf #"/"))
          next-node (get edges pf)

          param-val (some->> (string/split (subs s elements-found) #"/")
                             (filterv #(not (empty? %)))
                             #_(filterv (fn [search-path]
                                          (not (some paths (list search-path))))))

          param-name (some->> (string/split pf #"/")
                              (filterv #(not (empty? %)))
                              #_(filterv #(string/includes? % ":")))

          ;; FIXME: only put valid params into path params
          params-for-prefix (->> param-val
                                 (mapv #(hash-map (keyword (subs %1 1)) %2) param-name)
                                 (apply merge path-params)) ;; {:user-id "5317"}

          ;; params-for-prefix (->> param-val
          ;;                        (mapv vector param-name)
          ;;                        (reduce (fn [pparams [nom val]]
          ;;                                  (if (clojure.string/starts-with? nom ":")
          ;;                                    (assoc pparams (keyword (subs nom 1)) val)
          ;;                                    pparams))
          ;;                                path-params))

          pv (set (vals params-for-prefix))

          last-param (some->> (string/split (subs s elements-found) #"/")
                              (filterv #(some pv (list %)))
                              (peek))

          found (+ (count last-param)
                   (if last-param
                     (string/last-index-of (subs s elements-found)
                                           last-param)
                     0))

          ;; found (if (or (string/starts-with? (subs s elements-found) "/"))
          ;;         (inc (count (replace-path-params (subs s elements-found) params-for-prefix)))
          ;;         (count (replace-path-params (subs s elements-found) params-for-prefix)))
          ]
      {:next-node next-node
       :found found
       :params-for-prefix params-for-prefix})
    (catch Exception e
      (println (.getMessage e)))))

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
               (let [{:keys [next-node found params-for-prefix]}
                     (find-prefix-path-params prefixtree s elements-found path-params)]
                 (if next-node
                   (search next-node s (+ elements-found found) params-for-prefix)
                   (when-not (emptyv? next-nodes)
                     (recur (pop next-nodes)))))
               (search next-node s (+ elements-found (count next-node-k)) path-params)))))))))

(defn- remove-prefix
  [s prefix]
  (string/join (subvec (vec s) (count prefix))))

(defn insert
  "Insert `s` into `prefixtree` (created with ->prefixtree or tree-root above)
  with `data`. Optionally takes integer `element-found` for recursion (Don't use
  this param)."
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
         (let [existing-prefix (some->> prefixes
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
                                 (merge
                                  (->prefixtree
                                   (when (= suffix lcp)
                                     data)
                                   (merge
                                    (let [new-prefix (remove-prefix prefix lcp)]
                                      (->prefix
                                       new-prefix
                                       (->prefixtree
                                        (-> prefixtree :children (get prefix) :data)
                                        (-> prefixtree :children (get prefix) :children))))
                                    (when (not= suffix lcp)
                                      (->prefix
                                       (remove-prefix suffix lcp)
                                       (->prefixtree data nil)))))
                                  (when (empty? (remove-prefix prefix lcp))
                                    (->prefixtree
                                     data
                                     (-> prefixtree :children (get prefix) :children))))))})))
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
