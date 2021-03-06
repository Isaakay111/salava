(ns salava.social.ui.dashboard
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.social.ui.stream :as stream]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.modal :as mo]
            [salava.core.i18n :as i18n :refer [t translate-text]]
            [salava.core.time :refer [date-from-unix-time]]
            [reagent-modals.modals :as m]
            [salava.core.ui.helper :refer [path-for plugin-fun not-activated? navigate-to route-path]]
            [salava.badge.ui.my :as my]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.badge.ui.pending :as pb]
            [clojure.string :refer [blank?]]
            [dommy.core :as dommy :refer [has-class?] :refer-macros [sel sel1]]
            [salava.admin.ui.helper :refer [admin?]]))

(defn toggle-visibility [visibility-atom]
  (ajax/POST
   (path-for "/obpv1/user/profile/set_visibility")
   {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
    :handler (fn [new-value]
               (reset! visibility-atom new-value))}))

(defn init-gallery-stats [state]
  (ajax/GET
   (path-for (str "/obpv1/gallery/stats"))
   {:handler (fn [data]
               (swap! state assoc :gallery data))}))

(defn init-dashboard [state]
  (reset! (cursor state [:initializing]) true)
  (ajax/POST
    (path-for (str "/obpv1/spaces/user") true)
    {:handler (fn [data]
                (swap! state assoc :spaces data))
     :finally (fn [] (reset! (cursor state [:initializing]) false))})
  (pb/init-data state)

  (init-gallery-stats state)
  (ajax/GET
   (path-for (str "/obpv1/user/dashboard"))
   {:handler (fn [data]
               (swap! state merge data))})
  (stream/init-data state))



