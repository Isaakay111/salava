(ns salava.badge.schemas
   #?(:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
      :cljs (:require [schema.core :as s :include-macros true])))

#?(:cljs (defn describe [v _] v))

(s/defschema user-badge-p {:id                                   (describe s/Int "internal user-badge id")
                           :name                                 s/Str
                           :description                          (s/maybe s/Str)
                           :image_file                           (s/maybe s/Str)
                           :assertion_url                        s/Str
                           :revoked                              (describe (s/cond-pre s/Int s/Bool) "badge revoked flag (1 -> revoked)")
                           :issued_on                            s/Int
                           :expires_on                           (s/maybe s/Int)
                           :mtime                                s/Int
                           :visibility                           (describe (s/maybe (s/enum "private" "internal" "public")) "internal user-badge visibility")
                           (s/optional-key :issuer_content_name) (s/maybe s/Str)
                           (s/optional-key :issuer_content_url)  (s/maybe s/Str)
                           (s/optional-key :status)              (describe (s/maybe (s/enum "pending" "accepted" "declined")) "internal user-badge acceptance status")
                           (s/optional-key :tags)                (describe (s/maybe [s/Str]) "internal tags added by current user")
                           (s/optional-key :png_image_file)      (describe (s/maybe s/Str) "png version of svg badge image")})

(s/defschema user-badge (-> user-badge-p
                            (assoc (s/optional-key :user_endorsements_count)    (s/maybe s/Int)
                                   (s/optional-key :meta_badge)                 (describe (s/maybe s/Str) "badge is a metabadge")
                                   (s/optional-key :meta_badge_req)             (describe (s/maybe s/Str) "badge is a required part of a metabadge")
                                   (s/optional-key :pending_endorsements_count) (s/maybe s/Int)
                                   (s/optional-key :endorsement_count)          (s/maybe s/Int)
                                   (s/optional-key :message_count)              (describe {:new-messages (s/maybe s/Int)
                                                                                           :all-messages (s/maybe s/Int)} "internal user-badge comments"))))

(s/defschema user-badges {:badges [user-badge]})

(s/defschema user-badges-p {:badges [user-badge-p]})

(s/defschema congratulation {:id s/Int
                             :first_name (s/maybe s/Str)
                             :last_name (s/maybe s/Str)
                             :profile_picture (s/maybe s/Str)})

(s/defschema evidence-properties {(s/optional-key :hidden)         (describe (s/maybe s/Bool) "evidence visibility flag")
                                  (s/optional-key  :resource_id)   (describe (s/maybe s/Int) "used internally, attached evidence resource id")
                                  (s/optional-key  :mime_type)     (describe (s/maybe s/Str) "used internally, mime type of attached evidence resource")
                                  (s/optional-key  :resource_type) (s/maybe s/Str)})

(s/defschema evidence {:id                              (s/maybe s/Int)
                       :name                            (s/maybe s/Str)
                       :narrative                       (s/maybe s/Str)
                       :url                             s/Str
                       (s/optional-key  :description)   (s/maybe s/Str)
                       (s/optional-key  :ctime)         (s/maybe s/Int)
                       (s/optional-key  :mtime)         (s/maybe s/Int)
                       (s/optional-key  :properties)    (s/maybe evidence-properties)})

(s/defschema badge-evidence {:evidence [evidence]})

(s/defschema alignment {:name s/Str
                        :description (s/maybe s/Str)
                        :url (s/maybe s/Str)})

(s/defschema badge-content {(s/optional-key :creator_content_id)     (s/maybe s/Str)
                            (s/optional-key :creator_description)    (s/maybe s/Str)
                            (s/optional-key :creator_email)          (s/maybe s/Str)
                            (s/optional-key :creator_name)           (s/maybe s/Str)
                            (s/optional-key :creator_image)          (s/maybe s/Str)
                            (s/optional-key :creator_url)            (s/maybe s/Str)
                            (s/optional-key :criteria_content)       (s/maybe s/Str)
                            (s/optional-key :criteria_url)           (s/maybe s/Str)
                            (s/optional-key :issuer_content_id)      (s/maybe s/Str)
                            (s/optional-key :issuer_description)     (s/maybe s/Str)
                            (s/optional-key :issuer_contact)         (s/maybe s/Str)
                            (s/optional-key :issuer_image)           (s/maybe s/Str)
                            (s/optional-key :issuer_content_url)     (s/maybe s/Str)
                            (s/optional-key :issuer_content_name)    (s/maybe s/Str)
                            (s/optional-key :language_code)          (s/maybe s/Str)
                            (s/optional-key :default_language_code)  (s/maybe s/Str)
                            (s/optional-key :endorsement_count)      (s/maybe s/Int)
                            (s/optional-key :alignment)              (s/maybe [alignment])
                            :badge_id                                (describe (s/maybe s/Str) "used internally to group user badges with same content")
                            :name                                    s/Str
                            :image_file                              (s/maybe s/Str)
                            :description                             (s/maybe s/Str)})

