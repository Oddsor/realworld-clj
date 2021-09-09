(ns realworld-clj.routes.services
  (:require
   [realworld-clj.config :refer [env]]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [realworld-clj.middleware.formats :as formats]
   [ring.util.http-response :refer :all]
   [realworld-clj.db.core :as db]
   [buddy.sign.jwt :as jwt]))

(defn un-namespace-keywords [entity]
  (into {} (map (fn [[k v]]
                  [(keyword (name k)) v]))
        entity))

(def user-return {:username string?
                  :email string?
                  :bio string?
                  :image string?
                  :token string?})

(def article {:title string?
              :description string?
              :body string?
              :tagList coll?})

(defn service-routes []
  [["" {:coercion spec-coercion/coercion
        :muuntaja formats/instance
        :swagger {:id ::api
                  :info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}
        :middleware [;; query-params & form-params
                     parameters/parameters-middleware
                     ;; content-negotiation
                     muuntaja/format-negotiate-middleware
                     ;; encoding response body
                     muuntaja/format-response-middleware
                     ;; exception handling
                     coercion/coerce-exceptions-middleware
                     ;; decoding request body
                     muuntaja/format-request-middleware
                     ;; coercing response bodys
                     coercion/coerce-response-middleware
                     ;; coercing request parameters
                     coercion/coerce-request-middleware
                     ;; multipart
                     multipart/multipart-middleware]}

    ["/swagger.json"
     {:no-doc true
      :get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:no-doc true
      :get (swagger-ui/create-swagger-ui-handler
            {:url "/swagger.json"
             :config {:validator-url nil}})}]
    ["/articles"
     {:swagger {:tags ["articles"]}}
     ["" {:get {:summary "Get all articles"
                :responses {200 {:body {:articles coll?}}}
                :handler (fn [req]
                           (let [articles (map un-namespace-keywords
                                               (db/find-articles db/node))]
                             {:status 200
                              :body {:articles articles
                                     :articlesCount (count articles)}}))}
          :post {:summary "Create new article"
                 :parameters {:body {:article article}}
                 :responses {200 {:body {:article article}}
                             400 {:body string?}}
                 :handler (fn [{{{:keys [article]} :body} :parameters}]
                            (if-let [new-article (-> (db/create-new-article db/node article)
                                                     un-namespace-keywords)]
                              {:status 201
                               :body {:article new-article}}
                              {:status 400
                               :body "Failed to create article!"}))}}]]
    ["/users"
     {:swagger {:tags ["users"]}}
     ["" {:get (constantly (ok {:message "Endpoint exists!"}))
          :post {:summary "Create new user"
                 :parameters {:body {:user {:username string?
                                            :password string?
                                            :email string?}}}
                 :responses {201 {:body {:user user-return}}
                             400 {:body string?}}
                 :handler (fn [{{{:keys [user]} :body} :parameters}]
                            (let [user (db/create-user! db/node user)]
                              (if user
                                {:status 201
                                 :body {:user (-> user
                                                  un-namespace-keywords
                                                  (select-keys (keys user-return)))}}
                                {:status 400
                                 :body "Failed to create user"})))}}]
     ["/login" {:post {:summary "Log in as user"
                       :parameters {:body {:user {:email string?
                                                  :password string?}}}
                       :responses {200 {:body {:user user-return}}
                                   400 {:body string?}}
                       :handler (fn [{{{:keys [user]} :body} :parameters
                                      session :session}]
                                  (let [user (db/find-user-by-email-and-password db/node user)]
                                    (if user
                                      (let [jwt-token (jwt/sign {:user (:user/email user)} (:secret env))] 
                                        {:status 200
                                           :body {:user (assoc
                                                         (select-keys (into {} (map (fn [[k v]]
                                                                                      [(keyword (name k)) v]) user))
                                                                      (keys user-return))
                                                         :token jwt-token)}
                                           :session (assoc session :identity (:user/email user))})
                                      {:status 400
                                       :body "Failed to log in"})))}}]]]])
