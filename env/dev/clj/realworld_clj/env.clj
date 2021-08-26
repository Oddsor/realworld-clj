(ns realworld-clj.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [realworld-clj.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[realworld-clj started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[realworld-clj has shut down successfully]=-"))
   :middleware wrap-dev})
