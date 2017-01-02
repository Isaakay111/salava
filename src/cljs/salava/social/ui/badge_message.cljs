(ns salava.social.ui.badge-message
  (:require [reagent.core :refer [atom cursor create-class dom-node props]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim blank?]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid? hyperlink]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.social.ui.follow :as f]
            [salava.core.time :refer [date-from-unix-time]]))

(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/social/messages/" (:badge_content_id @state) "/" (:page_count @state)))
   {:handler (fn [data]
               (swap! state assoc
                      :messages (into (:messages @state) (:messages data))
                      :message ""
                      :page_count (inc (:page_count @state))
                      :messages_left (:messages_left data)))}))


(defn save-message [state]
  (let [{:keys [message user_id badge_content_id]} @state]
    (ajax/POST
     (path-for (str "/obpv1/social/messages/" badge_content_id))
     {:response-format :json
      :keywords? true
      :params {:message message
               :user_id user_id}
      :handler (fn [data]
                 (do
                   (if (= "success" (:connected? data))
                     (do
                       (f/init-data badge_content_id)
                       (swap! state assoc :start-following true))
                     (swap! state assoc :start-following false))
                   (swap! state assoc
                          :messages []
                          :page_count 0)
                   (init-data state)))
      :error-handler (fn [{:keys [status status-text]}]
                       (dump (str status " " status-text)))})))

(defn delete-message [id state]
  (ajax/POST
   (path-for (str "/obpv1/social/delete_message/" id))
   {:response-format :json
    :keywords? true 
    :handler (fn [data]
               (let [filtered-messages (filter #(not (= id (:id %))) (:messages @state))]
                 (swap! state assoc :messages filtered-messages)))
    :error-handler (fn [{:keys [status status-text]}]
                    (dump (str status " " status-text)))}))

(defn delete-message-button [id state]
  (let [delete-clicked (atom nil)]
    (fn []
      [:div.deletemessage
       [:button {:type       "button"
                 :class      "close"
                 :aria-label "OK"
                 :on-click   #(do
                                (reset! delete-clicked (if (= true @delete-clicked) nil true))
                                (.preventDefault %))}
        [:span {:aria-hidden "true"  
                :dangerouslySetInnerHTML {:__html "&times;"}}]]
       (if @delete-clicked
         [:div
          [:div {:class "alert alert-warning"}
           (t :badge/Confirmdelete)]
          [:button {:type  "button"
                    :class "btn btn-primary"
                    :on-click #(reset! delete-clicked nil)}
           (t :badge/Cancel)]
          [:button {:type  "button"
                    :class "btn btn-warning"
                    :on-click     #(delete-message id state)}
           (t :badge/Delete)]])])))

(defn blank-reduce [a-seq]
  (let [str-space (fn [str1 str2]
                    (do
                      (let [current (conj str1 " ")]
                        (conj current str2))))]
      (reduce str-space ()  a-seq)))

(defn search-and-replace-www [text]
  (let [split-words (clojure.string/split text #" ")
        helper (fn [current item]
                 (if (or (re-find #"www." item) (re-find #"^https?://" item) (re-find #"^http?://" item))
                     (conj current (hyperlink item))
                     (conj current (str item))))]
    (blank-reduce (reduce helper () split-words))))

(defn message-list-item [{:keys [message first_name last_name ctime id profile_picture user_id]} state]
  [:div {:class "media message-item" :key id}
   [:div.msgcontent
    [:span {:class "pull-left"}
     [:img {:class "message-profile-img" :src (profile-picture profile_picture)}]]
    [:div {:class "media-body"}
     [:h4 {:class "media-heading"}
      [:a {:href (path-for (str "/user/profile/" user_id)) :target "_blank"} (str first_name " "last_name)]
      [:span.date (date-from-unix-time (* 1000 ctime) "minutes")]]
     (into [:div] (for [ item (clojure.string/split-lines message)]
                    (into [:p.msg] (if (or (re-find #"www." item) (re-find #"https?://" item) (re-find #"http?://" item)) 
                                     (search-and-replace-www item)
                                     item))))]]
   (if (or (=  user_id (:user_id @state)) (= "admin" (:user_role @state)))
     [delete-message-button id state])])

(defn message-list-load-more [state]
  (if (pos? (:messages_left @state))
    [:div {:class "media message-item"}
     [:div {:class "media-body"}
      [:span [:a {:href     "#" 
                  :id    "loadmore"
                  :on-click #(do
                               (init-data state)
                               (.preventDefault %))}
              (str (t :social/Loadmore) " (" (:messages_left @state) " " (t :social/Messagesleft) ")")]]]]))


(defn scroll-bottom []
  (let [div (. js/document getElementById "message-list") ]
    (set! (. div -scrollTop) (. div -scrollHeight))))


(defn message-list [messages state]
  (create-class {:reagent-render (fn [messages]
                                   [:div {:id ""}
                                    (doall
                                     (for [item messages]
                                       (message-list-item item state)))
                                    (message-list-load-more state)])
                 ;:component-did-mount #(scroll-bottom)
                 ;:component-did-update #(scroll-bottom)
                 }))

(defn message-textarea [state]
  (let [message-atom (cursor state [:message])]
    [:div
     [:div {:class "form-group"}
      [:textarea {:class    "form-control"
                  :rows     "5"
                  :value    @message-atom
                  :onChange #(reset! message-atom (.-target.value %))} ]]
     [:div {:class "form-group"}
      [:button {:class    "btn btn-primary"
                :disabled (if (blank? @message-atom) "disabled" "")
                :on-click #(do
                             (save-message state)
                             (.preventDefault %))} 
      (t :social/Postnew)]]]))


(defn refresh-button [state]
  [:a {:href "#" 
       :class "pull-right" 
       :on-click #(do
                    (init-data state)
                    (.preventDefault %))} "Refresh"])

(defn start-following-alert [state]
  (let [{:keys [badge_content_id]} @state]
    [:div {:class (str "alert ""alert-success")}
     [:div.deletemessage
      [:button {:type       "button"
                :class      "close"
                :aria-label "OK"
                :on-click   #(do
                               (swap! state assoc :start-following false)
                               (.preventDefault %))}
       [:span {:aria-hidden "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     (str (t :social/Youstartedfollowbadge) "! ")
     [:a {:href "#"
          :on-click #(do
                       (f/unfollow-ajax-post badge_content_id)
                       (swap! state assoc :start-following false))}
      (t :social/Cancelfollowingbadge)]]))

(defn content [state]
  (let [{:keys [messages start-following]} @state]
    [:div
     (message-textarea state)
     (if start-following
       (start-following-alert state))
     [message-list messages state]]))


(defn badge-message-handler [badge_content_id]
  (let [state (atom {:messages [] 
                     :user_id (session/get-in [:user :id])
                     :user_role (session/get-in [:user :role])
                     :message ""
                     :badge_content_id badge_content_id
                     :show false
                     :page_count 0
                     :messages_left 0
                     :start-following false})]
    (init-data state)
    (fn []
      (content state))))
