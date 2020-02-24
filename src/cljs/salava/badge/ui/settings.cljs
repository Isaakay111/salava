(ns salava.badge.ui.settings
  (:require [reagent.core :refer [cursor atom]]
            [clojure.string :as string]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.tag :as tag]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.helper :refer [plugin-fun private? navigate-to path-for js-navigate-to hyperlink url?]]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.share :as s]
            [reagent.session :as session]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn init-badges
  ([state]
   (ajax/GET
    (path-for "/obpv1/badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                       :pending () ;(filter #(= "pending" (:status %)) data)
                       :initializing false))})))

(defn delete-badge [state]
  (ajax/DELETE
   (path-for (str "/obpv1/badge/" (:id @state)))
   {:handler  (fn []
                #_(init-badges state)
                #_(navigate-to "/badge"))}))

(defn export-to-pdf [user-badge-id]
  (let [lang-option "all"
        badge-url (str "/obpv1/badge/export-to-pdf?id=" user-badge-id "&lang-option=" lang-option)]
    (js-navigate-to badge-url)))

(defn update-settings [badge-id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/settings/" badge-id) true)
   {:handler (fn [data]
               (swap! state assoc :badge-settings (assoc data :new-tag "")))}))

#_(defn save-settings [state reload-fn]
    (let [{:keys [id visibility tags rating]} (:badge-settings @state)]
      (ajax/PUT
       (path-for (str "/obpv1/badge/save_settings/" id))
       {:params  {:visibility   visibility
                  :tags         tags
                  :rating       (if (pos? rating) rating nil)}
        :handler (fn [] (update-settings id state))
        :finally (fn [] (when reload-fn (reload-fn)))})))

(defn save-settings [state reload-fn]
  (let [{:keys [id visibility tags rating show_recipient_name]} (:badge-settings @state)]
    (ajax/PUT
     (path-for (str "/obpv1/badge/settings/" id))
     {:params  {:settings {:visibility   visibility
                           :rating       (if (pos? rating) rating nil)
                           :show_recipient_name show_recipient_name} 
                :tags tags}
      :handler (fn [] (update-settings id state))
      :finally (fn [] (when reload-fn (reload-fn)))})))

(defn toggle-recipient-name [state]
  (let [show-recipient-name-atom (cursor state [:show_recipient_name])
        _ (cursor state [:badge-settings :show_recipient_name])
        new-value (if (pos? @show-recipient-name-atom) 0 1)]
    (reset! _ new-value)
    (save-settings state (fn [] (reset! show-recipient-name-atom new-value)))))

(defn toggle-evidence [state]
  (let [id (get-in @state [:badge-settings :id])
        new-value (not (get-in @state [:badge-settings :show_evidence]))]
    (ajax/POST
     (path-for (str "/obpv1/badge/toggle_evidences_all/" id))
     {:params {:show_evidence new-value}
      :handler (fn [] (do

                        (swap! state assoc-in [:badge-settings :show_evidence] new-value)
                        (swap! state assoc :show_evidence new-value)))})))

(defn toggle-receive-notifications [badge_id notifications-atom]
  (let [req-path (if @notifications-atom
                   (str "/obpv1/social/delete_connection_badge/" badge_id)
                   (str "/obpv1/social/create_connection_badge/" badge_id))]
    (ajax/POST (path-for req-path)
               {:handler (fn [data]
                           (reset! notifications-atom (:connected? data)))})))

(defn delete-tab-content [{:keys [name image_file]} state]
  [:div.row.flip
   [:div {:class "col-md-3 badge-image modal-left"}
    [:img {:src (str "/" image_file) :alt name}]]
   [:div {:class "col-md-9 delete-confirm delete-tab"}
    [:div {:class "alert alert-warning"}
     (t :badge/Confirmdelete)]
    [:div
     [:button {:type     "button"
               :class    "btn btn-primary"
               :data-dismiss "modal"
               :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
      (t :badge/Cancel)]
     [:button {:type         "button"
               :class        "btn btn-warning"
               :data-dismiss "modal"
               :on-click     #(delete-badge state)}
      (t :badge/Delete)]]]])

(defn settings-tab-content [data state init-data]
  (let [{:keys [id name image_file issued_on expires_on show_evidence revoked rating]} data
        expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])
        notifications-atom (cursor state [:receive-notifications])
        revoked (pos? revoked)
        badge_id (:badge_id @state)]
    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]]
     [:div {:class "col-md-9 settings-content settings-tab"}
      (cond
        revoked [:div.revoked (t :badge/Revoked)]
        expired? [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))]
        (and (not revoked) (not expired?)) [:div [:div {:class "form-horizontal"}
                                                  [:div.form-group
                                                   [:fieldset {:class "col-md-9 checkbox"}
                                                    [:legend.md-9 ""]
                                                    [:div.col-md-12 [:label {:for "show-name"}
                                                                     [:input {:type      "checkbox"
                                                                              :id        "show-name"
                                                                              :on-change #(toggle-recipient-name state)
                                                                              :checked   @show-recipient-name-atom}]
                                                                     (t :badge/Showyourname)]]]]
                                                  [:div.form-group
                                                   [:fieldset {:class "col-md-9 checkbox"}
                                                    [:legend.col-md-9 ""]
                                                    [:div.col-md-12 [:label {:for "receive-notifications"}
                                                                     [:input {:type      "checkbox"
                                                                              :id        "receive-notifications"
                                                                              :on-change #(toggle-receive-notifications badge_id notifications-atom)
                                                                              :checked   @notifications-atom}]
                                                                     (str (t :social/Getbadgenotifications))]]]]
                                                  [:div
                                                   [:div {:class "row"}
                                                    [:label {:class "col-md-12 sub-heading" :for "newtags"}
                                                     (t :badge/Tags)]]
                                                   [:div {:class "row"}
                                                    [:div {:class "col-md-12"}
                                                     [tag/tags (cursor state [:badge-settings :tags]) state init-data]]]
                                                   [:div {:class "form-group"}
                                                    [:div {:class "col-md-12"}
                                                     [tag/new-tag-input (cursor state [:badge-settings :tags]) (cursor state [:badge-settings :new-tag]) state init-data]]]

                                                   (into [:div]
                                                         (for [f (plugin-fun (session/get :plugins) "block" "evidence_block")]
                                                           [f data state init-data]))

                                                   (into [:div]
                                                         (for [f (plugin-fun (session/get :plugins) "block" "badge_settings")]
                                                           [f id]))]]

                                            [:div.modal-footer]])]]))

