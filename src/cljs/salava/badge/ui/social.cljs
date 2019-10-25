(ns salava.badge.ui.social
 (:require
  [clojure.string :refer [upper-case replace]]
  [reagent.core :refer [cursor atom create-class force-update]]
  [reagent.session :as session]
  [salava.badge.ui.helper :refer [badge-expired?]]
  [salava.badge.ui.settings :refer [update-settings set-visibility save-settings]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.i18n :refer [t]]
  [salava.core.ui.modal :as mo]
  [salava.core.ui.popover :refer [info]]
  [salava.core.ui.helper :refer [path-for plugin-fun private?]]
  [salava.core.ui.rate-it :as r]
  [salava.core.ui.share :as s]
  [salava.core.time :refer [date-from-unix-time]]
  [salava.social.ui.badge-message-modal :refer [badge-message-link]]
  [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]))

(defn toggle-panel [key x]
  (if (= key @x) (reset! x nil) (reset! x key)))

(defn- init-pending-endorsements [state]
 (when (:owner? @state)
   (ajax/GET
    (path-for (str "/obpv1/badge/endorsement/pending_count/" (:id @state)))
    {:handler (fn [data] (reset! (cursor state [:pending_endorsements_count]) data)
                         (swap! state assoc :notification (+ data @(cursor state [:message_count :new-messages]))))})))

(defn- get-rating [dataatom state]
 (ajax/GET
  (path-for (str "/obpv1/gallery/rating/" (:gallery_id @state)))
  {:handler (fn [data] (reset! (cursor dataatom [:rating]) data))}))

(defn get-pending-requests [dataatom state]
 (ajax/GET
  (path-for (str "/obpv1/badge/endorsement/request/pending/" (:id @state)))
  {:handler (fn [data] (reset! (cursor dataatom [:sent-requests]) data))}))

