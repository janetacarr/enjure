(ns enjure.router.internal.radix-tree-test
  (:require [enjure.router.internal.radix-tree :refer [tree-root insert search] :as tree]
            [enjure.router.internal.radix-tree-data :refer [test-data]]
            [clojure.test :refer [deftest is testing run-tests]]))

(defn insert-all
  [prefixtree routes]
  (try
    (reduce (fn [tree [route data]]
              (tree/insert tree route data))
            prefixtree
            routes)
    (catch clojure.lang.ExceptionInfo e
      (println e))))

(defn found-route
  [data]
  (let [{:keys [get put post delete]} data
        f (or get put post delete)]
    (when f
      (f {}))))

(def router (-> (tree-root)
                (insert "/owners" {:get (fn [req] {:status 200 :body "owners"})})
                (insert "/owlers" {:get (fn [req] {:status 200 :body "owlers"})})
                (insert "/users/:user-id/owner" {:get (fn [req] {:status 200 :body "id/owner"})})
                (insert "/users/settings" {:get (fn [req] {:status 200 :body "users/settings"})})
                (insert "/user" {:get (fn [req] {:status 200 :body "user"})})
                (insert "/users/:user-id" {:get (fn [req] {:status 200 :body "user-id"})})
                (insert "/users" {:get (fn [req] {:status 200 :body "user"})})
                (insert
                 "/repos/:owner/:repo/statuses/:ref" {:get (fn [req] {:status 200
                                                                      :body "ref?"})})))

(deftest simple-router-test
  (testing "the basic route structure"
    (let []
      (is (= (found-route (search router "/owners")) {:status 200 :body "owners"}))
      (is (= (found-route (search router "/owlers")) {:status 200 :body "owlers"}))
      (is (= (found-route (search router "/users/5317/owner")) {:status 200 :body "id/owner"}))
      (is (= (:path-params (search router "/users/5317/owner")) {:user-id "5317"}))
      (is (= (found-route (search router "/users/settings")) {:status 200 :body "users/settings"}))
      (is (= (found-route (search router "/user")) {:status 200 :body "user"}))
      (is (= (found-route (search router "/users/5315")) {:status 200 :body "user-id"}))
      (is (= (:path-params (search router "/users/5315")) {:user-id "5315"}))
      (is (= (found-route (search router "/users")) {:status 200 :body "user"})))))

(defn replace-path-params
  "Replaces URI path parameters with values from the map."
  [uri params]
  (clojure.string/replace uri #":([\w-]+)" #(get params (keyword (second %)))))

(deftest large-router-test
  (testing "a complex router structure"
    (let [large-router (insert-all (tree-root) test-data)
          {get-handler :get
           post-handler :post
           put-handler :put
           delete-handler :delete
           path-params :path-params} (search large-router "/repos/janetacarr/enjure/statuses/135")
          path-params (select-keys path-params [:owner :repo :ref])]
      (is (= (post-handler {})
             {:status 201 :body "Status updated successfully."}))
      (is (= (get-handler {})
             {:status 200 :body "Statuses fetched successfully."}))
      (is (= path-params
             {:owner "janetacarr" :repo "enjure" :ref "135"})))))

(def ^:dynamic *debug-tests* false)
(defn- handle
  [k m]
  (when-let [f (get m k)]
    (when *debug-tests*
      (println (f {})))
    (f {})))

#_(defn- debug
    [uri data large-router uri]
    (println)
    (println uri)
    (println data " <> " (search large-router uri))
    (println))

(deftest all-routes-test
  (testing "all the routes"
    (let [large-router (insert-all (tree-root) test-data)
          params {:id "123"
                  :user "janedoe"
                  :client_id "client123"
                  :access_token "token123456"
                  :owner "ownername"
                  :repository "enjure"
                  :repo "repository"
                  :org "orgname"
                  :assignee "assignee123"
                  :number "42"
                  :sha "1a2b3c4d"
                  :name "name123"
                  :branch "main"
                  :ref "main" ;;"refs/heads/main" ;; NOTE: Should (sub-)paths in params be allowed?
                  :state "open"
                  :keyword "search"
                  :email "email@example.com"
                  :target_user "targetuser123"}]
      (doseq [[route data] test-data]
        (let [uri (replace-path-params route params)
              {:keys [path-params]} (search large-router uri)
              params-decl (->> (clojure.string/split route #"/")
                               (filterv #(clojure.string/starts-with? % ":"))
                               (mapv #(keyword (subs % 1))))]
          (is (= (handle :get data) (handle :get (search large-router uri))))
          (is (= (handle :put data)) (handle :put (search large-router uri)))
          (is (= (handle :post data)) (handle :post (search large-router uri)))
          (is (= (handle :delete data)) (handle :delete (search large-router uri)))
          (is (= (select-keys params params-decl) (select-keys path-params params-decl))))))))

#_(run-tests)

#_(let [large-router (insert-all (tree-root) test-data)
        params {:id "123"
                :user "janedoe"
                :client_id "client123"
                :access_token "token123456"
                :owner "ownername"
                :repository "enjure"
                :repo "repository"
                :org "orgname"
                :assignee "assignee123"
                :number "42"
                :sha "1a2b3c4d"
                :name "name123"
                :branch "main"
                :ref "main" ;;"refs/heads/main" ;; NOTE: Should (sub-)paths in params be allowed?
                :state "open"
                :keyword "search"
                :email "email@example.com"
                :target_user "targetuser123"}
        test-data (mapv (fn [[route data]]
                          (vector (replace-path-params route params) data))
                        test-data)]
    (bench (mapv (fn [[uri data]]
                   (let [{get-handler :get
                          post-handler :post
                          put-handler :put
                          delete-handler :delete
                          path-params :path-params} (search large-router uri)]
                     (cond
                       get-handler (get-handler {})
                       put-handler (put-handler {})
                       post-handler (post-handler {})
                       delete-handler (delete-handler {}))))
                 test-data)))