(defn selfie-issue-event [event state]
  (let [{:keys [event_id subject verb image_file message ctime event_id name object first_name last_name profile_picture]}  event]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:profile :view] {:user-id subject})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img.small-image {:src (profile-picture profile_picture) :alt ""}]];(str first_name " " last_name)}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str first_name " " last_name  " " (t :badgeIssuer/Hasissuedselfie) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn follow-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        modal-message (str "messages")]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:gallery :badges] {:badge-id object})
                       (.preventDefault %))
          :style {:text-decoration "none"}}
      [:div {:class "media"}

       [:div.media-left
        [:img {:src (str "/" image_file) :alt name}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading
          (t :social/Youstartedfollowbadge)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn message-event [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:gallery :badges] {:badge-id object
                                                          :show-messages true
                                                          :reload-fn nil})
                       (.preventDefault %))
          :style {:text-decoration "none"}}
      [:div {:class "media"}
       [:div.media-left
        [:img {:src (str "/" image_file) :alt name}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (:message message)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn publish-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object first_name last_name]}  event]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:badge :info] {:badge-id object})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img {:src (str "/" image_file) :alt name}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str (t :social/User) " " first_name " " last_name " " (t :social/Publishedbadge) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn endorse-event-badge [event state]
  (let [{:keys [event_id subject verb image_file message ctime event_id name object first_name last_name profile_picture]}  event
        endorsement (select-keys event [:id :profile_picture :name :first_name :last_name :image_file :content :user_badge_id :issuer_id :issuer_name :endorsee_id :status])]

    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:badge :userendorsement] (atom {:endorsement endorsement}) {:hidden (fn [] (init-dashboard state))})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img.small-image {:src (profile-picture profile_picture) :alt ""}]] ;(str first_name " " last_name)}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str first_name " " last_name " " (t :badge/Hasendorsedyou) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn ext-endorse-event-badge [event state]
  (let [{:keys [event_id subject verb image_file message ctime event_id name object issuer_image issuer_name]}  event
        endorsement (select-keys event [:id :email :issuer_image :name :issuer_name :image_file :content :user_badge_id :issuer_id :status])]

    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:badge :extuserendorsement] (atom {:endorsement endorsement}) {:hidden (fn [] (init-dashboard state))})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img.small-image {:src (profile-picture issuer_image) :alt ""}]] ;(str first_name " " last_name)}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str issuer_name " " (t :badge/Hasendorsedyou) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn endorsement-request-event-badge [event state]
  (let [{:keys [event_id subject verb image_file message ctime event_id name object first_name last_name profile_picture]}  event
        endorsement (select-keys event [:id :profile_picture :first_name :last_name :name :image_file :content :user_badge_id :requester_id :status :description :issuer_name :issuer_content_id :issued_on])]

    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:badge :userendorsement] (atom {:endorsement endorsement :state (atom {:show-delete-dialogue false})}) {:hidden (fn [] (init-dashboard state))})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img.small-image {:src (profile-picture profile_picture) :alt ""}]];(str first_name " " last_name)}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str first_name " " last_name  " " (t :badge/requestsendorsement) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn publish-event-page [event state]
  (let [{:keys [subject verb profile_picture message ctime event_id name object first_name last_name]}  event]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:page :view] {:page-id object})

                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img {:src (profile-picture profile_picture) :alt (str first_name " " last_name)}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str (t :social/User) " " first_name " " last_name " " (t :social/Publishedpage) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn follow-event-user [event state]
  (let [{:keys [subject verb ctime event_id object s_first_name s_last_name o_first_name o_last_name o_id s_id owner o_profile_picture s_profile_picture]}  event
        modal-message (str "messages")]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:profile :view] {:user-id (if (= owner s_id)
                                                                   o_id
                                                                   s_id)})
                       (.preventDefault %))}
      [:div {:class "media"}
       [:div.media-left
        [:img {:src (profile-picture (if (= owner s_id)
                                       o_profile_picture
                                       s_profile_picture))}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (if (= owner s_id)
                               (str (t :social/Youstartedfollowing) " " o_first_name " " o_last_name)
                               (str  s_first_name " " s_last_name " " (t :social/Followsyou)))]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn badge-advert-event [event state]
  (let [{:keys [subject verb image_file ctime event_id name object issuer_content_id issuer_content_name issuer_image]} event
        visibility (if issuer_image "visible" "hidden")]
    [:div {:style {:height "100%"}}
     [:a {:href "#"
          :on-click #(do
                       (.preventDefault %)
                       (mo/open-modal [:application :badge] {:id subject :state state}))}
      [:div {:class "media"}

       [:div.media-left
        [:img {:src (str "/" image_file) :alt name}]]
       [:div.media-body
        [:div.content-text
         [:p.content-heading (str issuer_content_name " " (t :social/Publishedbadge) " " name)]
         [:span.date (date-from-unix-time (* 1000 ctime) "days")]]]]]
     (stream/hide-event event_id state)]))

(defn welcome-block-body [lang]
  (let [language (if-not (blank? lang) lang "en")
        message (first (plugin-fun (session/get :plugins) "block" "welcomeblockboConnectionsdy"))]
    (if message (message lang) "")))

