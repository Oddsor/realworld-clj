(ns realworld-clj.db.core
  (:require
    [crux.api :as crux]
    [mount.core :refer [defstate]]
    [realworld-clj.config :refer [env]])
  (:import (java.util UUID)))

(defstate node
  :start (doto (crux/start-node (:crux-config env))
           crux/sync)
  :stop (-> node .close))

(defn- put-user!
  [node user]
  (let [user-doc (assoc user :crux.db/id (:user/id user)
                             :realworld-clj/type :user)]
    (crux/await-tx
      node
      (crux/submit-tx node [[:crux.tx/put user-doc]]))))

(defn- remove-type-and-crux-id
  [user-doc]
  (dissoc user-doc :realworld-clj/type :crux.db/id))

(defn create-user!
  "e.g.
    (create-user! node {:user/email \"test@example.com\"
                        :user/name \"Example user\"})"
  [node user]
  (let [user-with-id (assoc user :user/id (UUID/randomUUID))]
    (put-user! node user-with-id)
    user-with-id))

(defn update-user!
  [node user]
  (put-user! node user))

(defn find-user-by-attribute
  [node attr value]
  (-> (crux/q
        (crux/db node)
        {:find  '[(pull user [*])]
         :where [['user attr 'value]
                 ['user :realworld-clj/type :user]]
         :in    '[attr value]}
        attr value)
      ffirst
      remove-type-and-crux-id))

(defn find-user-by-id
  [node id]
  (find-user-by-attribute node :user/id id))
