(ns salava.factory.routes
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.core.access :as access]
            [salava.core.middleware :as mw]
            [salava.core.util :as u]
            [salava.factory.db :as f]
            [salava.user.db :as user]))

(defn route-def [ctx]
  (routes
   (context "/obpv1/factory" []
            :tags ["factory"]
            :no-doc true

            (HEAD "/receive" []
                  :no-doc true
                  :summary "Capability check for GET /receive"
                  :query-params [e :- String
                                 k :- String
                                 t :- String]
                  (ok ""))

            (GET "/receive" []
                 :no-doc true
                 :summary "Receive new badges from OBF"
                 :query-params [e :- String
                                k :- String
                                t :- String]
                 :current-user current-user
                 (let [user_id (user/email-exists? ctx e)
                       badge-info (if (= user_id (:id current-user)) (assoc (f/receive-badge-json ctx e k t) :user_id user_id) (f/receive-badge-json ctx e k t))]
                   (if-let [user-badge-id (f/receive-badge ctx badge-info)]
                     (-> (str (u/get-base-path ctx) (str "/badge/receive/" user-badge-id "?banner=" (f/receive-banner (:banner badge-info))))
                         redirect
                         (assoc-in [:session] {:pending {:user-badge-id user-badge-id :email e} :identity current-user}))
                     (not-found "404 Not Found"))))

            (DELETE "/receive/:id" req
                    :no-doc true
                    :summary "Receive new badges from OBF"
                    :path-params [id :- s/Int]
                    :current-user current-user
                    (when (= id (get-in req [:session :pending :user-badge-id]))
                      (ok (f/reject-badge! ctx id (:id current-user)))))

            (POST "/backpack_email_list" []
                  :header-params [authorization :- s/Str]
                  :body-params [emails :- [s/Str]]
                  :middleware [#(mw/wrap-factory-auth % ctx)]
                  (ok (f/get-user-emails ctx emails)))

            (POST "/users_badges" []
                  :header-params [authorization :- s/Str]
                  :body-params [assertions :- s/Any]
                  :middleware [#(mw/wrap-factory-auth % ctx)]
                  (let [result (f/save-assertions-for-emails ctx assertions)]
                    (if result
                      (ok {:success true})
                      (internal-server-error {:error "transaction failed"}))))

            (GET "/get_updates" []
                 :query-params [user :- s/Int
                                badge :- s/Int]
                 (ok (f/get-badge-updates ctx user badge)))

            (GET "/pdf_cert/:user-badge-id" []
                 :no-doc true
                 :path-params [user-badge-id :- s/Int]
                 ;:auth-rules access/signed
                 :current-user current-user
                 (ok (f/get-pdf-cert-list ctx current-user user-badge-id)))

            (POST "/pdf_cert_request/:user-badge-id" []
                  :no-doc true
                  :path-params [user-badge-id :- s/Int]
                  :body-params [message :- s/Str]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (f/new-pdf-cert-request ctx current-user user-badge-id message))))))