(defn refresh-social-tab [dataatom state]
 (let [endorsements (cursor state [:user-badge-endorsements])
       view-stats (cursor dataatom [:views-stats])
       recipients (cursor dataatom [:other_recipients])
       rating (cursor dataatom [:rating])
       visibility (cursor state [:badge-settings :visibility])
       sent-requests (cursor dataatom [:sent-requests])]

   #_(update-settings (:id @state) state)

   (ajax/GET
    (path-for (str "/obpv1/social/messages_count/" (:badge_id @state)))
    {:handler (fn [data] (reset! (cursor state [:message_count]) data))
     :finally (fn [] (init-pending-endorsements state))})

   #_(when (= "public" @visibility)
      (get-rating dataatom state))

   (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsement/" (:id @state)))
    {:handler (fn [data]
               (let [pending-endorsements-count (->> data (filter #(= "pending" (:status %))) count)]
                   (reset! endorsements data)
                   (swap! state assoc :pending_endorsements_count pending-endorsements-count)))})

   (ajax/GET
    (path-for (str "/obpv1/badge/stats/views/" (:id @state)))
    {:handler (fn [data] (reset! view-stats data))})

   (ajax/GET
    (path-for (str "/obpv1/gallery/recipients/" (:gallery_id @state)))
    {:handler (fn [data] (reset! recipients data))})

   (get-pending-requests dataatom state)

   #_(ajax/GET)
     (path-for (str "/obpv1/badge/endorsement/request/pending/" (:id @state)))
     {:handler (fn [data] (reset! sent-requests data))}))

(defn save-rating [id state dataatom rating]
 (ajax/POST
   (path-for (str "/obpv1/badge/save_raiting/" id))
   {:params   {:rating  (if (pos? rating) rating nil)}
    :handler (fn [] (get-rating dataatom state))
    :finally (fn [] (update-settings (:id @state) state))}))


(defn badge-congratulations [congratulations state]
 (let [panel-identity :cg
       visible-area-atom (cursor state [:visible-area])
       icon-class (if (= @visible-area-atom :cg) "fa-chevron-circle-down" "fa-chevron-circle-right")]
  [:div.row
   [:div.col-md-12
    [:div.panel.expandable-block {:id "social-tab"}
     [:div.panel-heading
      [:a {:href "#" :on-click #(do (.preventDefault %) (toggle-panel :cg visible-area-atom))}
       [:h3 (str (t :badge/Congratulations) " : " (count congratulations))
         [:i.fa.fa-lg.panel-status-icon {:class icon-class}]]]]
     (when (= @(cursor state [:visible-area]) panel-identity)
       [:div.panel-body.congratulations {:id "endorsebadge"}
        (if (seq congratulations)
          (reduce (fn [r congratulation]
                    (let [{:keys [id first_name last_name profile_picture]} congratulation]
                      (conj r
                       [:div.col-md-12
                        [:div.panel.panel-default.endorsement
                         [:div.panel-body
                           [:div.row.flip.settings-endorsement
                            [:div.col-md-9
                              [:a {:href "#"
                                   :on-click #(mo/open-modal [:profile :view] {:user-id id})}
                               [:img.small-image {:src (profile-picture profile_picture)}]
                               (str first_name " " last_name) ] " " (t :badge/Congratulatedyou)]]]]])))
                  [:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"} ] congratulations)
          [:div.col-md-12 (t :badge/Nocongratulations)])])]]]))

(defn badge-endorsements [data dataatom state]
 (let [panel-identity :endo
       visible-area-atom (cursor state [:visible-area])
       icon-class (if (= @visible-area-atom :endo) "fa-chevron-circle-down" "fa-chevron-circle-right")
       pending-endorsements-count @(cursor state [:pending_endorsements_count])
       request-mode (cursor state [:request-mode])
       hand-icon (if @request-mode "fa-hand-o-down" "fa-hand-o-right")
       sent-requests (cursor dataatom [:sent-requests])]
   (create-class
     {:reagent-render
      (fn []
        [:div.row
         [:div.col-md-12
          [:div.panel.expandable-block {:id "social-tab"}
           [:div.panel-heading
            [:a {:id (str "#" panel-identity) :href "#" :on-click #(do (.preventDefault %) (toggle-panel :endo visible-area-atom))}
             [:h3 (str (t :badge/Userendorsements) " : "  @(cursor state [:badge-settings :user_endorsement_count]))
               [:i.fa.fa-lg.panel-status-icon {:class icon-class}]]
             (when (and (pos? @(cursor state [:notification]) )(pos? pending-endorsements-count) )[:span.badge.social-panel-info  pending-endorsements-count])]]
           (when (= @(cursor state [:visible-area]) panel-identity)
            [:div.panel-body.endorsements
             [:div.col-md-12.request-link {:id "endorsebadge"} [:a {:href "#"
                                                                    :on-click #(mo/open-modal [:badge :requestendorsement] {:state state :reload-fn (fn [] (get-pending-requests dataatom state)) }) #_(if @request-mode (reset! request-mode false) (reset! request-mode true))
                                                                    :id "#request_endorsement"}
                                                                [:span [:i {:class (str "fa fa-fw " hand-icon)}] (t :badge/Requestendorsement)]]]
             (when @(cursor state [:resp-message]) [:div.col-md-12 [:div.alert.alert-success {:style {:margin "15px 0"}} (t :badge/Requestsuccessfullysent)]])

             (when (seq @sent-requests)
               [:div.col-md-12
                [:hr.border]
                (reduce (fn [r u]
                          (let [{:keys [user_id first_name last_name profile_picture]} u]
                           (conj r [profile-link-inline-modal user_id first_name last_name profile_picture])))
                     [:div.col-md-12 {:style {:margin-bottom "20px"}} [:div.row {:style {:margin "5px auto"}} [:span.label.label-primary (t :badge/Alreadysentrequest)]]]
                     @sent-requests)
                [:hr.border]])

             #_(when @request-mode
                 (into [:div.col-md-12]
                   (for [f (plugin-fun (session/get :plugins) "block" "request_endorsement")]
                     [f state])))


             (into [:div.col-md-12 {:style {:margin-top "20px"}}]
               (for [f (plugin-fun (session/get :plugins) "block" "badge_endorsements")]
                 [f (:id @state) {:pending-endorsements-atom (cursor state [:pending_endorsements_count]) :pending-info-atom (cursor state [:notification]) :reload-fn (fn [] (update-settings (:id @state) state))}]))])]]])
      :component-did-update (fn [] (prn "updaete"))})))


(defn badge-views [data]
 [:div.row {:id "social-tab"}
  [:div.col-md-12
   [:div.panel
    [:div.panel-heading
     [:div [:h3 (t :badge/Badgeviews)]]]
    [:div.panel-body {:style {:padding "6px"}}
     [:div#dashboard {:style {:padding-bottom "20px"}}
      [:div.connections-block
        [:div.row.flip
         [:div.total-badges.info-block
          [:div.info
           [:div.text
            [:p.num (get-in data [:views-stats :reg_count] 0)]
            [:p.desc (t :badge/Loggedinusers)]]]]
         [:div.total-badges.info-block
          [:div.info
           [:div.text
            [:p.num (get-in data [:views-stats :anon_count] 0)]
            [:p.desc (t :badge/Anonymoususers)]]]]
         [:div.info-block
          [:div.info
           [:div.text
            [:p.num (date-from-unix-time (* (get-in data [:views-stats :latest_view] 0) 1000))]
            [:p.desc (t :badge/Latestview)]]]]]]]]]]])

(defn badge-recipients [recipients state]
  (let [panel-identity :recipients
        visible-area-atom (cursor state [:visible-area])
        icon-class (if (= @visible-area-atom :recipients) "fa-chevron-circle-down" "fa-chevron-circle-right")
        {:keys [public_users private_user_count all_recipients_count]} @recipients]
    [:div.row
     [:div.col-md-12
      [:div.panel.expandable-block {:id "social-tab"}
       [:div.panel-heading
        [:a {:href "#" :on-click #(do (.preventDefault %)
                                      (toggle-panel panel-identity visible-area-atom))}
         [:h3 (str (t :gallery/recipients) ": " all_recipients_count)
           [:i.fa.fa-lg.panel-status-icon {:class icon-class}]]]]
       (when (= @visible-area-atom panel-identity)
         [:div.panel-body.endorsements
          [:div.col-md-12
           (into [:div]
                 (for [user public_users
                       :let [{:keys [id first_name last_name profile_picture]} user]]
                   (profile-link-inline-modal id first_name last_name profile_picture)))
           (when (> private_user_count 0)
             (if (> (count public_users) 0)
               [:span "... " (t :core/and) " " private_user_count " " (t :core/more)]
               [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]
          (into [:div]
            (for [f (plugin-fun (session/get :plugins) "block" "gallery_badge")]
              [f (:gallery_id @state) (:badge_id @state)]))])]]]))

(defn- badge-rating [dataatom state]
 (let [average_rating (cursor dataatom [:rating :average_rating])
       rating_count (cursor dataatom [:rating :rating_count])
       rating (cursor dataatom [:rating])]
  (create-class
   {:reagent-render
    (fn [dataatom state]
      (if (and (= "public" @(cursor state [:badge-settings :visibility])) (pos? @average_rating))
       ^{:key @average_rating}
        [:div.rating
         [r/rate-it @(cursor dataatom [:rating :average_rating])]
         [:div (if (= @rating_count 1)
                 (str (t :gallery/Ratedby) " " (t :gallery/oneearner))
                 (str (t :gallery/Ratedby) " " @rating_count " " (t :gallery/earners)))]]
        [:div]))
    :component-did-mount (fn [] (get-rating dataatom state))
    :component-did-update (fn [] (get-rating dataatom state))})))

(defn- rate-badge [dataatom state]
 (let [rating_count (cursor dataatom [:rating :rating_count])
       user-rating (cursor state [:badge-settings :rating])]
  (create-class
   {:reagent-render
    (fn []
     [:div.row {:id "social-tab"}
      [:div.col-md-12
       [:div.panel
        [:div.panel-heading
         [:div
          [:h3 (t :badge/Rating)]
          [:div (cond
                 (not (pos? @rating_count)) [:span.label.label-primary (t :badge/Befirstrater)]
                 (not (pos? @user-rating))  [:span.label.label-primary (t :badge/Yettorate)]
                 :else "")]
          [:div.rating
           {:on-click #(do (.preventDefault %)(save-rating (:id @state) state dataatom @user-rating))}
           [r/rate-it @user-rating user-rating]]]]]]])})))
    ;:component-did-mount #(get-rating dataatom state)})))

(defn- message-link [state]
 (when-not (= "private" @(cursor state [:badge-settings :visibility]))
  (into [:div#endorsebadge]
   (for [f (plugin-fun (session/get :plugins) "block" "message_link")]
     [f (:message_count @state) (:badge_id @state)]))))


(defn- visibility-form [dataatom state]
 (fn []
  [:div {:class "form-horizontal"}
   [:div
    [:fieldset {:class "form-group visibility"}
     [:div {:class (str "col-md-12 " (get-in @state [:badge-settings :visibility]))}
      (if-not (private?)
        [:div [:input {:id              "visibility-public"
                       :name            "visibility"
                       :value           "public"
                       :type            "radio"
                       :on-change       #(do
                                           (set-visibility "public" state)
                                           (save-settings state))
                       :default-checked (= "public" (get-in @state [:badge-settings :visibility]))}]
         [:i {:class "fa fa-globe"}]
         [:label {:for "visibility-public"}
          (t :badge/Public)]])
      [:div [:input {:id              "visibility-internal"
                     :name            "visibility"
                     :value           "internal"
                     :type            "radio"
                     :on-change       #(do
                                         (set-visibility "internal" state)
                                         (save-settings state))

                     :default-checked (= "internal" (get-in @state [:badge-settings :visibility]))}]
       [:i {:class "fa fa-group"}]
       [:label {:for "visibility-internal"}
        (t :badge/Shared)]]
      [:div [:input {:id              "visibility-private"
                     :name            "visibility"
                     :value           "private"
                     :type            "radio"
                     :on-change       #(do
                                         (set-visibility "private" state)
                                         (save-settings state))
                     :default-checked (= "private" (get-in @state [:badge-settings :visibility]))}]
       [:i {:class "fa fa-lock"}]
       [:label {:for "visibility-private"}
        (t :badge/Private)]]]]]]))

(defn- share-badge [{:keys [id name image_file issued_on expires_on show_evidence revoked issuer_content_name]} dataatom state]
  (let [expired? (badge-expired? expires_on)
        revoked (pos? revoked)
        visibility (cursor state [:badge-settings :visibility])]
    [:div.row {:id "social-tab"}
     [:div.col-md-12
      [:div.panel
       [:div.panel-heading
        [:div [:h3 (t :badge/Badgevisibility)]]]

       [:div.panel-body {:style {:padding "6px"}}
        (if (and (not expired?) (not revoked))
          [visibility-form dataatom state])
        [:div
         [:hr]
         [s/share-buttons-badge
          (str (session/get :site-url) (path-for (str "/badge/info/" id)))
          name
          (= "public" @visibility)
          true
          (cursor state [:show-link-or-embed])
          image_file
          {:name     name
           :authory  issuer_content_name
           :licence  (str (upper-case (replace (session/get :site-name) #"\s" "")) "-" id)
           :url      (str (session/get :site-url) (path-for (str "/badge/info/" id)))
           :datefrom issued_on
           :dateto   expires_on}]]]]]]))

(defn social-tab [data state]
 (let [{:keys [name image_file obf_url assertion_url congratulations user_endorsement_count settings_fn ]} data
       dataatom (atom {:user-badge-endorsements [] :views-stats {} :other_recipients [] :rating {} :sent-requests []})
       endorsements (cursor state [:user-badge-endorsements])
       view-stats (cursor dataatom [:views-stats])
       recipients (cursor dataatom [:other_recipients])
       visibility (cursor state [:badge-settings :visibility])]

  (refresh-social-tab dataatom state)
  (fn []
   [:div {:id "badge-settings" :class "row flip"}
    [:div {:class "col-md-3 badge-image modal-left"}
     [:img {:src (str "/" image_file) :alt name}]
     [badge-rating dataatom state]
     [message-link state]]
    [:div {:class "col-md-9 settings-content social-tab" :style {:margin-top "20px"}}
     [share-badge data dataatom state]
     (case @visibility
       "public" [:div
                 (when (-> (:views-stats @dataatom) (dissoc :id) (->> (filter second) seq boolean)) [badge-views @dataatom])
                 [rate-badge dataatom state]
                 [badge-recipients recipients state]
                 [badge-endorsements @endorsements dataatom state]
                 [badge-congratulations congratulations state]]
       "internal" [:div
                   (when (-> (:views-stats @dataatom) (dissoc :id) (->> (filter second) seq boolean)) [badge-views @dataatom])
                   [badge-recipients recipients state]
                   [badge-endorsements @endorsements dataatom state]
                   [badge-congratulations congratulations state]]
       [:div {:id "endorsebadge"} [:p [:i.fa.fa-lock] (t :badge/Lockedsocialfeatures)]
             [:p [:b (t :badge/Unlocksocialfeatures)]]])]])))
