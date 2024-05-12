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

(def large-router (insert-all (tree-root)
                              (filterv (fn [[route data]]
                                         (not= route "/repos/:owner/:repo/statuses/:ref"))
                                       test-data)))

#_(insert large-router "/repos/:owner/:repo/statuses/:ref" {:post (fn [req] {:status 201 :body "Status updated successfully."})})
(run-tests)
