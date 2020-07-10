(ns salava.extra.customField.db
 (:require
  [clojure.tools.logging :as log]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]
  [salava.core.util :refer [get-db get-db-1]]
  [clojure.data.json :as json]))

(defqueries "sql/extra/customField/main.sql")

(defn custom-field-value [ctx name user-id]
  (:value (select-field-by-name {:name name :user_id user-id} (get-db-1 ctx))))

(defn update-field [ctx name value user-id]
 (try+
  (insert-custom-field! {:name name :user_id user-id :value value} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn update-custom-fields [ctx user-id custom-fields]
  (doseq [field (keys custom-fields)]
    (update-field ctx (name field) (field custom-fields) user-id)))

(defn organizations [ctx]
 (select-custom-field-organizations {} (get-db ctx)))

(defn update-organization-list [ctx orgs]
 (try+
   (doseq [name orgs]
    (insert-custom-field-org!  {:name name} (get-db ctx)))
   {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn apply-custom-filters-users [ctx filters users]
 (let [users (reduce
              (fn [m v]
               (conj m
                (into {}
                 (for [f (keys filters)
                       :let [val (custom-field-value ctx (name f) (:id v))]]
                   (assoc v f (or val "notset"))))))

              []
              users)]
     (filter #(= filters (select-keys % (keys filters))) users)))

(defn delete-organization! [ctx id]
 (try+
   (delete-custom-field-org! {:id id} (get-db ctx))
   {:status "success"}
   (catch Object _
     (log/error _)
     {:status "error"})))
