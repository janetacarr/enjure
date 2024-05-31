(ns enjure.responses)

(defn redirect*
  ([dest]
   (redirect* dest :found))
  ([dest kind]
   (let [status (case kind
                  :moved-permanently 301
                  :permanent-redirect 308
                  :found 302
                  :see-other 303
                  :temporary-redirect 307)
         existing? (get @#'enjure.router.core/*reverse-routing-table*
                        dest)
         url (cond
               existing? existing?
               (fn? dest) (dest)
               (string? dest) dest)]
     {:status status
      :body ""
      :headers {"Location" url}}))
  )

(defmacro redirect
  ([dest] `(redirect #'~dest :found))
  ([dest kind] `(redirect* #'~dest ~kind)))