(s/defschema user-badge-content (-> user-badge-p
                                    (select-keys [:id :assertion_url :mtime :issued_on :expires_on :visibility :revoked])
                                    (assoc :ctime                                   s/Int
                                           ;:deleted                                 (describe s/Int "badge deleted flag (1 -> deleted)")
                                           :owner?                                  s/Bool
                                           :issued_by_obf                           (describe s/Bool "badge issued by OBF?")
                                           :verified_by_obf                         (describe s/Bool "badge verified by OBF?")
                                           :issuer_verified                         (describe (s/maybe s/Int) "issuer verified by OBF?")
                                           :first_name                              (describe (s/maybe s/Str) "badge earner's first name")
                                           :last_name                               (describe (s/maybe s/Str) "badge earner's last name")
                                           :owner                                   (describe s/Int "internal id of badge owner")
                                           :content                                 [badge-content]
                                           (s/optional-key :assertion_json)         (s/maybe s/Str)
                                           (s/optional-key :view_count)             (describe (s/maybe s/Int) "no of times user badge has been viewed")
                                           (s/optional-key :gallery_id)             (describe s/Int "internal gallery badge id")
                                           (s/optional-key :user_endorsement_count) (s/maybe s/Int)
                                           (s/optional-key :congratulations)        (describe (s/maybe [congratulation]) "internal user badge congratulations by other users")
                                           (s/optional-key :congratulated?)         (s/maybe s/Bool)
                                           (s/optional-key :badge_id)               (describe (s/maybe s/Str) "used internally to group user badges with same content")
                                           (s/optional-key :qr_code)                (s/maybe s/Str)
                                           (s/optional-key :obf_url)                (describe (s/maybe s/Str) "OBF factory url badge was issued from")
                                           (s/optional-key :user-logged-in?)        (s/maybe s/Bool)
                                           (s/optional-key :show_recipient_name)    (describe (s/maybe s/Int) "used internally; when set, earner's name is shown in badge ")
                                           (s/optional-key :remote_url)             (describe (s/maybe s/Str) "domain where badge assertion is hosted")
                                           (s/optional-key :recipient_count)        (s/maybe s/Int))))

(s/defschema user-badge-content-p (-> user-badge-p
                                      (select-keys [:id :assertion_url :mtime :issued_on :expires_on :visibility :revoked])
                                      (assoc :ctime                                   s/Int
                                             :content                                 [badge-content]
                                             :first_name                              (describe (s/maybe s/Str) "badge earner's first name")
                                             :last_name                               (describe (s/maybe s/Str) "badge earner's last name")
                                             :issuer_verified                         (describe (s/maybe s/Int) "issuer verified by OBF?")
                                             :issued_by_obf                           (describe s/Bool "badge issued by OBF?")
                                             :verified_by_obf                         (describe s/Bool "badge verified by OBF?")
                                             (s/optional-key :gallery_id)             (describe s/Int "internal gallery badge id")
                                             (s/optional-key :show_recipient_name)    (describe (s/maybe s/Int) "used internally; when set, earner's name is shown in badge ")
                                             (s/optional-key :remote_url)             (describe (s/maybe s/Str) "domain where badge assertion is hosted")
                                             (s/optional-key :assertion_json)         (s/maybe s/Str)
                                             (s/optional-key :qr_code)                (s/maybe s/Str))))


