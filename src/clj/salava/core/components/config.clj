(ns salava.core.components.config
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [salava.core.helper :refer [dump plugin-str]]
            [clojure.java.io :as io]))

(defn- get-file [name]
  (if (= "/" (subs name 0 1))
    (io/file name)
    (io/resource name)))

(defn- exists [file]
  (and
    (not (nil? file))
    (.exists (io/as-file file))))

(defn- load-config [base-path plugin]
  (let [config-file (get-file (str base-path "/" (plugin-str plugin) ".edn"))]
    (if (exists config-file)
      (-> config-file slurp read-string))))


(defrecord Config [base-path config]
  component/Lifecycle

  (start [this]
    (let [core-conf (load-config base-path :core)
          config (reduce #(assoc %1 %2 (load-config base-path %2)) {} (:plugins core-conf))]

    (assoc this :config (assoc config :core core-conf))))

  (stop [this]
    (assoc this :config nil)))

(defn create [path]
  (log/info "loading config files from:" path)
  (map->Config {:base-path path}))
