(ns realworld-clj.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[realworld-clj started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[realworld-clj has shut down successfully]=-"))
   :middleware identity})