(s/defschema Badge {:id s/Int
                    :name s/Str
                    :description (s/maybe s/Str)
                    :user_id (s/maybe s/Int)
                    :email s/Str
                    :assertion_url (s/maybe s/Str)
                    :assertion_jws (s/maybe s/Str)
                    :assertion_json (s/maybe s/Str)
                    :badge_url (s/maybe s/Str)
                    :criteria_url (s/maybe s/Str)
                    :criteria_content (s/maybe s/Str)
                    :badge_id (s/maybe s/Str)
                    :image_file (s/maybe s/Str)
                    :issuer_content_id (s/maybe s/Str)
                    :issuer_email (s/maybe s/Str)
                    :issuer_content_name (s/maybe s/Str)
                    :issuer_content_url (s/maybe s/Str)
                    :issuer_image (s/maybe s/Str)
                    :issuer_url (s/maybe s/Str)
                    :creator_email (s/maybe s/Str)
                    :creator_name (s/maybe s/Str)
                    :creator_url (s/maybe s/Str)
                    :creator_image (s/maybe s/Str)
                    :issued_on (s/maybe s/Int)
                    :expires_on (s/maybe s/Int)
                    :evidence_url (s/maybe s/Str)
                    :status (s/maybe (s/enum "pending" "accepted" "declined"))
                    :visibility (s/maybe (s/enum "private" "internal" "public"))
                    :show_recipient_name (s/maybe s/Bool)
                    :rating (s/maybe s/Int)
                    :ctime s/Int
                    :mtime s/Int
                    :deleted (s/maybe s/Bool)
                    :revoked (s/maybe s/Bool)
                    :tags (s/maybe [s/Str])})

(s/defschema UserBadgeContent
  {:id                                   (describe s/Int "internal user-badge id")
   :name                                 (s/maybe s/Str)
   :description                          (s/maybe s/Str)
   :image_file                           (s/maybe s/Str)
   :issued_on                            s/Int
   :expires_on                           (s/maybe s/Int)
   :revoked                              s/Bool
   :mtime                                s/Int
   (s/optional-key :visibility)          (describe (s/maybe (s/enum "private" "internal" "public")) "internal user-badge visibility")
   (s/optional-key :status)              (describe (s/maybe (s/enum "pending" "accepted" "declined")) "internal user-badge acceptance status")
   (s/optional-key :badge_id)            (describe (s/maybe s/Str) "used internally to group user badges with same content")
   (s/optional-key :obf_url)             (describe (s/maybe s/Str) "OBF factory url badge was issued from")
   :issued_by_obf                        (describe s/Bool "badge issued by OBF?")
   :verified_by_obf                      (describe s/Bool "badge verified by OBF?")
   :issuer_verified                      (describe (s/maybe s/Int) "issuer verified by OBF?")
   (s/optional-key :issuer_content_name) (s/maybe s/Str)
   (s/optional-key :issuer_content_url)  (s/maybe s/Str)
   (s/optional-key :email)               (s/maybe s/Str)
   (s/optional-key :assertion_url)       (s/maybe s/Str)
   (s/optional-key :meta_badge)          (describe (s/maybe s/Str) "badge is a metabadge")
   (s/optional-key :meta_badge_req)      (describe (s/maybe s/Str) "badge is a required part of a metabadge")
   (s/optional-key :message_count)       (describe {:new-messages (s/maybe s/Int)
                                                    :all-messages (s/maybe s/Int)} "internal badge comments")
   (s/optional-key :tags)                (describe (s/maybe [s/Str]) "internal tags added by user")
   (s/optional-key :user_endorsements_count) (s/maybe s/Int)
   (s/optional-key :endorsement_count) (s/maybe s/Int)
   (s/optional-key :pending_endorsements_count) (s/maybe s/Int)})

(s/defschema BadgesToExport (select-keys Badge [:id :name :description :image_file
                                                :issued_on :expires_on :visibility
                                                :mtime :status :badge_content_id
                                                :email :assertion_url :tags
                                                :issuer_content_name ;:issuer_url
                                                :issuer_content_url]))

(s/defschema BadgeToImport {:status  (s/enum "ok" "invalid")
                            :message (s/maybe s/Str)
                            :error (s/maybe s/Str)
                            :import-key     s/Str
                            :name        s/Str
                            :description (s/maybe s/Str)
                            :image_file  (s/maybe s/Str)
                            :issuer_content_name (s/maybe s/Str)
                            :issuer_content_url (s/maybe s/Str)
                            :previous-id (s/maybe s/Int)})

(s/defschema Import {:status (s/enum "success" "error")
                     :badges [BadgeToImport]
                     :error  (s/maybe s/Str)})

(s/defschema Upload {:status (s/enum "success" "error")
                     :message s/Str
                     :reason (s/maybe s/Str)})