(defn welcome-block [state]
  (let [welcome-tip (get-in @state [:tips :welcome-tip])
        site-name (session/get :site-name)
        user (:user-profile @state)
        user-language (get-in user [:user :language] "en")
        message (welcome-block-body user-language)
        arrow-class (cursor state [:arrow-class])]
    [:div#welcome-block {:class ""}
     [:div.welcome-block.block-content.row (when (session/get-in [:user :current-space]) {:class "welcome-block-ed"})
      (if (blank? message)
        [:div.content
         [:div.row.welcome-message
          [:h1 {:style {:font-size "inherit" :padding "inherit" :margin "inherit"}}
           (if welcome-tip  (str (t :core/Welcometo) " " site-name (t :core/Service)) (str (t :core/Welcometo) " " (session/get :site-name) " " (get-in @state [:user-profile :user :first_name] "") "!"))]]]
        [:div.content
         [:div.row.welcome-message
          [:a {:data-toggle "collapse" :href "#hidden" :role "button" :aria-expanded "false" :aria-controls "hidden" :aria-label "collapse" :on-click #(do
                                                                                                                                                         (.preventDefault %)
                                                                                                                                                         (if (= "true" (dommy/attr (sel1 :#hidden) :aria-expanded))
                                                                                                                                                           (reset! arrow-class "fa-angle-up")
                                                                                                                                                           (reset! arrow-class "fa-angle-down")))} [:i {:class (str "fa icon " @arrow-class) #_(if (dommy/attr (sel1 :#hidden) :aria-expanded) "fa-angle-up" "fa-angle-down")}]]
          (if welcome-tip  (str (t :core/Welcometo) " " site-name (t :core/Service)) (str (t :core/Welcometo) " " (session/get :site-name) " " (get-in @state [:user-profile :user :first_name] "") "!"))
          [:div.collapse {:id "hidden"}
           [:div.hidden-content
            [:p message]]]]])]]))

(defn hide-all-events! [state]
  (ajax/POST
   (path-for (str "/obpv1/social/events/hide_all"))
   {:handler (fn [data] (init-dashboard state))}))

(defn notifications-block [state]
  (let [admin-events (->> (:events @state) (filter #(= (:verb %) "ticket")))
        events (->> (:events @state) (remove #(= (:verb %) "ticket")) (take (if (pos? (count admin-events)) 4 5)))
        tips (:tips @state)]
    [:div {:class "box col-md-4"}
     [:div#box_1
      [:div.col-md-12.block
       [:div.notifications-block.row_1.notifications;.block-content
        [:div.heading_1 [:i.fa.fa-rss.icon]
         [:a {:href "social/stream"} [:span.title (t :social/Stream)]] [:span.badge (count (:events @state))]
         [:span.icon.small]] (if (not-activated?)
                               [:div.content
                                [:div {:style {:font-size "initial"}}
                                 [:p (t :social/Notactivatedbody1)]
                                 [:ul
                                  [:li (t :social/Notactivatedbody2)]
                                  [:li (t :social/Notactivatedbody3)]
                                  [:li (t :social/Notactivatedbody4)]
                                  [:li (t :social/Notactivatedbody5)]
                                  [:li (t :social/Notactivatedbody6)]]]]
                               [:div.content
                                (when (and (admin?) (seq admin-events)) [:div.notification-div.ax_default {:style {:text-align "left" :margin-bottom "20px" :height "35px"}}
                                                                         [:a {:href (path-for "/admin/tickets") :style {:color "unset"}}
                                                                          [:div {:class "media"}
                                                                           [:div.media-left
                                                                            [:i.fa.fa-fw.fa-lightbulb-o]]
                                                                           [:div.media-body
                                                                            [:div.content-text
                                                                             [:p.content-heading (str (count admin-events) " " (t :social/Emailadmintickets))]]]]]])

                                (if (seq (:events @state)) (reduce (fn [r event]
                                                                     (conj r [:div.notification-div.ax_default
                                                                              (cond
                                                                                (and (= "badge" (:type event)) (= "follow" (:verb event))) (follow-event-badge event state)
                                                                                (and (= "user" (:type event)) (= "follow" (:verb event))) (follow-event-user event state)
                                                                                (and (= "badge" (:type event)) (= "publish" (:verb event))) (publish-event-badge event state)
                                                                                (and (= "page" (:type event)) (= "publish" (:verb event))) (publish-event-page event state)
                                                                                (= "advert" (:type event)) (badge-advert-event event state)
                                                                                (= "message" (:verb event)) [message-event event state]
                                                                                (and (= "badge" (:type event)) (= "endorse_badge" (:verb event))) (endorse-event-badge event state)
                                                                                (and (= "badge" (:type event)) (= "request_endorsement" (:verb event))) (endorsement-request-event-badge event state)
                                                                                (and (= "selfie" (:type event)) (= "issue" (:verb event))) (selfie-issue-event event state)
                                                                                (and (= "badge" (:type event)) (= "endorse_badge_ext" (:verb event))) (ext-endorse-event-badge event state)
                                                                                :else "")]))

                                                                   [:div (when (>= (count events) 4) [:div.clear_all [:a {:href "#" :on-click #(hide-all-events! state)} [:span [:i.fa.fa-remove.fa-fw] (t :social/clearall)]]])] events)
                                    [:div.col-md-12 (t :social/Emptystreamheader)])])]]]]))

(defn latest-earnable-badges []
  (let [block (first (plugin-fun (session/get :plugins)  "application" "latestearnablebadges"))]
    (if block [block] [:div ""])))

#_(defn latest-gettable-badges []
    (let [block (first (plugin-fun (session/get :plugins) "block" "latest_gettable_badges"))]
      (if block [block] [:div ""])))

(defn application-button [opt]
  (let [button (first (plugin-fun (session/get :plugins) "application" "button"))]
    (if button
      [button opt]
      [:div ""])))

(defn badges-block [state]
  (fn []
    (let [badges (:badges @state)
          welcome-tip (get-in @state [:tips :welcome-tip])]

      [:div {:class "box col-md-5 "}
       [:div#box_2
        [:div.col-md-12.block
         [:div.badge-block
          [:div.heading_1
           [:i.fa.fa-certificate.icon]
           [:a {:href (path-for "/badge")} [:span.title (t :badge/Badges)]]
           [:span.icon.small]]

          (when-not (not-activated?)  [application-button "button"])
          [:div.content
           [:div.connections-block {:style {:padding-bottom "20px"}}
            [:div.row.flip {:style {:margin-left "-17px"}}
             [:div.total-badges.info-block
              [:div.info
               [:div.text
                [:span.num (get-in @state [:stats :badge_count] 0)]
                [:span.desc (t :badge/Badges)]]]]
             [:div.total-badges.info-block
              [:div.info
               [:div.text
                [:span.num (->> (get-in @state [:stats :badge_views])
                                (reduce #(+ %1 (:reg_count %2) (:anon_count %2)) 0))]
                [:span.desc (t :badge/Badgeviews)]]]]
             [:div.info-block
              [:div.info
               [:div.text
                [:span.num (:published_badges_count @state)]
                [:span.desc (t :gallery/Sharedbadges)]]]]]]

           (when (seq (:pending-badges @state)) [:div.pending
                                                 [:p.header (t :badge/Pendingbadges)]
                                                 ;(if (seq (:pending-badges @state))
                                                 (conj
                                                  (reduce (fn [r badge]
                                                            (conj r [:a {:href "#" :on-click #(navigate-to "/badge")} [:img {:src (str "/" (:image_file badge)) :alt (str (t :badge/Badge) " " (:name badge)) :title (:name badge)}]]))
                                                          [:div] (take 5 (->> (:pending-badges @state)
                                                                              (sort-by :mtime >))))
                                                  (when (> (count (:pending-badges @state)) 5)
                                                    [:span " + " (- (count (:pending-badges @state)) 5)]))])

           (when welcome-tip
             [:div.pending {:style {:padding "10px 0"}} [:p.header (str (t :social/Youdonthaveanyanybadgesyet) ".")]])
           [:div.badge-columns
             ;[:div.pending [:p.header (str (t :social/Youdonthaveanyanybadgesyet) ".")]]
            (when (seq badges)
              [:div.badges
               [:p.header (t :social/Lastestearnedbadges)]
               (reduce (fn [r badge]
                         (conj r [:a {:href "#" :on-click #(do
                                                             (.preventDefault %)
                                                             (mo/open-modal [:badge :info] {:badge-id (:id badge)} {:hidden (fn [] (init-dashboard state))}))} [:img {:src (str "/" (:image_file badge)) :alt (:name badge)}]])) ;:title (:name badge)}]]))
                       [:div {:style {:padding "5px 0"}}] badges)])
            [latest-earnable-badges]
            #_[latest-gettable-badges]]
           (when (> (get-in @state [:stats :badge_count] 0) (:published_badges_count @state))
             [:div#_profiletips {:style {:position "relative" :bottom "1px" :margin-right "10px" :padding-top "20px"}}
              [:div.tip
               [:i.fa.fa-fw.fa-lightbulb-o.tipicon] [:span {:style {:margin "5px"}} (t :badge/Visibilityinfo)]]])]]]]])))

(defn explore-block [state]
  (let [hidden? (session/get-in [:filter-options :hide-explore-block-num] false)]
    [:div.box.col-md-5.col-sm-6.explore
     [:div#box_5 {:class "row_2 explore-block"}
      [:div.col-md-12.block
       [:div.row_2
        [:div.heading_1
         [:i.fa.fa-search.icon]
         [:a {:href (path-for "/gallery")} [:span.title (t :gallery/Gallery)]]
         [:span.icon.small]]

        [:div.content
         [:div.col-md-12
          [:div.row
           [:div.col-sm-4.button-block
            [:div.info-block.badge-connection
             [:a {:href (path-for "/gallery/badges")}
              [:div.info
               [:i.fa.fa-certificate.icon]
               [:div.text
                (when-not hidden? [:span.num (get-in @state [:gallery :badges :all] 0)])
                [:span.desc (t :gallery/Sharedbadges)]]]]]
            (when (pos? (get-in @state [:gallery :badges :since-last-visited] 0))
              [:div.since-last-login [:span.new.no-flip (str "+" (get-in @state [:gallery :badges :since-last-visited] 0))]])]
           [:div.col-sm-4.button-block
            [:div.info-block.page
             [:a {:href (path-for "/gallery/pages")}
              [:div
               [:div.info
                [:i.fa.fa-file.icon]
                [:div.text
                 (when-not hidden? [:span.num (get-in @state [:gallery :pages :all] 0)])
                 [:span.desc (t :gallery/Sharedpages)]]]]]]
            (when (pos? (get-in @state [:gallery :pages :since-last-visited] 0))
              [:div.since-last-login [:span.new.no-flip (str "+" (get-in @state [:gallery :pages :since-last-visited] 0))]])]
           [:div.col-sm-4.button-block
            [:div.info-block
             [:a {:href (path-for "/gallery/profiles")}
              [:div
               [:div.info
                [:i.fa.fa-user.icon]
                [:div.text
                 (when-not hidden? [:span.num (get-in @state [:gallery :profiles :all] 0)])
                 [:span.desc (t :gallery/Sharedprofiles)]]]]]]
            (when (pos? (get-in @state [:gallery :profiles :since-last-visited] 0))
              [:div.since-last-login [:span.new.no-flip (str "+" (get-in @state [:gallery :profiles :since-last-visited] 0))]])]
           [:div.col-sm-4.button-block
            [:div.info-block.map
             [:a {:href (path-for "/gallery/map")}
              [:div
               [:div.info
                [:i.fa.fa-map-marker.icon]
                [:div.text
                 (when-not hidden? [:span.num (get-in @state [:gallery :map :all] 0)])
                 [:span.desc (t :location/Map)]]]]]]
            [:div.since-last-login]]]]]]]]]))

(defn user-connections-stats []
  (let [blocks (first (plugin-fun (session/get :plugins) "block" "userconnectionstats"))]
    (if blocks
      [blocks]
      [:div ""])))

(defn connections-block [state]
  [:div.box.col-md-4.col-sm-6.connections ;{:style {:min-width "45%"}}
   [:div#box_4 {:class "connections-block"}
    [:div.col-md-12.block
     [:div.row_2
      [:div
       [:div.heading_1 [:i.fa.fa-group.icon]
        [:a {:href (path-for "/connections")} [:span.title (t :social/Connections)]]
        [:span.small.icon]]

       [:div.content
        [user-connections-stats]
        [:div.info-block
         [:a {:href "#" #_(str (path-for "/connections/badge")) :on-click #(do
                                                                             (.preventDefault %)
                                                                             (navigate-to "/connections/badge")
                                                                             (session/put! :visible-area :badges))}

          [:div.info
           [:div.text
            [:span.num (get-in @state [:connections :badges])]
            [:span.desc (t :badge/Badges)]]]]]
        [:div.info-block
         [:a {:href "#" #_(str (path-for "/connections/endorsement")) :on-click #(do
                                                                                   (.preventDefault %)
                                                                                   (navigate-to "/connections/endorsement")
                                                                                   (session/put! :visible-area "given"))}

          [:div.info
           [:div.text
            [:span.num (:endorsing @state)]
            [:span.desc (t :badge/Endorsing)]]]]]

        [:div.info-block
         [:a {:href "#" #_(str (path-for "/connections/endorsement")) :on-click #(do
                                                                                   (.preventDefault %)
                                                                                   (navigate-to "/connections/endorsement")
                                                                                   (session/put! :visible-area "received"))}

          [:div.info
           (when (pos? (:pending-endorsements @state)) [:span.badge (:pending-endorsements @state)])
           [:div.text
            [:span.num (:endorsers @state)]
            [:span.desc (t :badge/Endorsers)]]]]]
        (when (pos? (:pending-endorsements-requests @state)) #_(pos? (:endorsement-requests @state))
              [:div.info-block
               [:a {:href "#" #_(str (path-for "/connections/endorsement")) :on-click #(do
                                                                                         (.preventDefault %)
                                                                                         (navigate-to "/connections/endorsement")
                                                                                         (session/put! :visible-area "requests"))}
                [:div.info
                 (when (pos? (:pending-endorsements-requests @state)) [:span.badge "!"])
                 [:div.text
                  [:span.num (:pending-endorsements-requests @state)]
                  [:span.desc (t :badge/Endorsementrequests)]]]]])]]]]]])

(defn profile-tips-block []
  (let [block (first (plugin-fun (session/get :plugins) "block" "profiletips"))]
    (when block [block])))

(defn profile-block [state]
  (let [user (:user-profile @state)
        profile-picture-tip (get-in @state [:tips :profile-picture-tip])
        firstname (get-in user [:user :first_name] "")
        lastname (get-in user [:user :last_name])
        user-name (str firstname " " lastname)]
    [:div.box.col-md-3
     [:div {:id "box_3"}
      [:div.col-md-12.block
       [:div.profile-block.row_1;.block-content
        [:div.heading_1
         [:i.fa.fa-user.icon]
         [:a {:href (str (path-for "/profile/") (get-in user [:user :id]))} [:span.title
                                                                             (t :user/Profile)]]
         [:span.small.icon]]

        [:div.content
         (when-not (not-activated?) [:a.btn.button {:href "#" #_(path-for (str "/profile/" (session/get-in [:user :id])))
                                                    :on-click #(do
                                                                 (.preventDefault %)
                                                                 (navigate-to (str "/profile/" (session/get-in [:user :id])))
                                                                 (session/put! :edit-mode true))} (t :page/Edit)])

         [:div
          [:div.media
           [:div.media-left [:img.img-rounded {:src (profile-picture (get-in user [:user :profile_picture])) :alt ""}]]

           [:div.media-body [:div.name user-name]
            [:div.stats
             #_[:div.text
                [:p.num (:pages_count @state)]
                [:p.desc (t :page/Pages)]]
             #_[:div.text
                [:p.num (:files_count @state)]
                [:p.desc (t :file/Files)]]
             [:div.text
              [:div.profile-visibility
               (when-not (not-activated?)
                 [:a {:href "#" :on-click #(do
                                             (.preventDefault %)
                                             (toggle-visibility (cursor state [:user-profile :user :profile_visibility])))}
                  [:i.fa.icon {:class (case @(cursor state [:user-profile :user :profile_visibility])
                                        "internal" "fa-eye-slash"
                                        "public" "fa-eye"
                                        nil)}]
                  [:span.text (case  @(cursor state [:user-profile :user :profile_visibility])
                                "public" (t :core/Public)
                                "internal"  (t :user/Visibleonlytoregistered)
                                nil)]])]]]]

           (when-not (not-activated?)
             [:div.row {:style {:margin-top "10px"}}
              [profile-tips-block]])]]]]]]]))

(defn quicklinks []
  (let [blocks (plugin-fun (session/get :plugins) "routes" "quicklinks")
        quicklinks  (mapcat #(%) blocks)]
    (reduce (fn [r link]
              (conj r [:a {:href "#" #_(:url link) :on-click #(do (.preventDefault %)
                                                                  (navigate-to (route-path (:url link)))
                                                                  (when (:function link) ((:function link))))} (:title link)]))
            [:div.quicklinks]
            (some->> quicklinks (sort-by :weight <)))))

(defn help-block [state]
  [:div {:class "box col-md-3 col-sm-12"}
   [:div#box_6
    [:div.col-md-12.block
     [:div.row_2.help
      [:div.heading_1
       ;[:i.fa.fa-info-circle.icon]
       [:span.title.help (t :core/Quicklinks)]]
      [:div.content
       [:p  {:style {:font-size "20px" :color "black"}} (t :social/Iwantto)]
       [quicklinks]]]]]])

(defn space-list []
  (into [:div.box.col-md-4.col-sm-12.space-list-block]
    (for [f (plugin-fun (session/get :plugins) "block" "space_list_dashboard")]
      (when (ifn? f) [f]))))

(defn custom-field-init [state]
  (as-> (first (plugin-fun (session/get :plugins) "block" "custom_fields_init")) f
        (when (ifn? f) (f state))))

(defn custom-field-notice [state]
  (into [:div]
    (for [f (plugin-fun (session/get :plugins) "block" "custom_fields_alert")]
      (when (ifn? f) [f state]))))

(defn content [state]
  [:div#dashboard-container
   [m/modal-window]

   [:h1 {:style {:display "none"}} "Dashboard"]
   (if (not-activated?)
     (not-activated-banner))
   [custom-field-notice state]
   [welcome-block state]
   [:div.row
    [:div [layout/space-info-banner]]]
   (if @(cursor state [:initializing])
     [:div
      [:span [:i.fa.fa-cog.fa-spin.fa-lg.fa-fw] (t :core/Loading)]]
     [:div
      [:div.row.flip
       [notifications-block state]
       [badges-block state]
       [profile-block state]]
      [:div.row.flip
       (if (seq @(cursor state [:spaces])) [space-list] [connections-block state])
       [explore-block state]
       [help-block]]
      [:div.row.flip
       (if (seq @(cursor state [:spaces])) [connections-block state] [:div])]])])


(defn new-user-oauth []
  (let [[service _] (-> (clojure.string/split js/window.location.search #"&"))
        service (-> (clojure.string/split service #"=") second)]
    (when (and service _)
      (do
        (.push window.dataLayer (clj->js {:event "virtualPage" :vpv "app/user/register/complete" :registration-method (str service "-oauth")}))
        (.pushState js/history "" "dashboard" (path-for "/social"))))))

(defn handler [site-navi]
  (let [state (atom {:endorsing 0
                     :endorsers 0
                     :badges []
                     :pending-endorsements 0
                     :connections {:badges 0}
                     ;:pages_count 0
                     ;:files_count 0
                     :arrow-class "fa-angle-down"
                     :custom-fields nil
                     :initializing false})]
                     ;:show-custom-field-alert? false})]
    (new-user-oauth)
    (custom-field-init state)
    (init-dashboard state)

    (fn []
     (layout/dashboard site-navi [content state]))))
