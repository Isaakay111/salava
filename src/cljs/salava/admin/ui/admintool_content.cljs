(ns salava.admin.ui.admintool-content
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.badge.ui.helper :as bh]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.user.ui.helper :refer [profile-picture profile-link-inline]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? admin? message-form email-select status-handler]]))




(defn delete-item [state visible_area item_owner]
  (let [{:keys [item_id mail item_owner_id gallery-state init-data name item_type]} @state
         mail (cursor state [:mail])]
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "delete-item" @visible_area) "" "delete-item"))) :class (if (= "delete-item" @visible_area) "opened" "")} (t :core/Delete) ]]
     (if (= @visible_area "delete-item")
       [:div.col-md-12
        
        (str (t :admin/Deletemessage1) " " item_owner " " name " "(t (keyword (str "admin/" item_type))) "?")
        [:br]
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/delete_"item_type"/" item_id))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          {:subject (:subject @mail)
                                                     :message (:message @mail)
                                                     :user-id  item_owner_id}
                                   :handler         (fn [data]
                                                      (if (and (= "success" data) init-data)
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin"))
                                                      
                                                      )
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text))
                                                      )})}
         (t :core/Delete)]
        ])])
  )

(defn send-message [state visible_area item_owner]
  (let [{:keys [item_owner_id gallery-state init-data item_type info]} @state
        mail (cursor state [:mail])
        status (cursor state [:status])
        email-atom (cursor state [:selected-email])
        ]
    
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a
       {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "send-message" @visible_area) "" "send-message")))}
       
       (if (or (= item_type "badge") (= item_type "page"))
         (t :admin/Sendmessagetoowner)
         (t :admin/Sendmessage))]]
     (if (= @visible_area "send-message")
       [:div.col-md-12
        (str (t :admin/Sendmessageforuser) " " item_owner)
        [:div.form-group
         [:label 
          (str (t :user/Email) ":")]
         (email-select (:emails info) email-atom) ]
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :disabled (if-not (and
                                     (< 1 (count (:subject @mail)))
                                     (< 1 (count (:message @mail))))
                              "disabled")
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/send_message/" item_owner_id ))
                                  {:response-format :json
                                   :keywords?       true
                                   :params        {:subject (:subject @mail)
                                                   :message (:message @mail)
                                                   :email  @email-atom}  
                                   :handler         (fn [data]
                                        ;(navigate-to "/admin")
                                                      (reset! status data)
                                                      (reset! mail {:subject ""
                                                                    :message ""})
                                                      )
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text))
                                                      )})
                  }
         (t :admin/Sendmessage)]
       (status-handler status item_type)]
       )]))

(defn label-text [label text]
  [:div {:class "issuer-data clearfix"}
   [:label.pull-left  label ":"]
   [:div {:class "issuer-links pull-left"}
    [:a {:href "#"} " " text]]])


(defn badge-info-block [info owner owner_id]
  (let [{:keys [issuer_content_name issuer_content_url issuer_contact issuer_image creator_name creator_url creator_email creator_image]} info]
    [:div
     (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
     (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
     (if (and owner owner_id)
       [:div {:class "issuer-data clearfix"}
        [:label.pull-left  (t :admin/Owner) ":"]
        [:div {:class "issuer-links pull-left"}
         [:a {:target "_blank" :href (path-for (str "/user/profile/" owner_id))} owner]]])
     ]))




(defn user-info-block [info]
 [:div
   [:div {:class "row"}
    [:label.col-xs-2 (t :user/Email) ":"]
    [:div.col-xs-8
     (doall
      (for [element-data (:emails info)]
        [:div {:key (hash (:email element-data)) :class (if (:primary_address element-data) "primary-address" "") }  (:email element-data)]
        ))]
    
    ]
  [:div {:class "row"}
   [:label.col-xs-2 (t :admin/Created) ":"]
   [:div.col-xs-8
    (date-from-unix-time (* 1000 (:ctime info)) "minutes")]]
  [:div {:class "clearfix"}
    [:label.pull-left (t :admin/Lastlogin) ":"]
   (date-from-unix-time (* 1000 (:last_login info)) "minutes")]])

(defn page-info-block [owner owner_id]
  [:div {:class "issuer-data clearfix"}
        [:label.pull-left  (t :admin/Owner) ":"]
        [:div {:class "issuer-links pull-left"}
         [:a {:target "_blank" :href (path-for (str "/user/profile/" owner_id))} owner]]])


(defn info-block [state item_type]
  (let [{:keys [info item_owner item_owner_id]} @state]
    (cond
      (= "badge" item_type) (badge-info-block info item_owner item_owner_id)
      (= "badges" item_type) (badge-info-block info nil nil)
      (= "user" item_type)                              (user-info-block info)
      (= "page" item_type)                              (page-info-block item_owner item_owner_id)
      :else                                             "")))

(defn private-item [state visible_area item_owner]
  (let [{:keys [item_type item_id gallery-state init-data name]} @state]
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "private-item" @visible_area) "" "private-item"))) :class (if (= "private-item" @visible_area) "opened" "")} (t :admin/Privatethis)]]
     (if (= @visible_area "private-item")
       [:div.col-md-12
        [:div.privateitem (str (t :admin/Privatethis) " "  item_owner " " name " " (t (keyword (str "admin/" item_type))) "?" ) ]
        [:div [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/private_"item_type"/" item_id))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          {:item-type item_type :item-id item_id}
                                   :handler         (fn [data]
                                                      (if init-data
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin")))
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text)))})}
         (t :admin/Private)]]])]))


