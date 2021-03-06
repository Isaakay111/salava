(ns salava.badge.endorsement-schemas
  #? (:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true])))

#? (:cljs (defn describe [v _] v))

(defn either [s1 s2]
  #? (:clj (s/either s1 s2)
      :cljs (s/cond-pre s1 s2)))

(def content (describe (s/constrained s/Str #(and (>= (count %) 5)
                                                  (not (clojure.string/blank? %)))) "Content in markdown format"))
(def _name (s/constrained s/Str #(and (>= (count %) 1)
                                      (<= (count %) 255))))

(s/defschema endorsement {:id s/Int
                          :user_badge_id s/Int
                          :content s/Str
                          :status (s/enum "pending" "accepted" "declined")
                          :mtime s/Int})

(s/defschema request (-> endorsement
                         (assoc
                          :status (s/enum "pending" "endorsed" "declined")
                          (s/optional-key  :type) (s/enum "sent_request" "request" "ext_request")
                          (s/optional-key :ctime) s/Int)))

(s/defschema received-user-endorsement-p  (-> endorsement
                                              (assoc :issuer_id (s/maybe s/Int))))

(s/defschema received-user-endorsement (-> received-user-endorsement-p
                                           (assoc
                                            (s/optional-key :issuer_name) (s/maybe _name)
                                            :issuer_url (s/maybe s/Str)
                                            :profile_picture (describe (s/maybe s/Str) "Issuer's image")
                                            :description (s/maybe s/Str)
                                            :name (s/maybe s/Str)
                                            :image_file (s/maybe s/Str))))

(s/defschema received-ext-endorsements (-> received-user-endorsement
                                           (dissoc :issuer_id :profile_picture :description :image_file :name)
                                           (assoc :issuer_id s/Str
                                                  :type s/Str
                                                  (s/optional-key :issuer_image) (s/maybe s/Str)
                                                  (s/optional-key :email) s/Str
                                                  (s/optional-key :profile_picture) (s/maybe s/Str))))

(s/defschema given-user-endorsement-p  (-> endorsement
                                           (assoc
                                            :endorsee_id (describe (s/maybe s/Int) "ID of endorsement receiver"))))

(s/defschema given-user-endorsement  (-> given-user-endorsement-p
                                         (assoc
                                          :profile_picture (s/maybe s/Str)
                                          :first_name _name
                                          :last_name _name
                                          :name (describe s/Str "Badge name")
                                          :image_file (describe (s/maybe s/Str) "Badge image")
                                          :description (describe (s/maybe s/Str) "Badge description"))))

(s/defschema sent-request-p (-> request
                                (assoc :requestee_id (describe (s/maybe s/Int) "ID of request receiver"))))

(s/defschema sent-request  (-> sent-request-p
                               (assoc
                                :profile_picture (s/maybe s/Str)
                                :name (s/maybe s/Str)
                                :image_file (s/maybe s/Str)
                                (s/optional-key :issuer_name) (s/maybe _name))))

(s/defschema received-request-p (-> request
                                    (assoc :requester_id (s/maybe s/Int))))

(s/defschema received-request (-> received-request-p
                                  (assoc
                                   :description (s/maybe s/Str)
                                   (s/optional-key :issuer_name) (s/maybe s/Str)
                                   :issued_on s/Int
                                   :issuer_content_id (s/maybe s/Str)
                                   :first_name _name
                                   :last_name _name
                                   :profile_picture (s/maybe s/Str)
                                   :image_file (s/maybe s/Str)
                                   :name s/Str)))

(s/defschema pending-requests [(s/maybe received-request)])

(s/defschema pending-sent-requests [(s/maybe (-> request
                                                 (dissoc :type)
                                                 (assoc :user_id (s/maybe s/Int)
                                                        :profile_picture (s/maybe s/Str)
                                                        :first_name _name
                                                        :last_name _name)))])

(s/defschema all-endorsements-p {:given [(s/maybe given-user-endorsement-p)]
                                 :received [(s/maybe received-user-endorsement-p)]
                                 :sent-requests [(s/maybe sent-request-p)]
                                 :requests [(s/maybe received-request-p)]})


