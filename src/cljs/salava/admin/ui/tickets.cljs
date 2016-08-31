(ns salava.admin.ui.tickets
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :as set :refer [intersection]]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for unique-values]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/admin/tickets/")
   {:handler (fn [tickets]
               (swap! state assoc :tickets tickets))}))

(defn ticket [ticket-data state]
  (let [{:keys [item_type item_id item_name first_name last_name item_url reporter_id description ctime report_type id]} ticket-data]
    [:div {:class "media" :id "ticket-container"}
     [:div.media-body
      [:div {:class (str "title-bar title-bar-" report_type ) }
       [:div {:class "pull-right"} (date-from-unix-time (* 1000 ctime) "minutes")]
       [:h3 {:class "media-heading"}
        [:u (t (keyword (str "admin/" report_type)))]]
       [:h4 {:class "media-heading"}
        [:a {:href item_url :target "_blank"}
         (str (t (keyword (str "admin/" item_type))) " - " item_name)]]]
      [:div.media-descriprtion
       [:div.col-xs-12
        [:div [:label (str (t :admin/Description) ": ")] " " description]
        [:div [:label (str (t :admin/Reporter) ": ")] " " [:a {:href (path-for (str "/user/profile/" reporter_id))}(str first_name " " last_name)]]]
       [:button {:class    "btn btn-primary pull-right"
                 :on-click #(do
                              (.preventDefault %)
                              (ajax/POST
                               (path-for (str "/obpv1/admin/close_ticket/" id))
                                 {:response-format :json
                                  :keywords? true
                                  :handler (fn [data]
                                             (init-data state)
                                             )
                                  :error-handler (fn [{:keys [status status-text]}])}))}
        (t :admin/Done)]]]]))

(defn ticket-visible? [element state]
  (if (or (> (count
              (intersection
               (into #{} (:types-selected @state))
               #{(:report_type element)}))
             0)
          (= (:types-all @state)
             true))
    true false))

(defn grid-buttons-with-translates [title buttons key all-key state]
  [:div.form-group
   [:label {:class "control-label col-sm-2"} title]
   [:div.col-sm-10
    (let [all-checked? (= ((keyword all-key) @state) true)
          buttons-checked ((keyword key) @state)]
      [:div.buttons
       [:button {:class (str "btn btn-default " (if all-checked? "btn-active"))
                 :id "btn-all"
                 :on-click (fn []
                             (swap! state assoc (keyword key) [])
                             (swap! state assoc (keyword all-key) true))}
        (t :core/All)]
       (doall
        (for [button buttons]
          (let [value button
                checked? (boolean (some #(= value %) buttons-checked))]
            [:button {:class    (str "btn btn-default " (if checked? "btn-active"))
                      :key      value
                      :on-click (fn []
                                  (swap! state assoc (keyword all-key) false)
                                  (if checked?
                                    (do
                                      (if (= (count buttons-checked) 1)
                                        (swap! state assoc (keyword all-key) true))
                                      (swap! state assoc (keyword key)
                                             (remove (fn [x] (= x value)) buttons-checked)))
                                    (swap! state assoc (keyword key)
                                           (conj buttons-checked value))))}
             (t (keyword (str "admin/" value)))])))])]])

(defn content [state]
  (let [tickets (:tickets @state)]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     [grid-buttons-with-translates (str (t :admin/Types) ":")  (unique-values :report_type tickets) :types-selected :types-all state]
     [:div
      (into [:div {:class "row"}]
            (for [data tickets]
              (if (ticket-visible? data state)
                (ticket data state))))]]))

(defn handler [site-navi]
  (let [state (atom {:tickets []
                     :types-selected []
                     :types-all true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
