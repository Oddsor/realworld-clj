(ns realworld-clj.middleware
  (:require
   [realworld-clj.env :refer [defaults]]
   [realworld-clj.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.auth.accessrules :refer [wrap-access-rules error]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.backends :as backends])
  )

(defn on-error [request response]
  {:status 403
   :headers {}
   :body (str "Access to " (:uri request) " is not authorized")})

(defn authenticated-access
  [request]
  (if (:identity request)
    true
    (error "Only authenticated users allowed")))

(defn any-access
  [request]
  true)

(def token-authentication :user)

(def rules [{:pattern #"^/articles.*"
             :handler #'authenticated-access}
            {:pattern #"^user/.*"
             :handler any-access}
            {:pattern #"^*"
             :handler any-access}])

(defn wrap-auth [handler]
  (let [session-backend (session-backend)
        token-backend (backends/jws {:secret (:secret env)
                                     :token-name "Bearer"
                                     :authfn token-authentication})]
    (-> handler
        (wrap-access-rules {:rules rules :on-error on-error})
        (wrap-authentication session-backend token-backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))
