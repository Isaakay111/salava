(ns salava.badge.ui.ext-endorsement
 (:require
  [clojure.string :refer [blank?]]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [reagent.session :as session]
  [salava.badge.ui.endorsement :as end :refer [process-text profile toggle-delete-dialogue confirm-delete]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.i18n :refer [t translate-text]]
  [salava.core.ui.helper :refer [path-for]]
  [salava.core.ui.input :refer [editor markdown-editor text-field file-input]]))

(defn status-modal [{:keys [message status]}]
  (let [alert-class (if (= status "success") "alert-success" "alert-danger")]
    [:div
     [:div.modal-header
      [:button {:type "button"
                :class "close"
                :data-dismiss "modal"
                :aria-label "OK"}
       [:span {:aria-hidden "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body
      [:div {:class (str "alert " alert-class)}
       message]]
     [:div.modal-footer
      [:button {:type "button"
                :class "btn btn-primary"
                :data-dismiss "modal"}
       "OK"]]]))

(defn delete-sent-request! [id state]
 (ajax/DELETE
  (path-for (str "/obpv1/badge/user_endorsement/ext_request/" id) true)
  {:handler (fn [data]
              "")}))

(defn init-endorsement [user-badge-id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/ext/" user-badge-id "/"(:endorser-id @state)))
   {:handler (fn [data]
               (reset! (cursor state [:ext-endorsement]) data))}))

(defn init-ext-endorser [id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/ext_request/endorser/" id))
   {:handler (fn [data]
               (reset! (cursor state [:ext-endorser]) data))}))

(defn init-request [endorser-id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user_endorsement/ext_request/info/" (:id @state) "/" (:endorser-id @state)))
    {:handler (fn [data]
                (when-not (empty? data)
                  (reset! (cursor state [:request]) data)
                  (init-ext-endorser (:endorser-id @state) state)
                  (init-endorsement (:id @state) state)))}))

(defn delete-endorsement [id state reload-fn]
  (let [id (if id id @(cursor state [:ext-endorsement :id]))]
    (ajax/DELETE
      (path-for (str "/obpv1/badge/user_endorsement/ext/endorsement/" id))
      {:handler (fn [data]
                  (when (= (:status data) "success")
                    (if reload-fn
                     (reload-fn)
                     (do
                       (m/modal! [status-modal {:status "success" :message (t :badge/Endorsementdeletesuccess)}] {})
                       (init-request (:endorser-id @state) state)
                       (swap! state assoc :show-content "none")))))})))

(defn decline-endorsement-request [user-badge-id state]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/ext_request/update_status/" user-badge-id))
   {:params {:status "declined"
             :email @(cursor state [:ext-endorser :email])}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (m/modal! [status-modal {:status "success" :message (t :badge/Requestsuccessfullydeclined)}] {})
                 (init-request (:endorser-id @state) state)))}))

(defn save-ext-endorsement [user-badge-id state update?]
 (let [path (if update?
                (str "/obpv1/badge/user_endorsement/ext/edit/" @(cursor state [:ext-endorsement :id]) "/" user-badge-id)
                (str "/obpv1/badge/user_endorsement/ext/endorse/" user-badge-id))]
  (ajax/POST
   (path-for path)
   {:params {:content (if update? @(cursor state [:ext-endorsement :content]) @(cursor state [:endorsement-comment]))
             :endorser (-> @(cursor state [:ext-endorser]) (select-keys [:name :url :email :description :ext_id :image_file]))}
    :handler (fn [{:keys [status]}]
               (when (= "error" status) (m/modal! [status-modal {:status "error" :message (t :core/Errorpage)}] {}))
               (when (= "success" status)
                 #_(swap! state assoc :show-link "none"
                                      :show-content "none")
                 (init-endorsement user-badge-id state)
                 (reset! (cursor state [:accept-terms] ) false)
                 (m/modal! [status-modal {:status "success" :message (t :badge/Endorsementsuccess)}] {})))})))

