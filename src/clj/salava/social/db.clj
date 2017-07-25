(ns salava.social.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :as util :refer [get-db plugin-fun get-plugins]]
            [salava.admin.helper :as ah]
            [clojure.tools.logging :as log]
            [clojure.string :refer [blank? join]]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/social/queries.sql")

(defn insert-social-event! [ctx data]
  (insert-social-event<! data (get-db ctx)))



(defn get-all-owners [ctx data]
  (let [funs (plugin-fun (get-plugins ctx) "event" "owners")]
    (set (mapcat (fn [f] (try (f ctx data) (catch Throwable _ ()))) funs)))
  )

(defn update-last-viewed [ctx events user_id]
  (let [event_ids (map  #(:event_id %) events)]
    (update-last-checked-user-event-owner! {:user_id user_id :event_ids event_ids} (get-db ctx))
    ))

(defn get-all-events [ctx user_id]
  (let [funs (plugin-fun (get-plugins ctx) "event" "events")
        events (flatten (pmap (fn [f] (try (f ctx user_id) (catch Throwable _ ()))) funs))]
    (sort-by :ctime > (set events))))

(defn insert-event-owners!
  "Creates event owners for event"
  [ctx data]
  (try
    (let [owners (get-all-owners ctx data)  
          query (vec (map #(assoc % :event_id (:event-id data)) owners))]     
     (jdbc/insert-multi! (:connection (get-db ctx)) :social_event_owners query))
   (catch Exception ex
      (log/error "insert-event-owners!: failed to save event owners")
      (log/error (.toString ex)))))


(defn get-all-events-add-viewed [ctx user_id]
  (let [events (get-all-events ctx user_id)]
    (if-not (empty? events)
      (update-last-viewed ctx events user_id))
    events))

(defn messages-viewed
 "Save information about viewing messages."
 [ctx badge_id user_id]
  (try+
   (replace-badge-message-view! {:badge_id badge_id :user_id user_id} (get-db ctx))
   (catch Object _
     "error")))




(defn is-connected? [ctx user_id badge_id]
  (let [id (select-connection-badge {:user_id user_id :badge_id badge_id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))]
    (= badge_id id)))

(defn insert-connection-badge! [ctx user_id badge_id]
  (insert-connect-badge<! {:user_id user_id :badge_id badge_id} (get-db ctx))
  (util/event ctx user_id "follow" badge_id "badge")
  (messages-viewed ctx badge_id user_id))

(defn create-connection-badge! [ctx user_id  badge_id]
  (try+
   (insert-connection-badge! ctx user_id badge_id)
   {:status "success" :connected? (is-connected? ctx user_id badge_id)}
   (catch Object _
     {:status "error" :connected? (is-connected? ctx user_id badge_id)}
     )))
;; STREAM ;;


(defn badge-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item) (:object item)]]
                    (-> current
                        (assoc  key item)
                        ;(assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn admin-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item)]]
                    (-> current
                        (assoc  key item)
                        (assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn badge-message-map
  "returns newest message and count new messages"
  [messages]
  (let [message-helper (fn [current item]
                         (let [key  (:badge_id item)
                               new-messages-count (get-in current [key :new_messages] 0)]
                           (-> current
                               (assoc key item)
                               (assoc-in [key :new_messages] (if (> (:ctime item) (:last_viewed item))
                                                              (inc new-messages-count)
                                                              new-messages-count)))))]
    (reduce message-helper {} (reverse messages))))


(defn filter-badge-message-events [events]
  (filter #(= "message" (:verb %)) events))

(defn filter-own-events [events user_id]
  (filter #(and (= user_id (:subject %)) (= "follow" (:verb %))) events) )



#_(defn get-user-admin-events [ctx user_id]
  (select-admin-events {:user_id user_id} (get-db ctx)))

#_(defn get-user-admin-events-sorted [ctx user_id]
  (let [events (get-user-admin-events ctx user_id)]
   (if (not-empty events)
     (update-last-viewed ctx events user_id))
   events))


(defn hide-user-event! [ctx user_id event_id]
  (try+
   (update-hide-user-event! {:user_id user_id :event_id event_id} (get-db ctx))
   "success"
   (catch Object _
     "error")))



;; MESSAGES ;;

(defn message! [ctx badge_id user_id message]
  (try+
   (if (not (blank? message))
     (let [badge-connection (if (not (is-connected? ctx user_id badge_id))
                              (:status (create-connection-badge! ctx user_id badge_id))
                              "connected")]
       (do
         (insert-badge-message<! {:badge_id badge_id :user_id user_id :message message} (get-db ctx))
         (util/event ctx user_id "message" badge_id "badge")
         {:status "success" :connected? badge-connection}))
     {:status "error" :connected? nil})
   (catch Object _
     {:status "error" :connected? nil})))


(defn get-badge-message-count [ctx badge_id user-id]
  (let [badge-messages-user-id-ctime (select-badge-messages-count {:badge_id badge_id} (get-db ctx))
        last-viewed (select-badge-message-last-view {:badge_id badge_id :user_id user-id} (into {:result-set-fn first :row-fn :mtime} (get-db ctx)))
        new-messages (if last-viewed (filter #(and (not= user-id (:user_id %)) (< last-viewed (:ctime %))) badge-messages-user-id-ctime) ())]
    {:new-messages (count new-messages)
     :all-messages (count badge-messages-user-id-ctime)}))


(defn get-badge-messages-limit [ctx badge_id page_count user_id]
  (let [limit 10
        offset (* limit page_count)
        badge-messages (select-badge-messages-limit {:badge_id badge_id :limit limit :offset offset} (get-db ctx))
        messages-left (- (:all-messages (get-badge-message-count ctx badge_id user_id)) (* limit (+ page_count 1)))]
    (if (= 0  page_count)
      (messages-viewed ctx badge_id user_id))
    {:messages badge-messages
     :messages_left (if (pos? messages-left) messages-left 0)}))


(defn message-owner? [ctx message_id user_id]
(let [message_owner (select-badge-message-owner {:message_id message_id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
 (= user_id message_owner)))

(defn delete-message! [ctx message_id user_id]
  (try+
   (let [message-owner (message-owner? ctx message_id user_id)
         admin (ah/user-admin? ctx user_id)]
     (if (or message-owner admin)
       (update-badge-message-deleted! {:message_id message_id} (get-db ctx))))
   "success"
   (catch Object _
     "error"
     )))


(defn create-connection-badge-by-badge-id! [ctx user_id user_badge_id]
  (let [badge_id (select-badge-id-by-user-badge-id {:user_badge_id user_badge_id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))]
    (try+
     (insert-connection-badge! ctx user_id badge_id)
     (catch Object _
       ))))


(defn delete-connection-badge-by-badge-id! [ctx user_id badge_id]
 (try+
   (delete-connect-badge-by-badge-id! {:user_id user_id :badge_id badge_id} (get-db ctx))
   (catch Object _
     )))

(defn delete-connection-badge! [ctx user_id  badge_id]
  (try+
   (delete-connect-badge! {:user_id user_id :badge_id badge_id} (get-db ctx))
   
   {:status "success" :connected? (is-connected? ctx user_id badge_id)}
   (catch Object _
     
     {:status "error" :connected? (is-connected? ctx user_id badge_id)}
     )))


(defn get-connections-badge [ctx user_id]
  (select-user-connections-badge {:user_id user_id} (get-db ctx)))

(defn get-users-not-verified-emails [ctx user_id]
  (select-user-not-verified-emails {:user_id user_id} (get-db ctx)))

(defn get-user-tips [ctx user_id]
  (let [welcome-tip (= 0 (select-user-badge-count {:user_id user_id} (into {:result-set-fn first :row-fn :count} (get-db ctx))))
        profile-picture-tip (if (not welcome-tip) (nil? (select-user-profile-picture {:user_id user_id} (into {:result-set-fn first :row-fn :profile_picture} (get-db ctx)))) false)]
    {:profile-picture-tip profile-picture-tip
     :welcome-tip welcome-tip
     :not-verified-emails (get-users-not-verified-emails ctx user_id)}
    ))

