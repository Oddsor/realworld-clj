(ns realworld-clj.db.core
  (:require
   [crux.api :as crux]
   [mount.core :refer [defstate]]
   [realworld-clj.config :refer [env]]
   [buddy.hashers :as h])
  (:import (java.util UUID)))

(defstate node
  :start (doto (crux/start-node (:crux-config env))
           crux/sync)
  :stop (-> node .close))

(defn init-create-if-not-exists-fn []
  (crux/submit-tx node [[:crux.tx/put {:crux.db/id :fn-create-if-not-exists
                                       :crux.db/fn '(fn [ctx new-user]
                                                      (let [db (crux.api/db ctx)
                                                            existing-user (first (crux.api/q db
                                                                                             {:find '[user]
                                                                                              :where [['user :user/username (:user/username new-user)]
                                                                                                      ['user :realworld-clj/type :user]]}))]
                                                        (if existing-user
                                                          false
                                                          [[:crux.tx/put new-user]])))}]]))

(comment (init-create-if-not-exists-fn))


(defn- remove-type-and-crux-id
  [user-doc]
  (dissoc user-doc :realworld-clj/type :crux.db/id))

(defn find-user-by-attribute
  [node attr value]
  (-> (crux/q
        (crux/db node)
        (doto {:find  '[(pull user [*])]
               :where [['user attr 'value]
                       ['user :realworld-clj/type :user]]
               :in    '[attr value]} prn)
        attr value)
      ffirst
      remove-type-and-crux-id))

(defn find-user-by-username
  [node username]
  (find-user-by-attribute node :user/username username))

(defn create-user!
  "e.g.
    (create-user! node {:email \"test@example.com\"
                        :username \"exampleuser\"
                        :password \"12345678\"})"
  [node user]
  (let [new-id (UUID/randomUUID)
        user-with-id #:user{:crux.db/id new-id
                            :realworld-clj/type :user
                            :username (:username user)
                            :password (h/derive (:password user) {:alg :bcrypt+sha512})
                            :email (:email user)
                            :bio (:bio user "")
                            :token (:token user "")
                            :image (:image user "")}]
    (crux/await-tx
     node
     (crux/submit-tx node [[:crux.tx/fn :fn-create-if-not-exists user-with-id]]))
    (crux/entity (crux/db node) new-id)))