(s/defschema BadgeStats {:badge_count           s/Int
                         :expired_badge_count   s/Int
                         :badge_views           [(merge
                                                   (select-keys Badge [:id :name :image_file])
                                                   {:reg_count   s/Int
                                                    :anon_count  s/Int
                                                    :latest_view (s/maybe s/Int)})]
                         :badge_congratulations [(merge
                                                   (select-keys Badge [:id :name :image_file])
                                                   {:congratulation_count  s/Int
                                                    :latest_congratulation (s/maybe s/Int)})]
                         :badge_issuers         [(-> Badge
                                                     (select-keys [:issuer_content_id :issuer_content_name :issuer_content_url])
                                                     (assoc :badges [(select-keys Badge [:id :name :image_file])]))]})


(s/defschema Endorsement {:id s/Str
                          :content s/Str
                          :issued_on s/Int
                          :issuer {:id   s/Str
                                   :language_code s/Str
                                   :name s/Str
                                   :url  s/Str
                                   :description (s/maybe s/Str)
                                   :image_file (s/maybe s/Str)
                                   :email (s/maybe s/Str)
                                   :revocation_list_url (s/maybe s/Str)
                                   :endorsement [(s/maybe (s/recursive #'Endorsement))]}})

(s/defschema BadgeContent {:id    s/Str
                           :language_code s/Str
                           :name  s/Str
                           :image_file  s/Str
                           :description s/Str
                           (s/optional-key :obf_url)    (s/maybe s/Str)
                           :alignment [(s/maybe {:name s/Str
                                                 :url  s/Str
                                                 :description (s/maybe s/Str)})]
                           :tags      [(s/maybe s/Str)]})

(s/defschema IssuerContent {:id   s/Str
                            :language_code s/Str
                            :name s/Str
                            :url  s/Str
                            :description (s/maybe s/Str)
                            :image_file (s/maybe s/Str)
                            :email (s/maybe s/Str)
                            :revocation_list_url (s/maybe s/Str)
                            :endorsement [(s/maybe Endorsement)]})


(s/defschema CreatorContent (-> IssuerContent
                                (dissoc :revocation_list_url)
                                (assoc  :json_url s/Str)))

(s/defschema CriteriaContent {:id s/Str
                              :language_code s/Str
                              :url s/Str
                              :markdown_text (s/maybe s/Str)})

(s/defschema UserBackpackEmail {:email s/Str
                                :backpack_id (s/maybe s/Int)})

#_(s/defschema Evidence {:id (s/maybe s/Int)
                         :name (s/maybe s/Str)
                         :narrative (s/maybe s/Str)
                         :url s/Str
                         (s/optional-key  :resource_id) (s/maybe s/Int)
                         (s/optional-key  :resource_type) s/Str
                         (s/optional-key  :mime_type) (s/maybe s/Str)})

(s/defschema UserEndorsement {:id s/Int
                               :user_badge_id s/Int
                               :content s/Str
                               (s/optional-key :status) (s/maybe s/Str)
                               (s/optional-key :ctime) (s/maybe s/Int)
                               (s/optional-key :mtime) (s/maybe s/Int)
                               (s/optional-key :issuer_id) (s/maybe s/Int)
                               (s/optional-key :endorsee_id) (s/maybe s/Int)
                               (s/optional-key :issuer_name) (s/maybe s/Str)
                               (s/optional-key :issuer_url) (s/maybe s/Str)
                               (s/optional-key :first_name) s/Str
                               (s/optional-key :last_name) s/Str
                               (s/optional-key :profile_picture) (s/maybe s/Str)
                               (s/optional-key :profile_visibility) (s/enum "internal" "public")
                               (s/optional-key :name) (s/maybe s/Str)
                               (s/optional-key :image_file) (s/maybe s/Str)
                               (s/optional-key :description) (s/maybe s/Str)})

(s/defschema EndorsementRequest (-> UserEndorsement
                                    (assoc (s/optional-key :issued_on) (s/maybe s/Int)
                                           (s/optional-key :type) (s/maybe s/Str)
                                           (s/optional-key :requester_id) s/Int
                                           (s/optional-key :requestee_id) s/Int
                                           (s/optional-key :user_id) s/Int
                                           (s/optional-key :issuer_content_id) s/Str)))

(s/defschema AllEndorsements {:given [(s/maybe UserEndorsement)]
                              :received [(s/maybe UserEndorsement)]
                              :requests [(s/maybe EndorsementRequest)]
                              :sent-requests [(s/maybe EndorsementRequest)]
                              :all-endorsements [(s/maybe (merge EndorsementRequest UserEndorsement))]})
