(ns salava.badgeIssuer.ui.criteria
  (:require
   [cemerick.url :as url]
   [clojure.walk :refer [keywordize-keys]]
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.badgeIssuer.ui.helper :refer [logo]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for disable-background-image]]
   [salava.core.ui.layout :as layout]))

(defn init-data [id state]
  (ajax/GET
   (path-for (str "/obpv1/selfie/criteria/" id))
   {:handler (fn [data]
               (swap! state merge data))}))

(defn content [state]
   (create-class
    {:reagent-render
     (fn []
       (let [{:keys [id criteria_content name description image_file]} @state]
        [:div
         [:div#content
          [:div.container.main-container
           [:div.row
            [:div.col-md-2.col-sm-3]
            [:div.col-md-10.col-sm-9
             [:div.col-md-12
              [:div#selfie_criteria
               [:div.row
                (logo)]
               [:div.panel.panel-default.thumbnail {:style {:margin "10px 0"}}
                [:div.panel-body
                 [:div.row.flip
                  [:div.col-md-3.badge-image
                   [:img {:src (str "/" image_file) :alt ""}]]
                  [:div.col-md-9
                    [:div.col-md-12
                     [:h1.uppercase-header name]
                     [:div.description {:style {:font-weight "bold"}} description]
                     [:hr.border]
                     [:div.criteria-background
                      {:dangerouslySetInnerHTML {:__html criteria_content}}]]]]]]]]]]]]
         (layout/footer nil)]))
     :component-will-mount
     (fn [] (disable-background-image))}))

(defn handler [site-navi params]
  (let [id (:id params)
        state (atom {:id id})]
    (init-data id state)
    (fn []
      (layout/embed-page [content state]))))