(defn unlock-user [state visible_area item_owner]
  (let [{:keys [item_type item_owner_id gallery-state init-data name]} @state]
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "unlock-user" @visible_area) "" "unlock-user")))} (t :admin/Unlockuser)]]
     (if (= @visible_area "unlock-user")
       [:div.col-md-12
        (str (t :admin/Unlockuser) " "   item_owner "?" )
        [:button {:type         "button"
                  :class        "btn btn-primary pull-right"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/undelete_user/" item_owner_id))
                                  {:response-format :json
                                   :keywords?       true
                                   :handler         (fn [data]
                                                      (if init-data
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin")))
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text)))})}
         (t :core/Yes)]])]))






(defn password-reset [state visible_area item_owner]
  (let [{:keys [item_type item_owner_id gallery-state init-data name info]} @state
        email-atom (cursor state [:selected-email])]
    [:div {:class "row"}
     [:div {:class "col-xs-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "password-reset" @visible_area) "" "password-reset")))} (t :admin/Sendpasswordreset)]]
     (if (= @visible_area "password-reset")
       [:div.col-xs-12.row
        [:div {:class "form-group col-sm-8 col-xs-12"}
         (email-select (:emails info) email-atom)
         
         
         ]
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/user/reset/"))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          {:email @email-atom}
                                   :handler         (fn [data]
                                                      (if init-data
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin")))
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text)))})}
         [:span (t :admin/Sendresetlink)]]])]))

(defn lock-user [state visible_area item_owner]
  (let [{:keys [mail item_owner_id gallery-state init-data info]} @state
        mail (cursor state [:mail])
        email-atom (cursor state [:selected-email])]
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "lock-user" @visible_area) "" "lock-user"))) :class (if (= "lock-user" @visible_area) "opened" "")} (t :admin/Lockuser) ]]
     (if (= @visible_area "lock-user")
       [:div.col-md-12
        (str (t :admin/Lockuser) " " item_owner "?")
        [:div.form-group
         [:label 
          (str (t :user/Email) ":")]
         (email-select (:emails info) email-atom) ]
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/delete_user/" item_owner_id ))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          {:subject (:subject @mail)
                                                     :message (:message @mail)
                                                     :email  @email-atom}
                                   :handler         (fn [data]
                                                      (if (and (= "success" data) init-data)
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin"))
                                                      )
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text)) )})}
         (t :admin/Lock)]]
       )]))

(defn admin-modal-container [state]
  (let [{:keys [item_type item_id item_owner_id image_file name info item_owner gallery-state init-data]} @state
        visible_area (cursor state [:visible_area])
        item_owner (if (and (vector? item_owner) (< 1 (count item_owner))) (str (count item_owner) " " (t :admin/Earners)  ) (if (vector? item_owner) (first item_owner) item_owner))
        mail (cursor state [:mail])]
    [:div {:class "admin-modal"}
     [:div.row
      [:div {:class "col-sm-3 badge-image modal-left"}
       [:img {:src (profile-picture image_file)} ]]
      [:div {:class "col-sm-9 badge-info"}
       [:div {:class "row info"}
        [:div {:class "col-md-12"}
         [:h1.uppercase-header name]
         (info-block state item_type)
         ]]
        [:div.actions
       (if (not (= item_type "badges"))
         (send-message state visible_area item_owner))
       (if (not (= item_type "user"))
         (private-item state visible_area item_owner))
       (if (not (= item_type "user"))
         (delete-item state visible_area item_owner))
       (if (= item_type "user")
         (if (:deleted info)
           (unlock-user state visible_area item_owner)
           (lock-user state visible_area item_owner)))
       (if (= item_type "user")
         (password-reset state visible_area item_owner))]]]]))

(defn admin-modal [state]
  [:div
   [:div.modal-header
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (admin-modal-container state)
      ]
     [:div.modal-footer
      ;[:button {:type         "button"
      ;          :class        "btn btn-primary"
      ;          :data-dismiss "modal"}
      ; (t :core/Close)]
      ]])