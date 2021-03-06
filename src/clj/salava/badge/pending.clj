(ns salava.badge.pending
  (:require [salava.badge.main :refer [fetch-badge badge-issued-and-verified-by-obf user-badges-pending]]
            [salava.factory.db :refer [save-pending-assertions]]
            [salava.user.db :refer [email-exists?]]))

(defn pending-badge-content [ctx req]
  (let [{:keys [user-badge-id email]} (get-in req [:session :pending])
        badge-owner-id (email-exists? ctx email)
        user-in-session? (= badge-owner-id (get-in req [:session :identity :id]))]
    (assoc (->> user-badge-id
                (fetch-badge ctx)
                (badge-issued-and-verified-by-obf ctx))
           :user_exists? badge-owner-id
           :user_in_session? user-in-session?)))

(defn pending-badges [ctx user-id]
  (save-pending-assertions ctx user-id)
  {:pending-badges (user-badges-pending ctx user-id)})