#_(defn revalidation-request [user-badge-id state]
    (ajax/POST
     (path-for (str "/obpv1/factory/pdf_cert_request/" user-badge-id))
     {:params  {:message   "Please revalidate my badge!"}
      :handler (fn [data]
                 (swap! state merge data))}))

(defn cert-block [user-badge-id state]
  [:div
   [:span._label.sub-heading (t :badge/Downloadpdf)]
   (if-let [cert-uri (some-> @state :cert :uri)]
     [:div
      #_[:hr]
      #_[:div (t :badge/Downloadcertificateinfo)]
      #_[:label.sub-heading "Version"
         [:select.form-control {:name "cert_version" :style {:width "200px" :margin "10px 0"}}
          [:option "2019-11-01"]
          [:option "2019-11-14"]
          [:option "2019-11-28"]]]
      [:div
       (doall
        (map (fn [[badge lang]]
               (let [uri (str cert-uri "&lang=" lang)]
                 [:p {:key uri}
                  [:i.fa.fa-file-pdf-o.fa-2x] " "
                  [:a {:href uri} (if-not (string/blank? lang) (str badge " (" lang ")") badge)]]))
             (:badge @state)))]
      #_[:div
         [:button {:class "btn btn-primary" :on-click #(revalidation-request user-badge-id state)} (t :badge/RevalidationRequest)]]]
     [:div
      [:p
       [:i.fa.fa-file-pdf-o.fa-2x] " "
       [:a {:href "#" :on-click #(export-to-pdf user-badge-id)} (-> @state :badge first first)]]])
   [:div (t :badge/Pdfdownload)]])

(defn init-cert [user-badge-id state]
  (ajax/GET
   (path-for (str "/obpv1/factory/pdf_cert/" user-badge-id))
   {:handler (fn [data]
               (swap! state merge data))}))

(defn download-tab-content [{:keys [name image_file obf_url assertion_url]} state]
  (let [user-badge-id (:id @state)
        badge (map (fn [v] [(:name v) (:language_code v)]) (:content @state))
        cert-state (atom {:cert [] :badge badge})]
    (init-cert user-badge-id cert-state)
    (fn []
      [:div {:id "badge-settings" :class "row flip"}
       [:div {:class "col-md-3 badge-image modal-left"}
        [:img {:src (str "/" image_file) :alt ""}]]
       [:div {:class "col-md-9 settings-content download-tab"}
        [cert-block user-badge-id cert-state]
        [:hr]
        [:div
         [:a {:class "btn btn-primary" :href (str obf_url "/c/receive/download?url=" (js/encodeURIComponent assertion_url))} (t :badge/Downloadbadgeimage)]
         [:div (t :badge/Downloadbakedbadge)]]]])))