(s/defschema all-endorsements {:given [(s/maybe given-user-endorsement)]
                               :received [(s/maybe received-user-endorsement)]
                               :sent-requests [(s/maybe sent-request)]
                               :requests [(s/maybe received-request)]
                               (s/optional-key :ext-received) [(s/maybe (-> received-user-endorsement
                                                                            (assoc :issuer_id s/Str
                                                                                   :issuer_image (s/maybe s/Str)
                                                                                   :email s/Str
                                                                                   :type (s/eq "ext"))
                                                                            (dissoc :profile_picture)))]
                               (s/optional-key :ext-sent-requests) [(s/maybe (-> sent-request
                                                                                 (dissoc :profile_picture :type :requestee_id)
                                                                                 (assoc :issuer_image (s/maybe s/Str)
                                                                                        :email s/Str)))]
                               :all-endorsements [(s/maybe s/Any)]})

(s/defschema user-badge-endorsements-p {:endorsements [(s/maybe received-user-endorsement-p)]})

(s/defschema user-badge-endorsement {:endorsements [(s/maybe (either (-> received-user-endorsement
                                                                         (assoc  :profile_visibility (s/maybe (s/enum "internal" "public")))
                                                                         (dissoc :name :image_file :description))
                                                                     received-ext-endorsements))]})

(s/defschema pending-user-endorsements {:endorsements [(s/maybe (either
                                                                 (-> received-user-endorsement (dissoc :status :mtime) (assoc :ctime s/Int (s/optional-key :mtime) s/Int))
                                                                 (-> received-ext-endorsements
                                                                     (assoc :image_file (s/maybe s/Str)
                                                                            :name s/Str
                                                                            :description (s/maybe s/Str)))))]})

(s/defschema request-endorsement {:content content
                                  (s/optional-key :user-ids) [(s/maybe s/Int)]
                                  (s/optional-key :emails) [(s/maybe s/Str)]})

(s/defschema ext-request {:content content
                          :id s/Int
                          :user_badge_id s/Int
                          :status (s/enum "pending" "endorsed" "declined")
                          :mtime s/Int
                          (s/optional-key :issuer_email) s/Str
                          (s/optional-key :issuer_id) (s/maybe s/Int)
                          (s/optional-key :url) (s/maybe s/Str)
                          (s/optional-key :description) (s/maybe s/Str)
                          (s/optional-key :name) (s/maybe s/Str)
                          (s/optional-key :image_file) (s/maybe s/Str)})


(s/defschema pending-ext-requests [(s/maybe ext-request)])
(s/defschema ext-endorser (s/maybe {:id s/Int
                                    :ext_id s/Str
                                    :name (s/maybe _name)
                                    :url (s/maybe s/Str)
                                    :description (s/maybe s/Str)
                                    :image_file (s/maybe s/Str)
                                    :email s/Str
                                    :ctime s/Int
                                    :mtime s/Int}))

(s/defschema save-ext-endorsement {:content content
                                   :endorser {:ext_id s/Str
                                              :name (s/maybe _name)
                                              :url (s/maybe s/Str)
                                              :description (s/maybe s/Str)
                                              :image_file (s/maybe s/Str)
                                              :email s/Str}})
                                    ;(-> ext-endorser (select-keys [:ext_id :name :url :description :image_file :email]))})

(s/defschema issued-ext-endorsement (s/maybe (-> endorsement
                                                 (merge {(s/optional-key :name) s/Str
                                                         (s/optional-key :image_file) s/Str
                                                         (s/optional-key :type) s/Str
                                                         :first_name s/Str
                                                         :last_name s/Str
                                                         :profile_picture (s/maybe s/Str)}))))

(s/defschema issued-ext-endorsement-all [issued-ext-endorsement])

(s/defschema endorsement-requests-all [(s/maybe (-> ext-request
                                                 (merge {(s/optional-key :name) s/Str
                                                         (s/optional-key :image_file) s/Str
                                                         (s/optional-key :type) s/Str
                                                         :first_name s/Str
                                                         :last_name s/Str
                                                         :profile_picture (s/maybe s/Str)})))])