(defn update-status [id status user_badge_id state reload-fn]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/ext/update_status/" id))
   {:params {:user_badge_id user_badge_id
             :status status}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (when reload-fn (reload-fn state))))}))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file [id state]
  (let [file (-> (.querySelector js/document "#picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                   (js/FormData.)
                   (.append "file" file (.-name file)))]
    (swap! state assoc :uploading-image true)
    (ajax/POST
     (path-for (str "/obpv1/badge/user_endorsement/ext_request/endorser/upload_image"))
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! (cursor state [:ext-endorser :image_file]) (:url data))
                     (reset! (cursor state [:uploading-image]) false))
                  (m/modal! (upload-modal data) {:hidden #(reset! (cursor state [:uploading-image]) false)})))})))

(defn accept-terms [state]
 (let [term-atom (cursor state [:accept-terms])]
  [:div {:style {:text-align "center"}}
   [:fieldset.accept-terms-checkbox {:class "checkbox"}
    [:div [:label
           [:input {:type     "checkbox"
                    :on-change (fn [e]
                                 (if (.. e -target -checked)
                                   (reset! term-atom true)
                                   (reset! term-atom false)))}]
           (str (t :user/Externaluserterms) " ")
           [:a {:href (path-for  (str "/user/external/data/" @(cursor state [:ext-endorser :ext_id]) ) #_(t :user/Doyouaccept))
                :target "_blank"} (t :badge/here)]]]]]))

(defn endorser-info [id state]
  (let [{:keys [ext_id image_file name url description]} @(cursor state [:ext-endorser])]
   [:div.well.well-lg {:style {:padding-bottom "5px"}}
    [:form.form-horizontal
     [:div.form-group
      [:label {:for "input-name"} (t :badge/Name) [:span.form-required " *"] #_[info {:content (t :badgeIssuer/Badgenameinfo) :placement "right"}]]
      [text-field
       {:name "name"
        :atom (cursor state [:ext-endorser :name])
        :placeholder (t :badge/Inputyournameororganization)}]]
     [:div.form-group
      [:label {:for "input-url"} (t :badge/URL)]
      [text-field
       {:name "url"
        :atom (cursor state [:ext-endorser :url])}]]
     [:div.form-group
      [:label {:for "input-image"} (t :badge/Logoorpicture)]
      [:p (t :badge/Uploadimginstructions)]
      [:div {:style {:margin "5px"}}
       (if-not @(cursor state [:uploading-image])
         (if-not (blank? image_file)
          [:img {:src (if (re-find #"^data:image" image_file)
                        image_file
                        (str "/" image_file))
                 :alt "image"
                 :style {:width "100px" :height "auto"}}]
          [:i.fa.fa-file-image-o {:style {:font-size "60px" :color "#757575"}}])
         [:span.fa.fa-spin.fa-cog.fa-2x])]
      [:div.text-center {:style {:margin "5px" :width "100px"}}
       [:span {:class "btn btn-primary btn-file btn-bulky"}
             [:input {:id "picture-upload"
                      :type       "file"
                      :name       "file"
                      :on-change  #(send-file ext_id state)
                      :accept     "image/png"}]
        [:span #_[:i.fa.fa-file-image-o.fa-lg.fa-fw {:style {:color "inherit"} }](t :file/Upload)]]]]]]))



(defn endorse-badge-content [state]
 (let [{:keys [ext_id image_file name url description]} @(cursor state [:ext-endorser])]
  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr.border]
     [:div.endorse {:style {:margin "5px"}} (t :badge/Endorsehelptext)]

     [:div.row
      [:div.col-xs-12
       [:div.list-group
        [:a.list-group-item {:id "phrase1" :href "#" :on-click #(do
                                                                  (.preventDefault %)
                                                                  (process-text (t :badge/Endorsephrase1) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase1)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase2) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase2)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase3) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase3)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase4) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase4)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase5) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase5)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase6) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase6)]]]]]

     [:div.editor
      [:div.form-group
       [:label {:for (str "editor" ext_id)} (str (t :badge/Composeyourendorsement) ":") [:span.form-required " *"]]

       [:div [markdown-editor  (cursor state [:endorsement-comment]) (str "editor" ext_id)]]]

      [endorser-info ext_id state]
      [accept-terms state]
      [:div.text-center.btn-toolbar
       [:div.btn-group {:style {:float "unset"}}
        [:button.btn.btn-primary.btn-bulky {:on-click #(do
                                                         (.preventDefault %)
                                                         (save-ext-endorsement (:id @state) state nil))
                                            :disabled (or (not @(cursor state [:accept-terms]))  (blank? @(cursor state [:endorsement-comment])) (blank? @(cursor state [:ext-endorser :name])))}

         (t :badge/Endorsebadge)]

        [:button.btn.btn-danger.btn-bulky {:on-click #(do
                                                        (decline-endorsement-request (:id @state) state)
                                                        (.preventDefault %))}
          (t :badge/Deleteendorsementrequest)]]]


      [:hr.border]]])))


(defn ext-endorse-form [id state]
   (swap! state assoc :endorsement-comment "" :show-content "block" :show-link "none")
   ;(init-ext-endorser id state)
   (fn []
     [:div#endorsebadge {:style {:margin "20px auto"}}
       [endorse-badge-content state]]))

