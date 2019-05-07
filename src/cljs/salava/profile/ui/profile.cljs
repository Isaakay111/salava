(ns salava.profile.ui.profile
  (:require [salava.core.ui.helper :refer [plugin-fun path-for not-activated? private?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.profile.ui.block :as pb]
            [salava.core.ui.error :as err]
            [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.profile.ui.helper :as ph]
            [reagent-modals.modals :as m]
            [salava.profile.ui.edit :as pe]
            [salava.page.themes :refer [themes borders]]
            [salava.core.ui.modal :as mo]))


(defn tabs [state])

(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))

(defn add-page [state]
   (when (:edit-mode @state) [:span.dropdown [:a.dropdown-toggle {:data-toggle "dropdown"} [:i.fa.fa-plus-square]]
                                [:ul.dropdown-menu
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (mo/open-modal [:page :my] {:tab-atom (cursor state [:tabs])} {:hidden (fn [] (prn @(cursor state [:tabs])))}))}

                                          (t :profile/Addexistingpage)]]
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (as-> (first (plugin-fun (session/get :plugins) "my" "create_page")) f (when f (f)) ))} (t :profile/Createnewpage)]]]]))



(defn profile-navi [state]
  [:div.profile-navi

    (conj (reduce (fn [r tab]
                   (conj r [:li.nav-item {:key (:id tab) :class (if (= (:id tab) (:active-index @state)) "active")}
                            [:a.nav-link {:href "#" :on-click #(do
                                                                ;(.preventDefault %)
                                                                (swap! state assoc :active-index (:id tab) :show-manage-buttons false))}
                                         (:name tab)]]))
           [:ul.nav.nav-tabs
            [:li.nav-item {:class (if (= 0 (:active-index @state)) "active")}
                          [:a.nav-link {:on-click #(swap! state assoc :active-index 0 :show-manage-buttons true)}
                                       (t :user/Myprofile)]]]
           @(cursor state [:tabs])) [add-page state])
    #_(when (:edit-mode @state) [:div.dropdown [:a.dropdown-toggle {:data-toggle "dropdown"} [:i.fa.fa-plus-square]]]
                               [:ul.dropdown-menu
                                [:li [:a {:href "#" :on-click #(do
                                                                (.preventDefault %)
                                                                (mo/open-modal [:page :my] {:tab-atom (cursor state [:tabs])} {:hidden (fn [] (prn @(cursor state [:tabs])))}))}

                                         (t :profile/Addexistingpage)]]
                                [:li [:a {:href "#" :on-click #(do
                                                                (.preventDefault %)
                                                                (as-> (first (plugin-fun (session/get :plugins) "my" "create_page")) f (when f (f)) ))} (t :profile/Createnewpage)]]])])

(defn profile-blocks [state]
  (let [blocks (cursor state [:blocks])
        block-count (count @blocks)
        position (if (pos? block-count) (dec block-count) nil)]
    [:div {:id "field-editor"}
     (into [:div {:id "page-blocks"}]
           (for [index (range (count @blocks))]
             (ph/block-for-edit (cursor blocks [index]) state index)))
     [ph/field-after blocks state nil]]))

(defn get-page [state]
 (let [block (first (plugin-fun (session/get :plugins) "block" "page"))]
  (if block [block @(cursor state [:active-index]) [:div ""]])))



(defn edit-profile-content [state]
   (if (= 0 @(cursor state [:active-index]))
       [:div#page-edit
         [pe/action-buttons state]
         [:div.panel.thumbnail
          [:div.panel-heading
           [:h3 (t :profile/Personalinformation)]]
          [:div.panel-body
           [pe/edit-profile state]]]
         [profile-blocks state]]
    [get-page state]))
    ;(into [:div] (as-> (first (plugin-fun (session/get :plugins) "block" "page")) f (when f (f @(cursor state [:active-index])))))))


(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        blocks (cursor state [:blocks])]
    [:div#page-view
     [:div {:id (str "theme-" (or @(cursor state [:theme]) 0))
            :class "page-content"}
      [:div.panel
       [:div.panel-left
        [:div.panel-right
         [:div.panel-content
          [:div.panel-heading
           [:h3 (t :profile/Personalinformation)]]
          [:div.panel-body
           [profile-info-block]
           (into [:div#profile]
                 (for [index (range (count @blocks))]
                   (ph/block (cursor blocks [index]) state index)))]]]]]]]))

(defn theme-selection [theme-atom themes]
  (reduce (fn [r theme]
            (conj r [:div {:id (str "theme-" (:id theme))}
                     [:a {:href "#" :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! theme-atom (js/parseInt (:id theme))))
                          :alt (t (:name theme)) :title (t (:name theme))}[:div {:class (str "panel-right theme-container" (if (= @theme-atom (:id theme)) " selected"))} " "]]]))
          [:div {:id "theme-container"}] themes))

(defn edit-theme [state]
  [:div {:id "page-edit-theme"}
   [:div {:class "panel page-panel thumbnail" :id "theme-panel"}
    [:div.panel-heading
     [:h3 (t :page/Selecttheme)]]
    [:div.panel-body
     [theme-selection (cursor state [:theme]) themes]]]
   [pe/action-buttons state]
   [view-profile state]])

(defn edit-settings [state]
  (let [visibility-atom (cursor state [:edit-profile :user :profile_visibility])]
    [:div#page-edit
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :page/Settings)]]
      [:div.panel-body
       (if-not (private?)
         [:div
          [:div.row [:label.col-xs-12 (t :user/Profilevisibility)]]
          [:div.radio {:id "visibility-radio-internal"}
           [:label [:input {:name      "visibility"
                            :value     "internal"
                            :type      "radio"
                            :checked   (= "internal" @visibility-atom)
                            :on-change #(reset! visibility-atom (.-target.value %))}]
            (t :user/Visibleonlytoregistered)]]
          [:div.radio
           [:label [:input {:name      "visibility"
                            :value     "public"
                            :type      "radio"
                            :checked   (= "public" @visibility-atom)
                            :on-change #(reset! visibility-atom (.-target.value %))}]
            (t :core/Public)]]])]]

     [pe/action-buttons state]]))

(defn edit-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        content @(cursor state [:edit :active-tab])]
    [:div

     (case content
       :content [edit-profile-content state]
       :theme [edit-theme state]
       :settings [edit-settings state]
      :preview [view-profile state]
       nil)]))

(defn content [state]
  [:div
   [m/modal-window]
   [:div#profile
    [profile-navi state]
    (when @(cursor state [:show-manage-buttons]) [ph/manage-buttons state])]
   (if @(cursor state [:edit-mode])
     [edit-profile state]
     [view-profile state])])





(defn init-data [user-id state]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data]
                (let [data-with-uuids (assoc data :blocks (vec (map #(assoc % :key (pe/random-key))
                                                                    (get data :blocks))))]
                  (swap! state assoc :permission "success" :edit {:active-tab :content})
                  (swap! state merge data-with-uuids)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true
                     :active-index 0
                     :edit-mode false
                     :toggle-move-mode false
                     :blocks []
                     :edit {:active-tab :content}
                     :theme 0
                     :alert nil
                     :tabs []
                     :show-manage-buttons true})
        user (session/get :user)]
    (init-data user-id state)

    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/default site-navi [:div])
        (and user (= "error" (:permission @state)))(layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (= (:id user) (js/parseInt user-id)) (layout/default site-navi (content state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (content state))
        :else (layout/landing-page site-navi (content state))))))
