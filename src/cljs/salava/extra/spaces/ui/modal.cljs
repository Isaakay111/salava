(ns salava.extra.spaces.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :refer [cursor atom]]
    [reagent-modals.modals :as m]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.modal :as mo]
    [salava.core.ui.helper :refer [path-for]]
    [salava.core.i18n :refer [t]]
    [salava.core.time :refer [date-from-unix-time]]
    [salava.extra.spaces.ui.creator :as creator]
    [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]))

(defn delete-space! [state]
  (ajax/DELETE
    (path-for (str "/obpv1/spaces/delete/" (:id @state)) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  ""))}))

(defn init-data [id state]
  (ajax/GET
    (path-for (str "/obpv1/spaces/"id))
    {:handler (fn [data]
                (swap! state assoc :space data))}))

(defn downgrade-member! [admin-id state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/downgrade/" (:id @state) "/" admin-id) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (init-data (:id @state) state)))}))

(defn add-admin [state]
 (ajax/POST
  (path-for (str "/obpv1/spaces/add_admin/" (:id @state)) true)
  {:params {:admins (map :id @(cursor state [:new-admins]))}
   :handler (fn [data]
              (when (= "success" (:status data))
                (init-data (:id @state) state)
                (reset! (cursor state [:new-admins]) [])))}))

(defn space-logo [state]
  (let [{:keys [logo name]} @(cursor state [:space])]
    [:div.text-center {:class "col-md-3" :style {:margin-bottom "20px"}}
     [:div
      (if-not (blank? logo)
        [:img.space-img {:src (if (re-find #"^data:image" logo)
                                  logo
                                 (str "/" logo))
                         :alt name}]
        [:i.fa.fa-building-o.fa-5x {:style {:margin-top "10px"}}])]]))

(defn space-banner [state]
  (let [{:keys [banner]} @(cursor state [:space])]
    (when banner
      [:div.space-banner-container {:style {:max-width "640px" :max-height "120px"}}
        [:img {:src (if (re-find #"^data:image" banner)
                        banner
                        (str "/" banner))}]])))

(defn view-space [state]
  (let [{:keys [name description ctime status alias css]} @(cursor state [:space])
        {:keys [p-color s-color t-color]} css]
    [:div {:style {:line-height "2.5"}}
      [space-banner state]
      [:h1.uppercase-header name]
      [:p [:b description]]
      [:div [:span._label (str (t :extra-spaces/Alias) ":  ")] alias]
      [:div [:span._label (str (t :extra-spaces/Createdon) ":  ")] (date-from-unix-time (* 1000 ctime))]
      [:div [:span._label (str (t :extra-spaces/Status) ": ")] status]
      (when css
        [:div
          [:div [:span._label (str (t :extra-spaces/Primarycolor) ":  ")] [:span.color-span {:style {:background-color p-color}}]]
          [:div [:span._label (str (t :extra-spaces/Secondarycolor) ":  ")] [:span.color-span {:style {:background-color s-color}}]]
          [:div [:span._label (str (t :extra-spaces/Tertiarycolor) ":  ")] [:span.color-span {:style {:background-color t-color}}]]])]))


(defn edit-space [state]
   [creator/modal-content state])


(defn delete-space-content [state]
  [:div.row
    (when (> (:member_count @state) 1) [:p [:b (t :extra-spaces/Aboutdelete)]])
    [:div.alert.alert-danger
     (t :badge/Confirmdelete)]
    [:hr.line]
    [:div.btn-toolbar
     [:div.btn-group
      [:button.btn.btn-primary
       {:type "button"
        :aria-label (t :core/Cancel)
        :on-click #(do
                     (.preventDefault %)
                     (swap! state assoc :tab nil :tab-no 1))}
       (t :core/Cancel)]
      [:button.btn.btn-danger
       {:type "button"
        :on-click #(do
                     (.preventDefault %)
                     (delete-space! state))
        :data-dismiss "modal"}
       (t :core/Delete)]]]])

(defn manage-space [state]
  [:div.row
   [:div.form-group
    [:div.panel.panel-default
     [:div.panel-heading
      (t :extra-spaces/Admins)]

     [:table {:class "table" :summary (t :badge/Issuers)}
      [:thead
       [:tr
        [:th {:style {:display "none"}}  "Logo"]
        [:th {:style {:display "none"}} (t :badge/Name)]
        [:th {:style {:display "none"}} "Action"]]]
      (into [:tbody]
        (for [admin @(cursor state [:space :admins])
              :let [{:keys [id profile_picture first_name last_name]} admin
                    name (str first_name " " last_name)]]
          [:tr
            [:td  [:img {:style {:width "40px" :height "40px"} :alt "" :src (profile-picture profile_picture)}]]
            [:td.text-center {:style {:vertical-align "middle"}} [:a {:href "#" :on-click #(do
                                                                                             (.preventDefault %)
                                                                                             (mo/open-modal [:profile :view] {:user-id id}))}
                                                                  name]]
            [:td {:style {:vertical-align "middle"}} (when (= id @(cursor state [:space :last_modified_by])) [:span.label.label-info "last-modified"])]
            [:td {:style {:text-align "end"}} [:button.btn.btn-primary.btn-bulky
                                                {:on-click #(do
                                                              (.preventDefault %)
                                                              (downgrade-member! id state))
                                                 :disabled (= 1 (count @(cursor state [:space :admins])))}
                                                (t :extra-spaces/Downgradetomember)]]]))]
     [:hr.line]
     [:div#social-tab {:style {:background-color "ghostwhite" :padding "8px"}}
      #_[:span._label (t :extra-spaces/Admins)]
      #_[:p (t :extra-space/Aboutadmins)]
      [:div
        [:a {:href "#"
             :on-click #(do
                          (.preventDefault %)
                          (mo/open-modal [:gallery :profiles]
                           {:type "pickable"
                            :selected-users-atom (cursor state [:new-admins])
                            :existing-users-atom (cursor state [:space :admins])
                            :context "space_admins_modal"} {}))}

         [:span [:i.fa.fa-user-plus.fa-fw.fa-lg] (t :extra-spaces/Addadmins)]]]
      (reduce (fn [r u]
                (let [{:keys [id first_name last_name profile_picture]} u]
                  (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                           [:a {:href "#" :on-click (fn [] (reset! (cursor state [:space :admins]) (->> @(cursor state [:space :admins]) (remove #(= id (:id %))) vec)))}
                            [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
              [:div.selected-users-container] @(cursor state [:new-admins]))
      [:div.btn-toolbar
       [:div.btn-group
        [:button.btn-primary.btn.btn-bulky
         {:type "button"
          :on-click #(do
                       (.preventDefault %)
                       (add-admin state))
          :disabled (empty? @(cursor state [:new-admins]))}
         (t :core/Add)]
        [:button.btn-warning.btn.btn-bulky
         {:type "button"
          :on-click #(do
                       (.preventDefault %)
                       (reset! (cursor state [:new-admins]) []))}
         (t :core/Cancel)]]]]]]])

(defn space-navi [state]
 (let [disable-link (when (= "deleted" @(cursor state [:space :status])) "btn disabled")]
  [:div.row.flip-table
   [:div.col-md-3]
   [:div.col-md-9
     [:ul {:class "nav nav-tabs wrap-grid"}
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 1 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-space state] :tab-no 1)}
         [:div  [:i.nav-icon.fa.fa-info-circle.fa-lg] (t :metabadge/Info)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 2 (:tab-no @state))) "active")}
        [:a.nav-link {:class disable-link :href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 2)}
         [:div  [:i.nav-icon.fa.fa-edit.fa-lg] (t :extra-spaces/Edit)]]]
       #_[:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
          [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 3)}
           [:div  [:i.nav-icon.fa.fa-cog.fa-lg] (t :extra-spaces/Manage)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-space state]  :tab-no 4)}
         [:div  [:i.nav-icon.fa.fa-cogs.fa-lg] (t :extra-spaces/Managespace)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
        [:a.nav-link {:class disable-link :href "#" :on-click #(swap! state assoc :tab [delete-space-content state] :tab-no 5)}
         [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]]))

(defn space-content [state]
  [:div#space
   [space-navi state]
   [:div.col-md-12
    [space-logo state]
    [:div.col-md-9
     [:div {:style {:margin "10px 0"}}
       (or
         (:tab @state)
         (case (:tab-no @state)
           2 [edit-space state]
           4 [manage-space state]
           5 [delete-space-content state]
           [view-space state]))]]]])



(defn handler [params]
  (let [id (:id params)
        no-of-members (:member_count params)
        state (atom {:id id
                     :tab-no 1
                     :in-modal true
                     :member_count no-of-members
                     :new-admins []})]
    (init-data id state)
    (fn []
      [space-content state])))


(def ^:export modalroutes
  {:space {:info handler}})