(defn endorsement [state]
 (let [{:keys [first_name last_name profile_picture status]} @(cursor state [:ext-endorsement])
       ext_id @(cursor state [:ext-endorser :ext_id])]
   [:div.well.well-lg {:style {:margin "10px auto" :background-image "none" :display @(cursor state [:show-content])}}
    [:div#badge-info
     [:div;.col-md-12
      [:p [:b (t :badge/Manageendorsementtext1)]]
      [:hr.border]
      [:div.row
       [:div.col-md-4.col-md-push-8  " "]
       [:div.col-md-8.col-md-pull-4  [profile {:status status :label (t :social/pending) :profile_picture profile_picture} (str first_name " " last_name)]]]
      [:div {:style {:margin-top "15px"}}
       [:div.editor
        [:div.form-group
         [:label {:for (str "editor" :ext_id)} (str (t :badge/Composeyourendorsement) ":") [:span.form-required " *"]]

         [:div [markdown-editor  (cursor state [:ext-endorsement :content]) (str "editor" ext_id)]]
         [endorser-info ext_id state]
         [accept-terms state]
         [:div.btn-toolbar.text-center
          [:div.btn-group {:style {:float "unset"}}
           [:button.btn.btn-primary.btn-bulky
            {:on-click #(save-ext-endorsement (:id @state) state true)
             :disabled (or (blank? @(cursor state [:ext-endorsement :content]))
                           (blank? @(cursor state [:ext-endorser :name]))
                           (not @(cursor state [:accept-terms])))}
            (t :badge/Save)]
           [:button.btn.btn-danger.btn-bulky
            {:on-click #(toggle-delete-dialogue state)} ;(delete-endorsement nil state nil)}
            [:i.fa.fa-trash] (t :badge/Deleteendorsement)]]]
         [confirm-delete state #(delete-endorsement nil state nil)]]]]]]]))

(defn ext-endorse-badge [state]
  (let [{:keys [endorser-id user-logged-in?]} @state]
   (when (and (false? user-logged-in?) (not (nil? endorser-id)))
       (swap! state assoc :endorsement-comment "" :show-content "block" :show-link "none")
       (init-request endorser-id state)
       (fn []
        (if-not (empty?  @(cursor state [:ext-endorsement]))
         [endorsement state]
         (case @(cursor state [:request :status])
           "pending" [ext-endorse-form endorser-id state]
           "endorsed" [:div ""]
           [:div ""]))))))

(defn language-switcher [state]
 (let [{:keys [endorser-id user-logged-in?]} @state
       current-lang (session/get-in [:user :language] "en")
       languages (session/get :languages)]
  (when (and (false? user-logged-in?) (not (nil? endorser-id)))
    [:div.pull-right
     (doall
      (map (fn [lang]
             ^{:key lang} [:a {:style (if (= current-lang lang) {:font-weight "bold" :text-decoration "underline"} {})
                               :href "#"
                               :on-click #(session/assoc-in! [:user :language] lang)}
                           (clojure.string/upper-case lang) " "])
           languages))])))

(defn ext-endorsement-content [params]
  (fn []
    (let [{:keys [endorsement state]} @params
          {:keys [id name image_file content user_badge_id issuer_id issuer_name email status type issued_on issuer_image]} endorsement]
      [:div.row.flip {:id "badge-info"}
       [:div.col-md-3
        [:div.badge-image [:img.badge-image {:src (str "/" image_file) :alt name}]]]
       [:div.col-md-9
        [:div
         [:h1.uppercase-header name]
         [:div (if (= type "ext_request") (t :badge/Managesentendorsementrequest) (t :badge/Manageendorsementtext2))]
         [:hr.line]
         [:div.row
          [:div.col-md-4.col-md-push-8  " "]
          [:div.col-md-8.col-md-pull-4 [profile {;:id (or endorsee_id issuer_id requester_id requestee_id)
                                                 :profile_picture issuer_image
                                                 :issuer_name issuer_name
                                                 :status status
                                                 :email email
                                                 :label (t :social/pending)} issuer_name]]]
         (if-not (= type "ext_request")
          [:div {:style {:margin-top "15px"}}
           [:div {:dangerouslySetInnerHTML {:__html content}}]

           [:div.caption
            [:hr.line]
            (if (= "pending" status)
              [:div.buttons
               [:button.btn.btn-primary {:href "#"
                                         :on-click #(do
                                                      (.preventDefault %)
                                                      (update-status id "accepted" user_badge_id state nil))

                                         :data-dismiss "modal"}  (t :badge/Acceptendorsement)]
               [:button.btn.btn-warning.cancel {:href "#"
                                                :on-click #(do
                                                             (.preventDefault %)
                                                             (update-status id "declined" user_badge_id state nil))
                                                :data-dismiss "modal"} (t :badge/Declineendorsement)]]
              [:div.row.flip.control-buttons
               (if-not @(cursor state [:show-delete-dialogue])
                 [:div.col-md-6.col-sm-6.col-xs-6  [:button.btn.btn-primary.cancel {:data-dismiss "modal"} (t :core/Cancel)]]
                 [:div.col-md-6.col-sm-6.col-xs-6  ""])
               [:div.col-md-6.col-sm-6.col-xs-6 [:a.delete-btn {:style {:line-height "4" :cursor "pointer"}
                                                                :on-click #(do
                                                                             (.preventDefault %)
                                                                             (toggle-delete-dialogue state))
                                                                :href "#"}
                                                 [:i.fa.fa-trash] (t :badge/Deleteendorsement)]]])
            [confirm-delete state #(delete-endorsement id state (fn []))]]]
          [:div {:style {:margin-top "15px"}}
           [:div {:dangerouslySetInnerHTML {:__html content}}]
           [:div.caption
            [:hr.line]
            [:a {:href "#"
                 :on-click #(do
                              (.preventDefault %)
                              (toggle-delete-dialogue state))}
             [:span [:i.fa.fa-trash] (t :badge/Deleteendorsementrequest)]]
            [confirm-delete state #(delete-sent-request! id state)]]])]]])))
